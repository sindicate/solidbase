/*--
 * Copyright 2015 René M. de Bloois
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

package solidbase.core.plugins;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.lang3.StringUtils;


public class DBReader implements ResultSource
{
	private ResultSet result;
	private ResultSink sink;
	private ExportLogger counter;
	private Column[] columns;


	public DBReader( ResultSet result, ExportLogger counter, boolean dateAsTimestamp  ) throws SQLException
	{
		this.result = result;
		this.counter = counter;

		ResultSetMetaData metaData = result.getMetaData();
		int count = metaData.getColumnCount();
		this.columns = new Column[ count ];

		for( int i = 0; i < count; i++ )
		{
			int col = i + 1;
			String name = metaData.getColumnName( col ).toUpperCase();
			int type = metaData.getColumnType( col );
			if( type == Types.DATE && dateAsTimestamp )
				type = Types.TIMESTAMP;
			String table = StringUtils.upperCase( StringUtils.defaultIfEmpty( metaData.getTableName( col ), null ) );
			String schema = StringUtils.upperCase( StringUtils.defaultIfEmpty( metaData.getSchemaName( col ), null ) );
			this.columns[ i ] = new Column( name, type, table, schema );
		}
	}

	@Override
	public void setSink( ResultSink sink )
	{
		this.sink = sink;
	}

	@Override
	public Column[] getColumns()
	{
		return this.columns;
	}

	public void init()
	{
		this.sink.init( this.columns );
	}

	public void process() throws SQLException
	{
		ResultSet result = this.result;
		ResultSink sink = this.sink;

		sink.start();
		while( result.next() )
		{
			sink.process( result );

			if( this.counter != null )
				this.counter.count();
		}
		sink.end();

		if( this.counter != null )
			this.counter.end();

		return;
	}
}
