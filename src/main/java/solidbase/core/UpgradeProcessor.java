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

import static solidbase.util.Nulls.nonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import solidbase.util.Assert;
import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.lang.ThreadInterrupted;
import solidstack.script.scopes.MapScope;


/**
 * This class is the coordinator. It requests an upgrade path from the {@link UpgradeFile}, and reads commands from it.
 * It calls the {@link CommandListener}s, updates the DBVERSION and DBVERSIONLOG tables through {@link DBVersion}, calls
 * the {@link Database} to execute statements through JDBC, and shows progress to the user by calling
 * {@link ProgressListener}.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
// TODO Drop all connections at the end of a block?
public class UpgradeProcessor extends CommandProcessor implements ConnectionListener
{
	// Don't need whitespace at the end of the Patterns

	/**
	 * Pattern for TRANSIENT.
	 */
	static protected Pattern transientPattern = Pattern.compile( "TRANSIENT", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for /TRANSIENT.
	 */
	static protected Pattern transientPatternEnd = Pattern.compile( "END\\s+TRANSIENT|/TRANSIENT", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for IF HISTORY [NOT] CONTAINS.
	 */
	static protected Pattern ifHistoryContainsPattern = Pattern.compile( "IF\\s+HISTORY\\s+(NOT\\s+)?CONTAINS\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE );

	/**
	 * Pattern for INCLUDE.
	 */
	static protected Pattern includePattern = Pattern.compile( "INCLUDE\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE );

	// The fields below are all part of the upgrade context. It's reset at the start of each change package.

	/**
	 * All configured databases. This is used when the upgrade file selects a different database by name.
	 */
	// TODO Databases are also in the context
	protected DatabaseContext databases;

	/**
	 * The upgrade file being executed.
	 */
	protected UpgradeFile upgradeFile;

	/**
	 * The class that manages the DBVERSION and DBVERSIONLOG table.
	 */
	protected DBVersion dbVersion;

	/**
	 * The upgrade segment that is currently processed.
	 */
	protected UpgradeSegment segment;

	/**
	 * The upgrade execution context.
	 */
	// TODO This is redundant and risky. Also in SQLProcessor and CommandContext.
	protected UpgradeContext upgradeContext;

	/**
	 * Parameters.
	 */
	protected Map<String, Object> parameters;

	/**
	 * Constructor.
	 *
	 * @param listener Listens to the progress.
	 */
	public UpgradeProcessor( ProgressListener listener ) {
		super( listener );
	}

	/**
	 * Sets all configured databases.
	 *
	 * @param databases All configured databases.
	 */
	public void setDatabases( DatabaseContext databases ) {
		this.databases = databases;
	}

	/**
	 * Sets the upgrade execution context.
	 *
	 * @param context The upgrade execution context.
	 */
	public void setContext( UpgradeContext context ) {
		this.context = context;
		upgradeContext = context;
	}

	/**
	 * Sets the upgrade file.
	 *
	 * @param file The upgrade file to set.
	 */
	public void setUpgradeFile( UpgradeFile file ) {
		upgradeFile = file;
	}

	/**
	 * Sets the parameters.
	 *
	 * @param parameters The parameters to set.
	 */
	public void setParameters( Map<String, Object> parameters ) {
		this.parameters = parameters;
	}

	/**
	 * Initialize.
	 */
	// TODO Remove this init, should be in the constructor
	public void init() {
		dbVersion = new DBVersion( getDefaultDatabase(), progress, upgradeFile.versionTableName, upgradeFile.logTableName );
	}

	/**
	 * Returns the default database.
	 *
	 * @return The default database.
	 */
	@Override
	public Database getDefaultDatabase() {
		return databases.getDatabase( "default" );
	}

	@Override
	public void end() {
		for( Database database : databases.getDatabases() ) {
			database.closeConnections();
		}
		upgradeFile.close();
	}

	/**
	 * Get the current version of the database.
	 *
	 * @return The current version of the database.
	 */
	public String getCurrentVersion() {
		Assert.notNull( dbVersion, "default database may not have been configured" );
		return dbVersion.getVersion();
	}

	/**
	 * Get the current target.
	 *
	 * @return The current target.
	 */
	public String getCurrentTarget() {
		return dbVersion.getTarget();
	}

	/**
	 * Get the number of persistent statements executed successfully.
	 *
	 * @return The number of persistent statements executed successfully.
	 */
	public int getCurrentStatements() {
		return dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets from the current version. The current version is also considered.
	 *
	 * @param tips If true only the tips of the upgrade paths are returned.
	 * @param prefix Only return targets that start with the given prefix.
	 * @param downgradeable Also consider downgrade paths.
	 * @return All possible targets from the current version.
	 */
	// TODO Why is this a LinkedHashSet?
	public LinkedHashSet<String> getTargets( boolean tips, String prefix, boolean downgradeable ) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		upgradeFile.collectTargets( dbVersion.getVersion(), dbVersion.getTarget(), tips, downgradeable, prefix, result );
		return result;
	}

	/**
	 * Use the 'setup' change sets to upgrade the DBVERSION and DBVERSIONLOG tables to the newest specification.
	 *
	 * @throws ProcessException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void setupControlTables() throws ProcessException { // TODO This is a RuntimeException, remove or not? Keep in Javadoc?
		String spec = dbVersion.getSpec();

		List<UpgradeSegment> segments = upgradeFile.getSetupPath( spec );
		if( segments == null ) {
			return;
		}

		Assert.notEmpty( segments );

		// SETUP blocks get special treatment.
		for( UpgradeSegment segment : segments ) {
			process( segment );
			dbVersion.updateSpec( segment.getTarget() );
			if( Thread.currentThread().isInterrupted() )
			 {
				throw new ThreadInterrupted();
			// TODO How do we get a more dramatic error message here, if something goes wrong?
			}
		}
	}

	/**
	 * Upgrade to the given target.
	 *
	 * @param target The target to upgrade to.
	 * @throws ProcessException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	public void upgrade( String target ) throws ProcessException {
		upgrade( target, false );
	}

	/**
	 * Perform upgrade to the given target version. The target version can end with an '*', indicating whatever tip
	 * version that matches the target prefix.
	 *
	 * @param target The target requested.
	 * @param downgradeable Indicates that downgrade paths are allowed to reach the given target.
	 * @throws ProcessException When the execution of a command throws an {@link SQLException}.
	 */
	protected void upgrade( String target, boolean downgradeable ) throws ProcessException {
		setupControlTables();

		String version = dbVersion.getVersion();

		if( target == null ) {
			LinkedHashSet<String> targets = getTargets( true, null, downgradeable );
			if( targets.size() > 1 ) {
				throw new FatalException( "More than one possible target found, you should specify a target." );
			}
			Assert.notEmpty( targets );
			target = targets.iterator().next();

		} else if( target.endsWith( "*" ) ) {
			String targetPrefix = target.substring( 0, target.length() - 1 );
			LinkedHashSet<String> targets = getTargets( true, targetPrefix, downgradeable );
			if( targets.size() > 1 ) {
				throw new FatalException( "More than one possible target found for " + target );
			}
			if( targets.isEmpty() ) {
				throw new FatalException( "Target " + target + " is not reachable from version " + StringUtils.defaultString( version, "<no version>" ) );
			}
			target = targets.iterator().next();

		} else {
			LinkedHashSet<String> targets = getTargets( false, null, downgradeable );
			Assert.notEmpty( targets );

			// TODO Refactor this, put this in getTargets()
			boolean found = false;
			for( String t : targets ) {
				if( Objects.equals( t, target ) ) {
					found = true;
					break;
				}
			}

			if( !found ) {
				throw new FatalException( "Target " + target + " is not reachable from version " + StringUtils.defaultString( version, "<no version>" ) );
			}
		}

		if( Objects.equals( target, version ) ) {
			progress.noUpgradeNeeded();
			return;
		}

		upgrade( version, target, downgradeable );
	}

	/**
	 * Upgrade the database from the given version to the given target.
	 *
	 * @param version The current version of the database.
	 * @param target The target to upgrade to.
	 * @param downgradeable Allow downgrades to reach the target
	 * @throws ProcessException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void upgrade( String version, String target, boolean downgradeable ) throws ProcessException {
		if( target.equals( version ) ) {
			return;
		}

		Path path = upgradeFile.getUpgradePath( version, target, downgradeable );
		Assert.isTrue( path.size() > 0, "No upgrades found" );

		for( UpgradeSegment segment : path ) {
			process( segment );
			if( Thread.currentThread().isInterrupted() ) {
				throw new ThreadInterrupted();
			}
		}
	}

	/**
	 * Reads a command from the upgrade source.
	 *
	 * @return The command read.
	 */
	protected Command readCommand() {
		while( true ) {
			Command command = upgradeContext.getSource().readCommand();
			if( command != null ) {
				return command;
			}
			UpgradeContext parent = (UpgradeContext)upgradeContext.getParent();
			if( parent == null ) {
				return null;
			}
			setContext( parent );
		}
	}

	/**
	 * Execute a upgrade segment.
	 *
	 * @param segment The segment to be executed.
	 * @throws ProcessException Whenever an {@link SQLException} occurs during the execution of a command.
	 */
	protected void process( UpgradeSegment segment ) throws ProcessException {
		progress.reset();
		progress.upgradeStarting( nonNull( segment ) );

		// TODO Different levels of contexts and scopes, maybe merge them?
		UpgradeContext context = new UpgradeContext( upgradeFile.gotoSegment( segment ) );
		context.setDatabases( databases );
		if( parameters != null ) {
			context.swapScope( new MapScope( parameters, context.getScope() ) );
		}
		setContext( context );
		this.context.setCurrentDatabase( getDefaultDatabase() );
		this.context.getCurrentDatabase().resetUser();

		// Determine how many to skip
		int skipCount = 0;
		if( dbVersion.getTarget() != null ) {
			skipCount = dbVersion.getStatements();
		}

		int count = 0;
		this.segment = segment;
		try {
			Command command = readCommand();
			while( command != null ) {
				if( !command.isAnnotation() && !upgradeContext.isTransient() && !segment.isSetup() ) {
					boolean windForward = count < skipCount;
					count++;
					try {
						ProcessException result = executeWithListeners( command, windForward || this.context.skipping() );
						if( !windForward ) {
							// We have to update the progress even if the logging fails. Otherwise the segment cannot be
							// restarted. That's why the progress update is first. But some logging will be lost in that case.
							dbVersion.updateProgress( segment.getTarget(), count );
							if( result != null ) {
								dbVersion.logSQLException( segment, count, command.getCommand(), result );
							} else {
								dbVersion.log( segment, count, command.getCommand() );
							}
						}
					} catch( ProcessException e ) {
						// TODO We need a unit test for this, and the above
						dbVersion.logSQLException( segment, count, command.getCommand(), e );
						throw e;
					}
				} else {
					executeWithListeners( command, false );
				}

				if( !segment.isSetup() ) {
					if( Thread.currentThread().isInterrupted() ) {
						throw new ThreadInterrupted();
					}
				}

				command = readCommand();
			}

			progress.upgradeFinished();

			dbVersion.setStale(); // TODO With a normal segment, only set stale if not both of the 2 version tables are found
			if( segment.isSetup() ) {
				dbVersion.updateSpec( segment.getTarget() );
				Assert.isFalse( segment.isOpen() );
			} else {
				if( segment.isDowngrade() ) {
					Set<String> versions = upgradeFile.getReachableVersions( segment.getTarget(), null, false );
					versions.remove( segment.getTarget() );
					dbVersion.downgradeHistory( versions );
				}
				if( !segment.isOpen() ) {
					dbVersion.updateVersion( segment.getTarget() );
					dbVersion.logComplete( segment, count );
				}
			}
		} finally {
			this.segment = null;
		}
	}

	@Override
	protected boolean executeListeners( Command command, boolean skip ) throws SQLException {
		if( command.isAnnotation() ) {
			String sql = command.getCommand();
			Matcher matcher;
			if( transientPattern.matcher( sql ).matches() ) {
				startTransient( command.getLocation() );
				return true;
			}
			if( transientPatternEnd.matcher( sql ).matches() ) {
				stopTransient( command.getLocation() );
				return true;
			}
			if( ( matcher = ifHistoryContainsPattern.matcher( sql ) ).matches() ) {
				ifHistoryContains( matcher.group( 1 ), matcher.group( 2 ) );
				return true;
			}
			if( ( matcher = includePattern.matcher( sql ) ).matches() ) {
				include( matcher.group( 1 ) );
				return true;
			}
		}

		return super.executeListeners( command, skip );
	}

	@Override
	protected void executeJdbc( Command command ) throws SQLException {
		if( command.getCommand().trim().equalsIgnoreCase( "UPGRADE" ) ) {
			upgradeControlTables( command );
		} else {
			super.executeJdbc( command );
		}
	}

	private void upgradeControlTables( Command command ) throws SQLException {
		// TODO Can we put the segment in the command? Don't like this field.
		Assert.isTrue( segment.isSetup(), "UPGRADE only allowed in SETUP blocks" );
		Assert.isTrue( segment.getSource().equals( "1.0" ) && segment.getTarget().equals( "1.1" ), "UPGRADE only possible from spec 1.0 to 1.1" );

		SourceLocation location = command.getLocation();
		executeJdbc( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'S' WHERE RESULT IS NULL OR RESULT NOT LIKE 'COMPLETED VERSION %'", false, location ) );
		executeJdbc( new Command( "UPDATE DBVERSIONLOG SET TYPE = 'B', RESULT = 'COMPLETE' WHERE RESULT LIKE 'COMPLETED VERSION %'", false, location ) );
		executeJdbc( new Command( "UPDATE DBVERSION SET SPEC = '1.1'", false, location ) ); // We need this because the column is made NOT NULL in the upgrade setup block
	}

	/**
	 * Persistent commands should be considered transient.
	 *
	 * @param location Location of the TRANSIENT annotation.
	 */
	protected void startTransient( SourceLocation location ) {
		if( upgradeContext.isTransient() ) {
			throw new SourceException( "TRANSIENT already enabled", location );
		}
		upgradeContext.setTransient( true );
	}

	/**
	 * Persistent commands should be considered persistent again.
	 *
	 * @param location Location of the END TRANSIENT annotation.
	 */
	protected void stopTransient( SourceLocation location ) {
		if( !upgradeContext.isTransient() ) {
			throw new SourceException( "TRANSIENT is not enabled", location );
		}
		upgradeContext.setTransient( false );
	}

	/**
	 * If history does not contain the given version then start skipping the persistent commands. If <code>not</code> is
	 * true then this logic is reversed.
	 *
	 * @param not True causes reversed logic.
	 * @param version The version to look for in the history.
	 */
	private void ifHistoryContains( String not, String version ) {
		boolean c = dbVersion.logContains( version );
		if( not != null ) {
			c = !c;
		}
		context.skip( !c );
	}

	/**
	 * Include a file for upgrade.
	 *
	 * @param url The URL of the file.
	 */
	protected void include( String url ) {
		SQLFile file = Factory.openSQLFile( getResource().resolve( url ), progress );
		setContext( new UpgradeContext( upgradeContext, file.getSource() ) );
	}

	@Override
	protected void setDelimiters( Delimiter[] delimiters ) {
		upgradeContext.getSource().setDelimiters( delimiters );
	}

	/**
	 * Write the history to the given output stream.
	 *
	 * @param out The output stream to write to.
	 */
	public void logToXML( OutputStream out ) {
		dbVersion.logToXML( out, Charset.forName( "UTF-8" ) );
	}

	/**
	 * Write the log to the given resource.
	 *
	 * @param output The resource to write the log to.
	 */
	public void logToXML( Resource output ) {
		try( OutputStream out = output.newOutputStream() ) {
			logToXML( out );
		} catch( IOException e ) {
			throw new FatalIOException( e );
		}
	}

	/**
	 * Returns a statement of the current version of the database in a user presentable form.
	 *
	 * @return A statement of the current version of the database in a user presentable form.
	 */
	public String getVersionStatement() {
		return dbVersion.getVersionStatement();
	}

	@Override
	public void connected( Database database ) {
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
	public SourceReader getReader() {
		return upgradeFile.reader;
	}

	@Override
	public Resource getResource() {
		return upgradeFile.reader.getResource();
	}

	@Override
	public boolean implicitCommit() {
		return true;
	}

}
