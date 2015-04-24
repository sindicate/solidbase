/*--
 * Copyright 2009 René M. de Bloois
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

package solidbase.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * Some utilities.
 * 
 * @author René M. de Bloois
 */
public class Util
{
	/**
	 * This utility class cannot be constructed.
	 */
	private Util()
	{
		super();
	}

	/**
	 * Determines if the specified column is present in the resultset.
	 * 
	 * @param resultSet The resultset to check.
	 * @param columnName The column name to look for.
	 * @return True if the column is present in the resultset, false otherwise.
	 * @throws SQLException Can be thrown by JDBC.
	 */
	static public boolean hasColumn( ResultSet resultSet, String columnName ) throws SQLException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columns = metaData.getColumnCount();
		for( int i = 1; i <= columns; i++ )
			if( metaData.getColumnName( i ).equalsIgnoreCase( columnName ) )
				return true;
		return false;
	}
}
