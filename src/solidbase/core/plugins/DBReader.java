package solidbase.core.plugins;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import solidbase.util.JDBCSupport;

public class DBReader
{
	private ResultSet result;
	private DataProcessor processor;
	private ExportLogger counter;

	public DBReader( ResultSet result, DataProcessor processor, ExportLogger counter )
	{
		this.result = result;
		this.processor = processor;
		this.counter = counter;
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
