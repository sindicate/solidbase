package ronnie.dbpatcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.io.LineInputStream;
import com.logicacmg.idt.commons.util.Assert;


/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Patcher
{
	static protected Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE[ \\t]+SQL[ \\t]+ERROR[ \\t]+(\\w+([ \\t]*,[ \\t]*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static protected Pattern ignoreEnd = Pattern.compile( "/IGNORE[ \\t]+SQL[ \\t]+ERROR", Pattern.CASE_INSENSITIVE );
	static protected Pattern setUserPattern = Pattern.compile( "SET[ \\t]+USER[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );
	static protected Pattern startMessagePattern = Pattern.compile( "\\s*MESSAGE\\s+START\\s+'([^']*)'\\s*", Pattern.CASE_INSENSITIVE );
	static protected Pattern sessionConfigPattern = Pattern.compile( "SESSIONCONFIG", Pattern.CASE_INSENSITIVE );
	static protected Pattern sessionConfigPatternEnd = Pattern.compile( "/SESSIONCONFIG", Pattern.CASE_INSENSITIVE );

	static protected List< CommandListener > listeners = new ArrayList();
	static protected Stack ignoreStack = new Stack();
	static protected HashSet ignoreSet = new HashSet();
	static protected boolean dontCount;

	static protected ProgressListener callBack;
	static protected String defaultUser;
	static protected PatchFile patchFile;
	static protected Database database;
	static protected DBVersion dbVersion;

	static
	{
		listeners.add( new AssertCommandExecuter() );
		listeners.add( new OracleDBMSOutputPoller() );
	}

	static public void openPatchFile( String fileName ) throws IOException
	{
		if( fileName == null )
			fileName = "dbpatch.sql";

		LineInputStream lis;
		URL url = Patcher.class.getResource( "/" + fileName ); // In the classpath
		if( url != null )
		{
			callBack.openingPatchFile( url.toString() );
			lis = new LineInputStream( url );
		}
		else
		{
			File file = new File( fileName ); // In the current folder
			callBack.openingPatchFile( file.getAbsolutePath() );
			lis = new LineInputStream( new FileInputStream( file ) );
		}

		patchFile = new PatchFile( lis );

		// Need to close in case of an exception during reading
		try
		{
			patchFile.read();
		}
		catch( RuntimeException e )
		{
			patchFile.close();
			throw e;
		}
		catch( IOException e )
		{
			patchFile.close();
			throw e;
		}
	}

	static public void closePatchFile()
	{
		if( patchFile != null )
		{
			try
			{
				patchFile.close();
			}
			catch( IOException e )
			{
				throw new SystemException( e );
			}
		}
		patchFile = null;
	}

	static public String getCurrentVersion()
	{
		return dbVersion.getVersion();
	}

	static public String getCurrentTarget()
	{
		return dbVersion.getTarget();
	}

	static public int getCurrentStatements()
	{
		return dbVersion.getStatements();
	}

	/**
	 * Returns all possible targets.
	 * 
	 * @param tips If true only the tips of the patch paths are returned.
	 * @return
	 */
	static public LinkedHashSet< String > getTargets( boolean tips )
	{
		LinkedHashSet result = new LinkedHashSet();
		patchFile.collectTargets( dbVersion.getVersion(), dbVersion.getTarget(), tips, result );
		return result;
	}

	/**
	 * Patches to the given target version. The target version can end with an '*', indicating whatever tip version that matches the target prefix.
	 * 
	 * @param target
	 * @throws SQLException
	 */
	static public void patch( String target ) throws SQLException
	{
		boolean wildcard = target.endsWith( "*" );
		if( wildcard )
		{
			String targetPrefix = target.substring( 0, target.length() - 1 );
			Set< String > targets = getTargets( true );
			for( String t : targets )
				if( t.startsWith( targetPrefix ) )
				{
					patch( dbVersion.getVersion(), t );
					break;
				}

			terminateCommandListeners();
			// TODO Remove the possibility that the version is null?
			if( dbVersion.getVersion() != null && dbVersion.getVersion().startsWith( targetPrefix ) )
				callBack.patchingFinished();
			else
				throw new SystemException( "Target " + target + " is not a possible target" );

			return;
		}

		Set< String > targets = getTargets( false );
		for( String t : targets )
			if( t.equals( target ) )
			{
				patch( dbVersion.getVersion(), t );
				break;
			}

		terminateCommandListeners();
		if( dbVersion.getVersion() != null && dbVersion.getVersion().equals( target ) )
			callBack.patchingFinished();
		else
			throw new SystemException( "Target " + target + " is not a possible target" );
	}

	static protected void terminateCommandListeners()
	{
		for( CommandListener listener : listeners )
			listener.terminate();
	}

	/**
	 * Configures the connection to the database, including the default user.
	 * Each patch in the patch file starts with this default user.
	 * 
	 * @param database
	 * @param defaultUser
	 */
	static public void setConnection( Database database, String defaultUser, String passWord )
	{
		Patcher.database = database;
		database.setCurrentUser( defaultUser );
		if( passWord != null )
			database.initConnection( defaultUser, passWord ); // This prevents the password being requested from the user.

		dbVersion = new DBVersion( database );
		dbVersion.setUser( defaultUser );

		Patcher.defaultUser = defaultUser; // Overwrites the default user in the Database class at the start of each patch.

		callBack.debug( "driverName=" + database.driverName + ", url=" + database.url + ", user=" + defaultUser + "" );
	}

	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = patchFile.getPatchPath( version, target );
		Assert.isTrue( patches != null );
		Assert.isTrue( patches.size() > 0, "No patches found" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}

	static protected SQLException execute( Command command ) throws SQLException
	{
		Assert.isTrue( command.isNonRepeatable() );

		String sql = command.getCommand();
		if( sql.length() > 0 )
		{
			for( Iterator iter = listeners.iterator(); iter.hasNext(); )
			{
				CommandListener listener = (CommandListener)iter.next();
				if( listener.execute( database, command ) )
					return null;
			}

			try
			{
				Statement statement = database.getConnection().createStatement();
				try
				{
					statement.execute( sql ); // autocommit is on
				}
				finally
				{
					statement.close();
				}
			}
			catch( SQLException e )
			{
				String error = e.getSQLState();
				if( ignoreSet.contains( error ) )
					return e;
				Patcher.callBack.exception( command );
				throw e;
			}
		}

		return null;
	}

	static protected void patch( Patch patch ) throws SQLException
	{
		Assert.notNull( patch, "patch == null" );

		Patcher.callBack.patchStarting( patch.getSource(), patch.getTarget() );

		patchFile.gotoPatch( patch );
		int skip = dbVersion.getStatements();
		if( dbVersion.getTarget() == null )
			skip = 0;

		String startMessage = null;

		database.setCurrentUser( defaultUser ); // overwrite the default user at the start of each patch

		dbVersion.read();

		Command command = patchFile.readStatement();
		int count = 0;
		try
		{
			while( command != null )
			{
				String sql = command.getCommand();

				if( command.isRepeatable() ) // TODO Rename to isDBPatcherCommand
				{
					boolean done = false;
					for( Iterator iter = listeners.iterator(); iter.hasNext(); )
					{
						CommandListener listener = (CommandListener)iter.next();
						done = listener.execute( database, command );
						if( done )
							break;
					}

					if( !done )
					{
						Matcher matcher;
						if( ( matcher = ignoreSqlErrorPattern.matcher( sql ) ).matches() )
							pushIgnores( matcher.group( 1 ) );
						else if( ignoreEnd.matcher( sql ).matches() )
							popIgnores();
						else if( ( matcher = setUserPattern.matcher( sql ) ).matches() )
							setUser( matcher.group( 1 ) );
						else if( ( matcher = startMessagePattern.matcher( sql ) ).matches() )
							startMessage = matcher.group( 1 );
						else if( ( matcher = sessionConfigPattern.matcher( sql ) ).matches() )
							enableDontCount();
						else if( ( matcher = sessionConfigPatternEnd.matcher( sql ) ).matches() )
							disableDontCount();
						else
							Assert.fail( "Unknown command [" + sql + "]" );
					}
				}
				else if( dontCount )
				{
					Patcher.callBack.executing( command, startMessage );
					execute( command );
					Patcher.callBack.executed();

					startMessage = null;
				}
				else
				{
					count++;
					if( count > skip )
					{
						Patcher.callBack.executing( command, startMessage );
						SQLException sqlException = execute( command );
						if( !patch.isInit() )
						{
							dbVersion.setProgress( patch.getTarget(), count );
							if( sqlException != null )
								dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), sqlException );
							else
								dbVersion.log( patch.getSource(), patch.getTarget(), count, sql, (String)null );
						}
						Patcher.callBack.executed();
					}
					else
						Patcher.callBack.skipped( command );

					startMessage = null;
				}

				command = patchFile.readStatement();
			}
			Patcher.callBack.patchFinished();

			dbVersion.read();

			if( !patch.isOpen() )
			{
				dbVersion.setVersion( patch.getTarget() );
				dbVersion.log( patch.getSource(), patch.getTarget(), count, null, "COMPLETED VERSION " + patch.getTarget() );
			}
		}
		catch( RuntimeException e )
		{
			dbVersion.log( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw e;
		}
		catch( SQLException e )
		{
			dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw e;
		}
	}

	static protected void setUser( String user )
	{
		database.setCurrentUser( user );
		// Database.getConnection(); // To enter password
	}

	static protected void pushIgnores( String ignores )
	{
		String[] ss = ignores.split( "," );
		for( int i = 0; i < ss.length; i++ )
			ss[ i ] = ss[ i ].trim();
		ignoreStack.push( ss );
		refreshIgnores();
	}

	static protected void popIgnores()
	{
		ignoreStack.pop();
		refreshIgnores();
	}

	static protected void refreshIgnores()
	{
		HashSet ignores = new HashSet();
		for( Iterator iter = ignoreStack.iterator(); iter.hasNext(); )
		{
			String[] ss = (String[])iter.next();
			for( int i = 0; i < ss.length; i++ )
				ignores.add( ss[ i ] );
		}
		ignoreSet = ignores;
	}

	static protected void enableDontCount()
	{
		Assert.isFalse( dontCount, "Counting already enabled" );
		dontCount = true;
	}

	static protected void disableDontCount()
	{
		Assert.isTrue( dontCount, "Counting already disabled" );
		dontCount = false;
	}

	static public ProgressListener getCallBack()
	{
		return Patcher.callBack;
	}

	static public void setCallBack( ProgressListener callBack )
	{
		Patcher.callBack = callBack;
	}

	static public void logToXML( OutputStream out )
	{
		dbVersion.logToXML( out );
	}

	static public void end()
	{
		closePatchFile();
		if( database != null )
			database.closeConnections();
	}
}
