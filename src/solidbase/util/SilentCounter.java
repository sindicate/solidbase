package solidbase.util;

public class SilentCounter extends Counter
{
	public SilentCounter()
	{
		setNext();
	}

	@Override
	protected long getNext( long count )
	{
		return Long.MAX_VALUE;
	}
}
