/*--
 * Copyright 2016 René M. de Bloois
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
import java.sql.SQLException;
import java.sql.Types;


public class DefaultFromJDBCTransformer implements ResultSink, RecordSource
{
	private Column[] columns;
	private RecordSink sink;
	private Object[] currentRecord;


	@Override
	public void init( Column[] columns )
	{
		this.sink.init( columns );
		this.columns = columns;
	}

	@Override
	public Column[] getColumns()
	{
		return this.columns;
	}

	@Override
	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	@Override
	public Object[] getCurrentRecord()
	{
		return this.currentRecord;
	}

	@Override
	public void start()
	{
		this.sink.start();
	}

	@Override
	public void process( ResultSet result ) throws SQLException
	{
		int colCount = this.columns.length;
		Object[] values = this.currentRecord = new Object[ colCount ];

		for( int i = 0; i < colCount; i++ )
		{
			int col = i + 1;
			if( result.getObject( col ) != null )
			{
				int type = this.columns[ i ].getType();
				Object value;
				switch( type )
				{
					case Types.TINYINT:
					case Types.SMALLINT:
					case Types.INTEGER:
						value = result.getInt( col ); break;
					case Types.BIGINT:
						value = result.getLong( col ); break;
					case Types.NUMERIC:
					case Types.DECIMAL:
						value = result.getBigDecimal( col ); break;
					case Types.FLOAT:
						value = result.getFloat( col ); break;
					case Types.DOUBLE:
						value = result.getDouble( col ); break;
					case Types.BOOLEAN:
						value = result.getBoolean( col ); break;
					case Types.CHAR:
					case Types.VARCHAR:
						value = result.getString( col ); break;
					case Types.BINARY:
					case Types.VARBINARY:
						value = result.getBytes( col ); break;
					case Types.CLOB:
						value = result.getClob( col ); break;
					case Types.BLOB:
						value = result.getBlob( col ); break;
					case Types.DATE:
						value = result.getDate( col ); break;
					case Types.TIME:
						value = result.getTime( col ); break;
					case Types.TIMESTAMP:
						value = result.getTimestamp( col ); break;
					default:
						throw new UnsupportedOperationException( "type: " + type );
				}

				values[ i ] = value;
			}
		}

		this.sink.process( values );
	}

	@Override
	public void end() throws SQLException
	{
		this.sink.end();
	}
}
