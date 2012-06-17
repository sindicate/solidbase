package solidbase.util;

abstract public class Counter
{
	private long next;
	private long total;
	private boolean finalized = false;

	public boolean next()
	{
		this.total ++;
		return this.finalized = logNow();
	}

	protected boolean logNow()
	{
		if( this.total >= this.next )
		{
			setNext();
			return true;
		}
		return false;
	}

	public long total()
	{
		return this.total;
	}

	protected void setNext()
	{
		this.next = getNext( this.total );
	}

	protected long getNext( long count )
	{
		throw new UnsupportedOperationException();
	}

	public boolean needFinal()
	{
		return !this.finalized;
	}
}
