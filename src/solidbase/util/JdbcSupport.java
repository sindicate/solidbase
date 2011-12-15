package solidbase.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class JdbcSupport
{
	// ResultSet.getObject returns objects that are not always of the correct types
	// For example oracle.sql.TIMESTAMP or org.hsqldb.types.BlobDataID are not instances of java.sql.Timestamp or java.sql.Blob
	// TODO Maybe implement getValues which returns the complete row
	static public Object getValue( ResultSet result, int[] types, int index ) throws SQLException
	{
	    // TODO ROWID, NCHAR, NVARCHAR, LONGNVARCHAR, NCLOB and SQLXML are Java 6/JDBC 4. How to deal with that?

		int type = types[ index ];
		index++;
		switch( type )
		{
			case Types.TIMESTAMP:
				return result.getTimestamp( index );
			case Types.BLOB:
				return result.getBlob( index );
			case Types.CLOB:
				return result.getClob( index );
//			case Types.NCLOB:
//				return result.getNClob( index );
		}
		return result.getObject( index );
	}
}
