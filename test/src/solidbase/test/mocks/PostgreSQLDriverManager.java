package solidbase.test.mocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import mockit.Mock;
import mockit.MockClass;

// This mock DriverManager (and mock Connection, PreparedStatement) simulate PostgreSQL
// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it
@MockClass(realClass=DriverManager.class)
public class PostgreSQLDriverManager
{
	@Mock(reentrant=true)
	static public Connection getConnection( String url, String user, String password ) throws SQLException
	{
		Connection result = DriverManager.getConnection( url, user, password );
		return new PostgreSQLConnection( result );
	}
}
