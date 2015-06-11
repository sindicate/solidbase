/*--
 * Copyright 2015 Ren� M. de Bloois
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

import solidbase.util.JDBCSupport;

public class DBReader implements RecordSource
{
	private ResultSet result;
	private DataProcessor processor;
	private ExportLogger counter;
	private Column[] columns;
	private Object[] currentValues;


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

	public void setOutput( DataProcessor processor )
	{
		this.processor = processor;
	}

	public Column[] getColumns()
	{
		return this.columns;
	}

	public void init()
	{
		this.processor.init( this.columns );
	}

	@Override
	public Object[] getCurrentValues()
	{
		if( this.currentValues == null )
			throw new IllegalStateException( "There are no current values, called too early" );
		return this.currentValues;
	}

	public void process() throws SQLException
	{
		// TODO This metaData must go
		ResultSet result = this.result;
		ResultSetMetaData metaData = result.getMetaData();
		int columns = metaData.getColumnCount();

		int[] types = new int[ columns ];
		String[] names = new String[ columns ];
		for( int i = 0; i < columns; i++ )
		{
			int col = i + 1;
			types[ i ] = metaData.getColumnType( col );
			names[ i ] = metaData.getColumnName( col ).toUpperCase();
		}

		DataProcessor next = this.processor;

		while( result.next() )
		{
			Object[] values = this.currentValues = new Object[ columns ];
			for( int i = 0; i < columns; i++ )
				values[ i ] = JDBCSupport.getValue( result, types, i );

			next.process( values );

			if( this.counter != null )
				this.counter.count();
		}

		if( this.counter != null )
			this.counter.end();
	}
}
