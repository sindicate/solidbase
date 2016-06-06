package solidbase.core.plugins;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


public class DefaultResultSetTransformer implements ResultSink, RecordSource
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
}
