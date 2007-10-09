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
	static protected ArrayList listeners = new ArrayList();
	static protected Stack ignoreStack = new Stack();
	static protected HashSet ignoreSet = new HashSet();
	static protected Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE[ \\t]+SQL[ \\t]+ERROR[ \\t]+(\\w+([ \\t]*,[ \\t]*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static protected Pattern ignoreEnd = Pattern.compile( "/IGNORE[ \\t]+SQL[ \\t]+ERROR", Pattern.CASE_INSENSITIVE );
	static protected Pattern setUserPattern = Pattern.compile( "SET[ \\t]+USER[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );
	static protected Pattern startMessagePattern = Pattern.compile( "\\s*MESSAGE\\s+START\\s+'([^']*)'\\s*", Pattern.CASE_INSENSITIVE );
	static protected ProgressListener callBack;
	static protected String defaultUser;
	static protected PatchFile patchFile;
	static protected String patchFileName;
	static protected Database database;
	static protected DBVersion dbVersion;

	static
	{
		listeners.add( new AssertCommandExecuter() );
		listeners.add( new OracleDBMSOutputPoller() );
	}
	
	static public void setPatchFileName( String fileName )
	{
		patchFileName = fileName;
	}

	static public void openPatchFile() throws IOException
	{
		String fileName = patchFileName;
		if( fileName == null )
			fileName = "dbpatch.sql";
		
		LineInputStream lis;
		URL url = Patcher.class.getResource( "/" + fileName ); // In the classpath
		if( url != null )
		{
			lis = new LineInputStream( url );
			callBack.openingPatchFile( "Opening patchfile: " + url );
		}
		else
		{
			File file = new File( fileName ); // In the current folder
			lis = new LineInputStream( new FileInputStream( file ) );
			callBack.openingPatchFile( "Opening patchfile: " + file.getAbsolutePath() );
		}

		patchFile = new PatchFile( lis );
	}

	static public void readPatchFile() throws IOException
	{
		patchFile.read();
	}

	static public void closePatchFile()
	{
		try
		{
			patchFile.close();
		}
		catch( IOException e )
		{
			throw new SystemException( e ); 
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

	static public List getTargets()
	{
		LinkedHashSet result = new LinkedHashSet();
		patchFile.getTargets( dbVersion.getVersion(), dbVersion.getTarget(), result );
		return new ArrayList( result );
	}

	static public void patch( String target ) throws SQLException
	{
		List targets = getTargets();

		for( Iterator iter = targets.iterator(); iter.hasNext(); )
		{
			String t = (String)iter.next();
			if( t.equals( target ) )
			{
				patch( dbVersion.getVersion(), target );
				terminateCommandListeners();
				return;
			}
		}

		throw new SystemException( "Target " + target + " is not a possible target" );
	}

	static protected void terminateCommandListeners()
	{
		for( Iterator iter = listeners.iterator(); iter.hasNext(); )
		{
			CommandListener listener = (CommandListener)iter.next();
			listener.terminate();
		}
	}

	static public void setConnection( Database database, String user )
	{
		Patcher.database = database;
		database.setDefaultUser( user );

		dbVersion = new DBVersion( database );
		dbVersion.setUser( user );

		defaultUser = user; // Overwrites the default user in the Database class at the start of each patch.

		callBack.debug( "driverName=" + database.driverName + ", url=" + database.url + ", user=" + user + "" );
	}

	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = patchFile.getPatches( version, target );
		Assert.isTrue( patches != null );
		Assert.isTrue( patches.size() > 0, "No patches found" );

		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
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

		database.setDefaultUser( defaultUser ); // overwrite the default user at the start of each patch

		dbVersion.read();

		Command command = patchFile.readStatement();
		int count = 0;
		try
		{
			while( command != null )
			{
				String sql = command.getCommand();

				if( command.isRepeatable() )
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
						else
							Assert.fail( "Unknown command [" + sql + "]" );
					}
				}
				else
				{
					count++;
					if( count > skip )
					{
						Patcher.callBack.executing( command, startMessage );

						SQLException sqle = null;
						if( sql.length() > 0 )
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
										sqle = e;
									else
									{
										Patcher.callBack.exception( command );
										throw e;
									}
								}
						}

						if( !patch.isInit() )
						{
							dbVersion.setProgress( patch.getTarget(), count );
							if( sqle != null )
								dbVersion.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), sqle );
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
		database.setDefaultUser( user );
//		Database.getConnection(); // To enter password
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
		database.closeConnections();
	}
}
