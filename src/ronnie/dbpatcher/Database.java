package ronnie.dbpatcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.cmg.pas.SystemException;

public class Database
{
	static protected Connection connection2;
	
	static protected Connection getConnection()
	{
		if( connection2 == null )
		{
			try
			{
				Class.forName( "org.apache.derby.jdbc.EmbeddedDriver" );
			}
			catch( ClassNotFoundException e )
			{
				throw new SystemException( e );
			}
			try
			{
				Database.connection2 = DriverManager.getConnection( "jdbc:derby:c:/projects/java/dbpatcher/derbyDB;create=true" );
			}
			catch( SQLException e )
			{
				throw new SystemException( e );
			}
		}
		return connection2;
	}
}
