package solidbase.core.plugins;

import java.sql.SQLException;

public class RecordNumberPrepender implements RecordSource, RecordSink
{
	private RecordSink sink;
	private int recordNumber;

	@Override
	public Column[] getColumns()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	@Override
	public Object[] getCurrentRecord()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void init( Column[] columns )
	{
		this.sink.init( columns );
	}

	@Override
	public void start()
	{
		this.sink.start();
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		int len = record.length;
		Object[] result = new Object[ len + 1 ];
		result[ 0 ] = ++this.recordNumber;
		System.arraycopy( record, 0, result, 1, len );

		this.sink.process( result );
	}

	@Override
	public void end() throws SQLException
	{
		this.sink.end();
	}
}
