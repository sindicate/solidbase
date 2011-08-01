/*--
 * Copyright 2006 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import solidbase.util.Assert;
import solidbase.util.LineReader;
import solidbase.util.Resource;
import solidbase.util.ShutdownHook;
import solidbase.util.WorkerThread;


/**
 * This class is the coordinator. It requests an upgrade path from the {@link PatchFile}, and reads commands from it. It
 * calls the {@link CommandListener}s, updates the DBVERSION and DBVERSIONLOG tables through {@link DBVersion}, calls
 * the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class PatchProcessor extends CommandProcessor implements ConnectionListener
{
	// Don't need whitespace at the end of the Patterns

	/**
	 * Pattern for TRANSIENT.
	 */
	static protected Pattern transientPattern = Pattern.compile( "SESSIONCONFIG|TRANSIENT", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /TRANSIENT.
	 */
	static protected Pattern transientPatternEnd = Pattern.compile( "/(SESSIONCONFIG|TRANSIENT)", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for IF HISTORY [NOT] CONTAINS.
	 */
	static protected Pattern ifHistoryContainsPattern = Pattern.compile( "IF\\s+HISTORY\\s+(NOT\\s+)?CONTAINS\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE );

	// The fields below are all part of the upgrade context. It's reset at the start of each change package.

	/**
	 * Indicates that the statements are transient and should not be counted.
	 */
	protected boolean dontCount;

	/**
	 * The upgrade file being executed.
	 */
	protected PatchFile patchFile;

	/**
	 * The current source of a patch.
	 */
	protected PatchSource patchSource;

	/**
	 * The class that manages the DBVERSION and DBVERSIONLOG table.
	 */
	protected DBVersion dbVersion;

	/**
	 * The patch that is currently processed.
	 */
	protected Patch patch;

	/**
	 * Construct a new instance of the patcher.
	 *
	 * @param listener Listens to the progress.
	 */
	public PatchProcessor( ProgressListener listener )
	{
		super( listener );
		this.autoCommit = true;
	}

	/**
	 * Construct a new instance of the patcher.
	 *
	 * @param listener Listens to the progress.
	 * @param database The default database.
	 */
	public PatchProcessor( ProgressListener listener, Database database )
	{
		super( listener, database );
	}

	@Override
	public void addDatabase( Database database )
	{
		super.addDatabase( database );
		database.setConnectionListener( this );
	}

	/**
	 * Sets the patch file.
	 *
	 * @param patchFile The patch file to set.
	 */
	public void setPatchFile( PatchFile patchFile )
	{
		this.patchFile = patchFile;
	}

	/**
	 * Initialize the patcher.
	 */
	// TODO Remove this init, should be in the constructor
	public void init()
	{
		this.dbVersion = new DBVersion( getDefaultDatabase(), this.progress, this.patchFile.versionTableName, this.patchFile.logTableName );
	}

	/**
	 * Resets the upgrade context. This is called before the execution of each changeset.
	 */
	@Override
	protected void reset()
	{
		super.reset();
		this.dontCount = false;
	}

	@Override
	public void end()
	{
		super.end();
		this.patchFile.close();
	}

	/**
	 * Get the current version of the database.
	 *
	 * @return The current version of the database.
	 */
	public String getCurrentVersion()
	{
		Assert.notNull( this.dbVersion, "default database may not have been configured" );
		return this.dbVersion.getVersion();
	}

	/**
	 * Get the current target.
	 *
	 * @return The current target.
	 */
	public String getCurrentTarget()
	{
		return this.dbVersion.getTarget();
	}

	/**
	 * Get the number of persistent statements executed successfully.
	 *
	 * @return The number of persistent statements executed successfully.
	 */
	public int getCurrentStatements()
	{
		return this.dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets from the current version. The current version is also considered.
	 *
	 * @param tips If true only the tips of the upgrade paths are returned.
	 * @param prefix Only return targets that start with the given prefix.
	 * @param downgradeable Also consider downgrade paths.
	 * @return All possible targets from the current version.
	 */
	public LinkedHashSet< String > getTargets( boolean tips, String prefix, boolean downgradeable )
	{
		LinkedHashSet< String > result = new LinkedHashSet< String >();
		this.patchFile.collectTargets( this.dbVersion.getVersion(), this.dbVersion.getTarget(), tips, downgradeable, prefix, result );
		return result;
	}

	/**
	 * Use the 'setup' change sets to upgrade the DBVERSION and DBVERSIONLOG tables to the newest specification.
	 *
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void setupControlTables() throws SQLExecutionException
	{
		String spec = this.dbVersion.getSpec();

		List< Patch > patches = this.patchFile.getSetupPath( spec );
		if( patches == null )
			return;

		Assert.notEmpty( patches );

		// SETUP blocks get special treatment.
		for( Patch patch : patches )
		{
			process( patch );
			this.dbVersion.updateSpec( patch.getTarget() );
			if( Thread.currentThread().isInterrupted() )
				throw new ThreadDeath();
			// TODO How do we get a more dramatic error message here, if something goes wrong?
		}
	}

	/**
	 * Patch to the given target.
	 *
	 * @param target The target to patch to.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void patch( String target ) throws SQLExecutionException
	{
		patch( target, false );
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that
	 * matches the target prefix. This method protects itself against SIGTERMs (CTRL-C).
	 *
	 * @param target The target requested.
	 * @param downgradeable Indicates that downgrade paths are allowed to reach the given target.
	 * @throws SQLExecutionException When the execution of a command throws an {@link SQLException}.
	 */
	public void patch( final String target, final boolean downgradeable ) throws SQLExecutionException
	{
		WorkerThread worker = new WorkerThread()
		{
			@Override
			public void work()
			{
				patchProtected( target, downgradeable );
			}
		};

		// Protect from Ctrl-C aborting all threads, uses interrupt() instead.
		ShutdownHook hook = new ShutdownHook( worker );
		Runtime.getRuntime().addShutdownHook( hook );

		worker.start();
		try
		{
			worker.join();
			if( worker.getException() != null )
				throw worker.getException();
			if( worker.isThreadDeath() )
				throw new FatalException( "Interrupted by user" );
			try
			{
				Runtime.getRuntime().removeShutdownHook( hook );
			}
			catch( IllegalStateException e )
			{
				// Can't use isInterrupted(), that one returns false after the thread ended
				throw new FatalException( "Interrupted by user" );
			}
		}
		catch( InterruptedException e )
		{
			// TODO Shouldn't we throw "Interrupted by user" here?
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that
	 * matches the target prefix.
	 *
	 * @param target The target requested.
	 * @param downgradeable Indicates that downgrade paths are allowed to reach the given target.
	 * @throws SQLExecutionException When the execution of a command throws an {@link SQLException}.
	 */
	protected void patchProtected( String target, boolean downgradeable ) throws SQLExecutionException
	{
		setupControlTables();

		String version = this.dbVersion.getVersion();

		if( target == null )
		{
			LinkedHashSet< String > targets = getTargets( true, null, downgradeable );
			if( targets.size() > 1 )
				throw new FatalException( "More than one possible target found, you should specify a target." );
			Assert.notEmpty( targets );

			target = targets.iterator().next();
		}
		else if( target.endsWith( "*" ) )
		{
			String targetPrefix = target.substring( 0, target.length() - 1 );
			LinkedHashSet< String > targets = getTargets( true, targetPrefix, downgradeable );
			if( targets.size() > 1 )
				throw new FatalException( "More than one possible target found for " + target );
			if( targets.isEmpty() )
				throw new FatalException( "Target " + target + " is not reachable from version " + StringUtils.defaultString( version, "<no version>" ) );

			target = targets.iterator().next();
		}
		else
		{
			LinkedHashSet< String > targets = getTargets( false, null, downgradeable );
			Assert.notEmpty( targets );

			// TODO Refactor this, put this in getTargets()
			boolean found = false;
			for( String t : targets )
				if( ObjectUtils.equals( t, target ) )
				{
					found = true;
					break;
				}

			if( !found )
				throw new FatalException( "Target " + target + " is not reachable from version " + StringUtils.defaultString( version, "<no version>" ) );
		}

		if( ObjectUtils.equals( target, version ) )
		{
			this.progress.noUpgradeNeeded();
			return;
		}

		patch( version, target, downgradeable );

		this.progress.upgradeComplete();
		return;
	}

	/**
	 * Upgrade the database from the given version to the given target.
	 *
	 * @param version The current version of the database.
	 * @param target The target to upgrade to.
	 * @param downgradeable Allow downgrades to reach the target
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void patch( String version, String target, boolean downgradeable ) throws SQLExecutionException
	{
		if( target.equals( version ) )
			return;

		Path path = this.patchFile.getPatchPath( version, target, downgradeable );
		Assert.isTrue( path.size() > 0, "No upgrades found" );

		for( Patch patch : path )
		{
			process( patch );
			if( Thread.currentThread().isInterrupted() )
				throw new ThreadDeath();
		}
	}

	/**
	 * Execute a patch.
	 *
	 * @param patch The patch to be executed.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void process( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch );

		this.progress.patchStarting( patch );

		// Determine how many to skip
		this.patchSource = this.patchFile.gotoPatch( patch );
		int skip = this.dbVersion.getStatements();
		if( this.dbVersion.getTarget() == null )
			skip = 0;

		reset();

//		if( !patch.isSetup() )
//		{
//			Fragment initialization = this.patchFile.initialization;
//			if( initialization != null )
//			{
//				SQLProcessor processor = new SQLProcessor( this.progress );
//				for( Database database : this.databases.values() )
//					processor.addDatabase( database );
//				processor.setSQLSource( new SQLSource( initialization ) );
//				processor.reset();
//				processor.execute();
//			}
//		}

		int count = 0;
		this.patch = patch;
		try
		{
			Command command = this.patchSource.readCommand();
			while( command != null )
			{
				if( command.isPersistent() && !this.dontCount && !patch.isSetup() )
				{
					count++;
					if( count > skip && this.skipCounter == 0 )
					{
						try
						{
							executeWithListeners( command );
						}
						catch( SQLExecutionException e )
						{
							// TODO Should we also log the exception when it is ignored?
							// TODO We need a unit test for this
							this.dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), e );
							throw e;
						}
						// We have to update the progress even if the logging fails. Otherwise the patch cannot be
						// restarted. That's why the progress update is first. But some logging will be lost in that case.
						this.dbVersion.updateProgress( patch.getTarget(), count );
						this.dbVersion.log( "S", patch.getSource(), patch.getTarget(), count, command.getCommand(), (String)null );
					}
					else
						this.progress.skipped( command );
				}
				else
					executeWithListeners( command );

				if( !patch.isSetup() )
					if( Thread.currentThread().isInterrupted() )
						throw new ThreadDeath();

				command = this.patchSource.readCommand();
			}

			this.progress.patchFinished();

			this.dbVersion.setStale(); // TODO With a normal patch, only set stale if not both of the 2 version tables are found
			if( patch.isSetup() )
			{
				this.dbVersion.updateSpec( patch.getTarget() );
				Assert.isFalse( patch.isOpen() );
			}
			else
			{
				if( patch.isDowngrade() )
				{
					Set< String > versions = this.patchFile.getReachableVersions( patch.getTarget(), null, false );
					versions.remove( patch.getTarget() );
					this.dbVersion.downgradeHistory( versions );
				}
				if( !patch.isOpen() )
				{
					this.dbVersion.updateVersion( patch.getTarget() );
					this.dbVersion.logComplete( patch.getSource(), patch.getTarget(), count );
				}
			}
		}
		finally
		{
			this.patch = null;
		}
	}

	@Override
	protected boolean executeListeners( Command command ) throws SQLException
	{
		if( command.isTransient() )
		{
			String sql = command.getCommand();
			Matcher matcher;
			if( transientPattern.matcher( sql ).matches() )
			{
				enableDontCount();
				return true;
			}
			if( transientPatternEnd.matcher( sql ).matches() )
			{
				disableDontCount();
				return true;
			}
			if( ( matcher = ifHistoryContainsPattern.matcher( sql ) ).matches() )
			{
				ifHistoryContains( matcher.group( 1 ), matcher.group( 2 ) );
				return true;
			}
		}

		return super.executeListeners( command );
	}

	@Override
	protected void executeJdbc( Command command ) throws SQLException
	{
		if( command.getCommand().trim().equalsIgnoreCase( "UPGRADE" ) )
			upgrade( command );
		else
			super.executeJdbc( command );
	}

	private void upgrade( Command command ) throws SQLException
	{
		Assert.isTrue( this.patch.isSetup(), "UPGRADE only allowed in SETUP blocks" );
		Assert.isTrue( this.patch.getSource().equals( "1.0" ) && this.patch.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		int pos = command.getLineNumber();
		executeJdbc( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false, pos ) );
		executeJdbc( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETE' WHERE RESULT LIKE 'COMPLETED VERSION %'", false, pos ) );
		executeJdbc( new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false, pos ) ); // We need this because the column is made NOT NULL in the upgrade setup block
	}

	/**
	 * Persistent commands should be considered transient.
	 */
	protected void enableDontCount()
	{
		Assert.isFalse( this.dontCount, "Counting already enabled" );
		this.dontCount = true;
	}

	/**
	 * Persistent commands should be considered persistent again.
	 */
	protected void disableDontCount()
	{
		Assert.isTrue( this.dontCount, "Counting already disabled" );
		this.dontCount = false;
	}

	/**
	 * If history does not contain the given version then start skipping the persistent commands. If <code>not</code> is true then this logic is reversed.
	 *
	 * @param not True causes reversed logic.
	 * @param version The version to look for in the history.
	 */
	private void ifHistoryContains( String not, String version )
	{
		boolean c = this.dbVersion.logContains( version );
		if( not != null )
			c = !c;
		skip( !c );
	}

	@Override
	protected void setDelimiters( Delimiter[] delimiters )
	{
		this.patchSource.setDelimiters( delimiters );
	}

	/**
	 * Write the history to the given output stream.
	 *
	 * @param out The output stream to write to.
	 */
	public void logToXML( OutputStream out )
	{
		this.dbVersion.logToXML( out, Charset.forName( "UTF-8" ) );
	}

	/**
	 * Write the log to the given resource.
	 *
	 * @param output The resource to write the log to.
	 */
	public void logToXML( Resource output )
	{
		OutputStream out = output.getOutputStream();
		try
		{
			logToXML( out );
		}
		finally
		{
			try
			{
				out.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
	}

	/**
	 * Returns a statement of the current version of the database in a user presentable form.
	 *
	 * @return A statement of the current version of the database in a user presentable form.
	 */
	public String getVersionStatement()
	{
		return this.dbVersion.getVersionStatement();
	}

	public void connected( Database database )
	{
//		for( InitConnectionFragment init : this.patchFile.connectionInits )
//		if( init.getConnectionName() == null || init.getConnectionName().equalsIgnoreCase( database.getName() ) )
//			if( init.getUserName() == null || init.getUserName().equalsIgnoreCase( database.getCurrentUser() ) )
//			{
//				SQLProcessor processor = new SQLProcessor( this.progress );
//				processor.setConnection( database );
//				processor.setSQLSource( new SQLSource( init.getText(), init.getLineNumber() ) );
//				processor.execute();
//			}
	}

	@Override
	public LineReader getReader()
	{
		return this.patchFile.file;
	}

	@Override
	public Resource getResource()
	{
		return this.patchFile.file.getResource();
	}
}
