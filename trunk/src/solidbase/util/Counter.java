package solidbase.util;

abstract public class Counter
{
	private long total;
	private boolean logged = false;

	public boolean next()
	{
		this.total ++;
		return this.logged = logNow();
	}

	abstract protected boolean logNow();

	public long total()
	{
		return this.total;
	}

	public boolean needFinal()
	{
		return !this.logged;
	}
}
