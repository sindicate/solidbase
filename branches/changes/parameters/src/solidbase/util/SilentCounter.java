package solidbase.util;

public class SilentCounter extends RecordCounter
{
	public SilentCounter()
	{
		init();
	}

	@Override
	protected long getNext( long count )
	{
		return Long.MAX_VALUE;
	}
}
