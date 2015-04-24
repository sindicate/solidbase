package solidbase.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class Util
{
	static public boolean hasColumn( ResultSet resultSet, String name ) throws SQLException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columns = metaData.getColumnCount();
		for( int i = 1; i <= columns; i++ )
			if( metaData.getColumnName( i ).equalsIgnoreCase( name ) )
				return true;
		return false;
	}
}
