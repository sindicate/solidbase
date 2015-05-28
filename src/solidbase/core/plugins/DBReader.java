package solidbase.core.plugins;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import solidbase.util.JDBCSupport;

public class DBReader
{
	private ResultSet result;
	private DataProcessor processor;
	private Counter counter;

	public DBReader( ResultSet result, DataProcessor processor, Counter counter )
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
		for( int i = 0; i < columns; i++ )
			types[ i ] = metaData.getColumnType( i + 1 );

		DataProcessor next = this.processor;

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
