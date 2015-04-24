package ronnie.dbpatcher.core;

import java.sql.Connection;
import java.sql.SQLException;

import com.logicacmg.idt.commons.SystemException;

public class DatabaseTestUtil
{
	static public void shutdown( Database database )
	{
		Connection connection = database.getConnection();
		try
		{
			connection.createStatement().executeUpdate( "SHUTDOWN" );
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
	}
}
