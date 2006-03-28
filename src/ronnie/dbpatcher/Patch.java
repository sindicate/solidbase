package ronnie.dbpatcher;

import com.cmg.pas.util.Assert;

public class Patch
{
	protected String source;
	protected String target;
	protected String description;
	protected boolean open;
	protected boolean branch;
	protected boolean returnBranch;
	protected long pos;
	
	public Patch( String source, String target, String description, boolean open, boolean branch, boolean returnBranch )
	{
		Assert.check( !( branch && returnBranch ) );
		
		this.source = source;
		this.target = target;
		this.description = description;
		this.open = open;
		this.branch = branch;
		this.returnBranch = returnBranch;
		this.pos = -1;
	}

	public boolean isBranch()
	{
		return this.branch;
	}

	public String getDescription()
	{
		return this.description;
	}

	public boolean isOpen()
	{
		return this.open;
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
}
