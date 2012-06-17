package solidbase.util;

public class FixedCounter extends Counter
{
	private long interval;

	public FixedCounter( long interval )
	{
		this.interval = interval;
		setNext();
	}

	@Override
	protected long getNext( long count )
	{
		return count + this.interval;
	}
}
