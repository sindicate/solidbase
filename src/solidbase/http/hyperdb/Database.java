package solidbase.http.hyperdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import solidbase.http.HttpException;

public class Database
{
	static protected List< Table > tableCache;

	synchronized static public List< Table > getTables()
	{
		if( tableCache != null )
			return tableCache;

		String sql = "SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME";
//		System.out.println( "SQL: " + sql );

		List< Table > tables = new ArrayList< Table >();
		try
		{
			Connection connection = DataSource.getConnection();
			try
			{
				Statement statement = connection.createStatement();
				try
				{
					ResultSet result = statement.executeQuery( sql );
					while( result.next() )
						tables.add( new Table( result.getString( 1 ) ) );

//					for( Table table : tables )
//					{
//						result = statement.executeQuery( "SELECT COUNT(*) FROM " + table.name );
//						Assert.isTrue( result.next() );
//						table.records = result.getInt( 1 );
//					}

					tableCache = tables;
				}
				finally
				{
					statement.close();
				}
			}
			finally
			{
				DataSource.release( connection );
			}
		}
		catch( SQLException e )
		{
			throw new HttpException( e );
		}

		return tables;
	}
}
