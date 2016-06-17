/*--
 * Copyright 2012 Ren� M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.util;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import solidbase.core.SystemException;


/**
 * JDBC support class.
 *
 * @author Ren� de Bloois
 */
public class JDBCSupport
{
	static private Map< Integer, String > typeNames;
	static private Map< String, Integer > typeNames2;

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
		}
		return result.getObject( index );
	}

	static private void initTypeNames()
	{
		typeNames = new HashMap< Integer, String >();
		typeNames2 = new HashMap< String, Integer >();
		for( Field field : Types.class.getFields() )
			try
			{
				Object object = field.get( null );
				if( object instanceof Integer )
				{
					typeNames.put( (Integer)object, field.getName() );
					typeNames2.put( field.getName(), (Integer)object );
				}
			}
			catch( IllegalAccessException e )
			{
				throw new SystemException( e );
			}
	}

	static public String toTypeName( int type )
	{
		if( typeNames == null )
			initTypeNames(); // TODO Move to a static initializer

		String result = typeNames.get( type );
//		if( result == null )
//			return Integer.toString( type );
//		Assert.notNull( result, "Unknown JDBC type " + type );
		return result;
	}

	static public int fromTypeName( String type )
	{
		if( typeNames == null )
			initTypeNames(); // TODO Move to a static initializer

		Integer result = typeNames2.get( type );
		Assert.notNull( result, "Unknown JDBC type: " + type );
		return result;
	}
}
