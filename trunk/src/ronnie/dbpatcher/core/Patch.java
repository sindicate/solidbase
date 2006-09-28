package ronnie.dbpatcher.core;

import com.lcmg.rbloois.util.Assert;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:15 PM
 */
public class Patch
{
	protected String source;
	protected String target;
	protected boolean branch;
	protected boolean returnBranch;
	protected boolean open;
	protected boolean init;
	protected long pos;
	
	protected Patch( String source, String target, boolean branch, boolean returnBranch, boolean open, boolean init )
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

	protected boolean isBranch()
	{
		return this.branch;
	}

	protected boolean isReturnBranch()
	{
		return this.returnBranch;
	}

	protected String getSource()
	{
		return this.source;
	}

	protected String getTarget()
	{
		return this.target;
	}

	protected void setPos( long pos )
	{
		this.pos = pos;
	}

	protected long getPos()
	{
		return this.pos;
	}

	protected boolean isOpen()
	{
		return this.open;
	}

	protected boolean isInit()
	{
		return this.init;
	}
}
