package ronnie.dbpatcher.core;

import java.io.IOException;
import java.sql.Connection;
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

import com.cmg.pas.SystemException;
import com.cmg.pas.util.Assert;

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
	
	static
	{
		plugins.add( new AssertPlugin() );
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
				return;
			}
		}
		
		throw new SystemException( "Target " + target + " is not a possible target" );
	}

	static public void setConnection( String driverName, String url )
	{
		Database.setConnection( driverName, url );
	}
	
	static public void setConnection( Connection connection )
	{
		Database.setConnection( connection );
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
		Assert.check( result != null );
		return result;
	}
	
	static protected void patch( String version, String target ) throws SQLException
	{
		List patches = PatchFile.getPatches( version, target );
		Assert.check( patches != null );
		Assert.check( patches.size() > 0, "No patches found" );
		
//		System.out.println( "patch path for " + version + " to " + target + ":" );
		for( Iterator iter = patches.iterator(); iter.hasNext(); )
		{
			Patch patch = (Patch)iter.next();
			patch( patch );
		}
	}
	
	static protected void patch( Patch patch ) throws SQLException
	{
		System.out.print( "Patching \"" + patch.getSource() + "\" to \"" + patch.getTarget() + "\"" );
		
		PatchFile.gotoPatch( patch );
		int skip = DBVersion.getStatements();
		if( DBVersion.getTarget() == null )
			skip = 0;
		
		Statement statement = Database.getConnection().createStatement();
		Command command = PatchFile.readStatement();
		int count = 0;
		while( command != null )
		{
			String sql = command.getCommand();
//			System.out.println( sql );
//			System.out.println();

			if( !command.isCounting() )
			{
				Matcher matcher = ignoreSqlErrorPattern.matcher( sql );
				if( matcher.matches() )
					pushIgnores( matcher.group( 1 ) );
				else if( ignoreEnd.matcher( sql ).matches() )
					popIgnores();
				else
					Assert.fail( "Unknown command [" + sql + "]" );
			}
			else
			{
				if( count >= skip )
				{
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
								statement.execute( sql ); // autocommit is on
							}
							catch( SQLException e )
							{
								String error = e.getSQLState();
								if( !ignoreSet.contains( error ) )
								{
									System.err.println( "Exception while executing \n" + sql );
									throw e;
								}
							}
					}
					System.out.print( "." );
				}
				if( !patch.isInit() )
					DBVersion.setCount( patch.getTarget(), ++count );
			}
			
			command = PatchFile.readStatement();
		}
		System.out.println();

		if( patch.isInit() )
			DBVersion.versionTablesCreated();
		if( !patch.isOpen() )
			DBVersion.setVersion( patch.getTarget() );
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
}
