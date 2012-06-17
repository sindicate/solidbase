package solidbase.util;

public class FixedCounter extends RecordCounter
{
	private long interval;

	public FixedCounter( long interval )
	{
		this.interval = interval;
		init();
	}

	@Override
	protected long getNext( long count )
	{
		return count + this.interval;
	}
}
