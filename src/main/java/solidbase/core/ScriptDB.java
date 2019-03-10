/*--
 * Copyright 2013 René M. de Bloois
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import solidstack.script.objects.Tuple;

public class ScriptDB
{
	private CommandContext context;

	public ScriptDB( CommandContext context )
	{
		this.context = context;
	}

	public Object selectFirst( String sql ) throws SQLException
	{
		Connection connection = this.context.getCurrentDatabase().getConnection();
		ResultSet result = connection.createStatement().executeQuery( sql );
		boolean record = result.next();

		int count = result.getMetaData().getColumnCount();
		if( count == 1 )
			return record ? result.getObject( 1 ) : null;

		Tuple tuple = new Tuple(); // TODO This is not language independent
		for( int i = 1; i <= count; i++ )
			tuple.append( record ? result.getObject( i ) : null );
		return tuple;
	}
}
