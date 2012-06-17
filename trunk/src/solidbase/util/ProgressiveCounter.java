package solidbase.util;

public class ProgressiveCounter extends RecordCounter
{
	private int percent;
	private long min;
	private long max;

	public ProgressiveCounter( int percent, long min, long max )
	{
		this.percent = percent;
		this.min = min;
		this.max = max;
		init();
	}

	@Override
	protected long getNext( long count )
	{
		long delta = count * this.percent / 100;
		if( delta < this.min )
			delta = this.min;
		if( delta > this.max )
			delta = this.max;
		if( delta > 0 )
		{
			long pow = (long)Math.pow( 10, Math.floor( Math.log10( delta ) ) );
			long next = count + delta;
			return next / pow * pow;
		}
		return count;
	}
}
