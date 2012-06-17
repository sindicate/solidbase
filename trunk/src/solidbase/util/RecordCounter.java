package solidbase.util;

abstract public class RecordCounter extends Counter
{
	private long next;

	public void init()
	{
		this.next = getNext( 0 );
	}

	@Override
	protected boolean logNow()
	{
		long total = total();
		if( total >= this.next )
		{
			this.next = getNext( total );
			return true;
		}
		return false;
	}

	abstract protected long getNext( long count );
}
