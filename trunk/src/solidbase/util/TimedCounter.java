package solidbase.util;

public class TimedCounter extends Counter
{
	private int milliseconds;
	private long next;

	public TimedCounter( int seconds )
	{
		this.milliseconds = seconds * 1000;
		this.next = System.currentTimeMillis() + this.milliseconds;
	}

	@Override
	protected boolean logNow()
	{
		long now = System.currentTimeMillis();
		if( now >= this.next )
		{
			this.next += this.milliseconds;
			if( now >= this.next )
				this.next = now + this.milliseconds;
			return true;
		}
		return false;
	}
}
