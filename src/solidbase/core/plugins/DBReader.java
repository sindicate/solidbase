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

	public void process() throws SQLException
	{
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
		next.init( names );

		while( result.next() )
		{
			Object[] values = new Object[ columns ];
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
