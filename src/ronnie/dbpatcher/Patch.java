package ronnie.dbpatcher;

import com.cmg.pas.util.Assert;

public class Patch
{
	protected String source;
	protected String target;
	protected boolean branch;
	protected boolean returnBranch;
	protected boolean open;
	protected boolean init;
	protected long pos;
	
	public Patch( String source, String target, boolean branch, boolean returnBranch, boolean open, boolean init )
	{
		Assert.check( !( branch && returnBranch ) );
		
		this.source = source;
		this.target = target;
		this.branch = branch;
		this.returnBranch = returnBranch;
		this.open = open;
		this.init = init;
		this.pos = -1;
	}

	public boolean isBranch()
	{
		return this.branch;
	}

	public boolean isReturnBranch()
	{
		return this.returnBranch;
	}

	public String getSource()
	{
		return this.source;
	}

	public String getTarget()
	{
		return this.target;
	}

	public void setPos( long pos )
	{
		System.out.println( "Setting pos " + pos );
		this.pos = pos;
	}

	public long getPos()
	{
		return this.pos;
	}

	public boolean isOpen()
	{
		return this.open;
	}

	public boolean isInit()
	{
		return this.init;
	}
}
