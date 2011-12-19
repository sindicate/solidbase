package solidbase.util;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import solidbase.core.SystemException;

public class JdbcSupport
{
	static private Map< Integer, String > typeNames;

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

	static public String getTypeName( int type )
	{
		if( typeNames == null )
		{
			typeNames = new HashMap< Integer, String >();
			for( Field field : Types.class.getFields() )
				try
				{
					Object object = field.get( null );
					if( object instanceof Integer )
						typeNames.put( (Integer)object, field.getName() );
				}
				catch( IllegalAccessException e )
				{
					throw new SystemException( e );
				}
		}

		String result = typeNames.get( type );
		Assert.notNull( result, "Unknown JDBC type " + type );
		return result;
	}
}
