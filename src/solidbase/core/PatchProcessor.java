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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;


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

	/**
	 * Pattern for /IF.
	 */
	static protected Pattern ifHistoryContainsEnd = Pattern.compile( "/IF", Pattern.CASE_INSENSITIVE );

	// The fields below are all part of the upgrade context. It's reset at the start of each change package.

	/**
	 * Indicates that the statements are transient and should not be counted.
	 */
	protected boolean dontCount;

	/**
	 * Together with {@link PatchProcessor#conditionFalseCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link PatchProcessor#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link PatchProcessor#conditionFalseCounter} get incremented.
	 */
	protected int conditionTrueCounter;

	/**
	 * Together with {@link PatchProcessor#conditionTrueCounter} this enables nested conditions. As long as nested conditions
	 * evaluate to true the {@link PatchProcessor#conditionTrueCounter} gets incremented. After the first nested condition
	 * evaluates to false, the {@link PatchProcessor#conditionFalseCounter} get incremented.
	 */
	protected int conditionFalseCounter;

	/**
	 * The upgrade file being executed.
	 */
	protected PatchFile patchFile;

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
		setCommandSource( patchFile );
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
		this.conditionTrueCounter = this.conditionFalseCounter = 0;
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
	 * Returns all possible targets from the current version.
	 * 
	 * @param tips If true only the tips of the upgrade paths are returned.
	 * @param prefix Only return targets that start with the given prefix.
	 * @param downgradeable Also consider downgrade paths.
	 * @return All possible targets from the current version.
	 */
	public LinkedHashSet< String > getTargets( boolean tips, String prefix, boolean downgradeable )
	{
		LinkedHashSet result = new LinkedHashSet();
		this.patchFile.collectTargets( this.dbVersion.getVersion(), this.dbVersion.getTarget(), tips, downgradeable, prefix, result );
		return result;
	}

	/**
	 * Use the 'init' change sets to upgrade the DBVERSION and DBVERSIONLOG tables to the newest specification.
	 * 
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void initializeControlTables() throws SQLExecutionException
	{
		String spec = this.dbVersion.getSpec();

		List patches = this.patchFile.getInitPath( spec );
		if( patches == null )
			return;

		Assert.notEmpty( patches );

		// INIT blocks get special treatment.
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
			this.dbVersion.updateSpec( patch.getTarget() );
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
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target The target requested.
	 * @param downgradeable Indicates that downgrade paths are allowed to reach the given target.
	 * @throws SQLExecutionException When the execution of a command throws an {@link SQLException}.
	 */
	public void patch( String target, boolean downgradeable ) throws SQLExecutionException
	{
		initializeControlTables();

		if( target == null )
		{
			LinkedHashSet< String > targets = getTargets( true, null, downgradeable );
			if( targets.size() > 1 )
				throw new FatalException( "More than one possible target found, you should specify a target." );
			if( targets.size() == 0 )
				throw new SystemException( "Expected at least some targets" );

			String t = targets.iterator().next();
			patch( this.dbVersion.getVersion(), t, downgradeable );
			this.progress.upgradeComplete();
			return; // TODO What about the terminateCommandListeners below?
		}

		LinkedHashSet< String > targets;

		if( target.endsWith( "*" ) )
		{
			String targetPrefix = target.substring( 0, target.length() - 1 );
			targets = getTargets( true, targetPrefix, downgradeable );
			Assert.isTrue( targets.size() <= 1 );
			for( String t : targets )
				if( t.startsWith( targetPrefix ) )
				{
					patch( this.dbVersion.getVersion(), t, downgradeable );
					break;
				}
		}
		else
		{
			targets = getTargets( false, null, downgradeable );
			for( String t : targets )
				if( ObjectUtils.equals( t, target ) )
				{
					patch( this.dbVersion.getVersion(), t, downgradeable );
					break;
				}
		}

		terminateCommandListeners();

		if( targets.size() == 0 )
			throw new FatalException( "There is no upgrade path from the current version of the database (" + StringUtils.defaultString( this.dbVersion.getVersion(), "no version" ) + ") to the requested target version " + target );

		this.progress.upgradeComplete();
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
			patch( patch );
	}

	/**
	 * Execute a patch.
	 * 
	 * @param patch The patch to be executed.
	 * @throws SQLExecutionException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void patch( Patch patch ) throws SQLExecutionException
	{
		Assert.notNull( patch );

		this.progress.patchStarting( patch );

		// Determine how many to skip
		this.patchFile.gotoPatch( patch );
		int skip = this.dbVersion.getStatements();
		if( this.dbVersion.getTarget() == null )
			skip = 0;

		reset();

		int count = 0;
		this.patch = patch;
		try
		{
			Command command = this.patchFile.readCommand();
			while( command != null )
			{
				if( command.isPersistent() && !this.dontCount && !patch.isInit() )
				{
					count++;
					if( count > skip && this.conditionFalseCounter == 0 )
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

				command = this.patchFile.readCommand();
			}

			this.progress.patchFinished();

			this.dbVersion.setStale(); // TODO With a normal patch, only set stale if not both of the 2 version tables are found
			if( patch.isInit() )
			{
				this.dbVersion.updateSpec( patch.getTarget() );
				Assert.isFalse( patch.isOpen() );
			}
			else
			{
				if( patch.isDowngrade() )
				{
					Set versions = this.patchFile.getReachableVersions( patch.getTarget(), null, false );
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
	protected void execute( Command command ) throws SQLException
	{
		String sql = command.getCommand();
		if( command.isTransient() )
		{
			Matcher matcher;
			if( transientPattern.matcher( sql ).matches() )
				enableDontCount();
			else if( transientPatternEnd.matcher( sql ).matches() )
				disableDontCount();
			else if( ( matcher = ifHistoryContainsPattern.matcher( sql ) ).matches() )
				ifHistoryContains( matcher.group( 1 ), matcher.group( 2 ) );
			else if( ifHistoryContainsEnd.matcher( sql ).matches() )
				popCondition();
			else
				super.execute( command );
		}
		else if( this.dontCount )
		{
			super.execute( command );
		}
		else
		{
			if( sql.trim().equalsIgnoreCase( "UPGRADE" ) )
			{
				this.progress.executing( command, this.startMessage );
				this.startMessage = null;
				upgrade( command );
				this.progress.executed();
			}
			else
				super.execute( command );
		}
	}

	private void upgrade( Command command ) throws SQLException
	{
		Assert.isTrue( this.patch.isInit(), "UPGRADE only allowed in INIT blocks" );
		Assert.isTrue( this.patch.getSource().equals( "1.0" ) && this.patch.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		int pos = command.getLineNumber();
		jdbcExecute( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false, pos ) );
		jdbcExecute( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETE' WHERE RESULT LIKE 'COMPLETED VERSION %'", false, pos ) );
		jdbcExecute( new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false, pos ) ); // We need this because the column is made NOT NULL in the upgrade init block
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
		if( this.conditionFalseCounter == 0 )
		{
			boolean c = this.dbVersion.logContains( version );
			if( not != null )
				c = !c;
			if( c )
				this.conditionTrueCounter++;
			else
				this.conditionFalseCounter++;
		}
		else
			this.conditionFalseCounter++;
	}

	/**
	 * Pop a condition from the stack. If only true conditions remain, skipping of persistent commands is terminated.
	 */
	private void popCondition()
	{
		if( this.conditionFalseCounter > 0 )
			this.conditionFalseCounter--;
		else
		{
			Assert.isTrue( this.conditionTrueCounter > 0 );
			this.conditionTrueCounter--;
		}
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
	 * Write the history to the given file name.
	 * 
	 * @param filename The file name to write to.
	 */
	public void logToXML( String filename )
	{
		if( filename.equals( "-" ) )
			logToXML( System.out );
		else
		{
			try
			{
				logToXML( new FileOutputStream( filename ) );
			}
			catch( FileNotFoundException e )
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
		for( InitConnectionFragment init : this.patchFile.connectionInits )
			if( init.getConnectionName() == null || init.getConnectionName().equalsIgnoreCase( database.getName() ) )
				if( init.getUserName() == null || init.getUserName().equalsIgnoreCase( database.getCurrentUser() ) )
				{
					SQLProcessor processor = new SQLProcessor( this.progress, database );
					processor.setCommandSource( new SQLSource( init.getText(), init.getLineNumber() ) );
					processor.execute();
				}
	}
}
