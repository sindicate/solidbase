package solidbase.core.plugins;

import java.sql.SQLException;

import solidbase.core.plugins.DumpJSON.Coalescer;

public class CoalescerProcessor extends DataProcessor
{
	private Coalescer coalescer;
	private DataProcessor processor;

	public CoalescerProcessor( Coalescer coalescer, DataProcessor processor )
	{
		this.coalescer = coalescer;
		this.processor = processor;
	}

	@Override
	public void process( Object[] values ) throws SQLException
	{
		this.coalescer.coalesce( values );
		this.processor.process( values );
	}
}
