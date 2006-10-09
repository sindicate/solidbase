package ronnie.dbpatcher.core;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.logicacmg.idt.commons.SystemException;
import com.logicacmg.idt.commons.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class Patcher
{
	static protected ArrayList plugins = new ArrayList();
	static protected Stack ignoreStack = new Stack();
	static protected HashSet ignoreSet = new HashSet();
	static protected Pattern ignoreSqlErrorPattern = Pattern.compile( "IGNORE[ \\t]+SQL[ \\t]+ERROR[ \\t]+(\\w+([ \\t]*,[ \\t]*\\w+)*)", Pattern.CASE_INSENSITIVE );
	static protected Pattern ignoreEnd = Pattern.compile( "/IGNORE[ \\t]+SQL[ \\t]+ERROR", Pattern.CASE_INSENSITIVE );
	static protected Pattern setUserPattern = Pattern.compile( "SET[ \\t]+USER[ \\t]+(\\w+)[ \\t]*", Pattern.CASE_INSENSITIVE );
	static protected Pattern startMessagePattern = Pattern.compile( "\\s*MESSAGE\\s+START\\s+'([^']*)'\\s*", Pattern.CASE_INSENSITIVE );
	static protected ProgressListener callBack;
	static protected String defaultUser;
	
	static
	{
		plugins.add( new AssertPlugin() );
		plugins.add( new OracleDBMSOutputPlugin() );
	}
	
	static public void openPatchFile() throws IOException
	{
		PatchFile.open();
	}
	
	static public void readPatchFile() throws IOException
	{
		PatchFile.read();
	}
	
	static public void closePatchFile() throws IOException
	{
		PatchFile.close();
	}
	
	static public String getCurrentVersion()
	{
		return DBVersion.getVersion();
	}
	
	static public String getCurrentTarget()
	{
		return DBVersion.getTarget();
	}
	
	static public int getCurrentStatements()
	{
		return DBVersion.getStatements();
	}
	
	static public List getTargets()
	{
		return 	PatchFile.getTargets( DBVersion.getVersion() );
	}
	
	static public void patch( String target ) throws SQLException
	{
		List targets = PatchFile.getTargets( DBVersion.getVersion() );
		for( Iterator iter = targets.iterator(); iter.hasNext(); )
		{
			String s = (String)iter.next();
			if( s.equals( target ) )
			{
				patch( DBVersion.getVersion(), target );
				terminatePlugins();
				return;
			}
		}
		
		throw new SystemException( "Target " + target + " is not a possible target" );
	}

	static protected void terminatePlugins()
	{
		for( Iterator iter = plugins.iterator(); iter.hasNext(); )
		{
			Plugin plugin = (Plugin)iter.next();
			plugin.terminate();
		}
	}

	static public void setConnection( String driverName, String url, String user )
	{
		Database.setConnection( driverName, url );
		DBVersion.setUser( user );
		Database.setDefaultUser( user );

		defaultUser = user; // Overwrites the default user in the Database class at the start of each patch.
	}
	
	static public String getVersion()
	{
		Properties properties = new Properties();
		try
		{
			properties.load( Patcher.class.getResourceAsStream( "core.properties" ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		String result = properties.getProperty( "core.version" );
		Assert.isTrue( result != null );
		return result;
	}
	
	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = PatchFile.getPatches( version, target );
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
		
		PatchFile.gotoPatch( patch );
		int skip = DBVersion.getStatements();
		if( DBVersion.getTarget() == null )
			skip = 0;
		
		String startMessage = null;
		
		Database.setDefaultUser( defaultUser ); // overwrite the default user at the start of each patch
		
		Command command = PatchFile.readStatement();
		int count = 0;
		try
		{
			while( command != null )
			{
				String sql = command.getCommand();
	
				if( !command.isCounting() )
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
					{
						boolean done = false;
						for( Iterator iter = plugins.iterator(); iter.hasNext(); )
						{
							Plugin plugin = (Plugin)iter.next();
							done = plugin.execute( command );
							if( done )
								break;
						}
						if( !done )
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
							for( Iterator iter = plugins.iterator(); iter.hasNext(); )
							{
								Plugin plugin = (Plugin)iter.next();
								done = plugin.execute( command );
								if( done )
									break;
							}
							if( !done )
								try
								{
									Statement statement = Database.getConnection().createStatement();
									statement.execute( sql ); // autocommit is on
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
							DBVersion.setCount( patch.getTarget(), count );
							if( sqle != null )
								DBVersionLog.logSQLException( patch.getSource(), patch.getTarget(), count, command.getCommand(), sqle );
							else
								DBVersionLog.log( patch.getSource(), patch.getTarget(), count, sql, (String)null );
						}
						
						Patcher.callBack.executed();
					}
					else
						Patcher.callBack.skipped( command );
					
					startMessage = null;
				}
				
				command = PatchFile.readStatement();
			}
			Patcher.callBack.patchFinished();
	
			if( patch.isInit() )
				DBVersion.versionTablesCreated();
			if( !patch.isOpen() )
			{
				DBVersion.setVersion( patch.getTarget() );
				DBVersionLog.log( patch.getSource(), patch.getTarget(), count, null, "COMPLETED VERSION " + patch.getTarget() );
			}
		}
		catch( RuntimeException e )
		{
			DBVersionLog.log( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw e;
		}
		catch( SQLException e )
		{
			DBVersionLog.logSQLException( patch.getSource(), patch.getTarget(), count, command == null ? null : command.getCommand(), e );
			throw e;
		}
	}

	static protected void setUser( String user )
	{
		Database.setDefaultUser( user );
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
		DBVersionLog.logToXML( out );
	}
}
