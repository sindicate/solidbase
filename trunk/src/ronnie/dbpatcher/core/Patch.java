package ronnie.dbpatcher.core;

import com.logicacmg.idt.commons.util.Assert;

/**
 * Represents a single patch in the patch file.
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
		Assert.isTrue( !( branch && returnBranch ) );

		this.source = source;
		this.target = target;
		this.branch = branch;
		this.returnBranch = returnBranch;
		this.open = open;
		this.init = init;
		this.pos = -1;
	}

	/**
	 * Is this patch a branch.
	 * 
	 * @return
	 */
	protected boolean isBranch()
	{
		return this.branch;
	}

	/**
	 * Is this patch a return from a branch.
	 * 
	 * @return
	 */
	protected boolean isReturnBranch()
	{
		return this.returnBranch;
	}

	/**
	 * Gets the source version.
	 * 
	 * @return
	 */
	protected String getSource()
	{
		return this.source;
	}

	/**
	 * Gets the target version.
	 * 
	 * @return
	 */
	protected String getTarget()
	{
		return this.target;
	}

	/**
	 * Sets the byte position in the file for this patch.
	 * 
	 * @param pos
	 */
	protected void setPos( long pos )
	{
		this.pos = pos;
	}

	/**
	 * Gets the byte position in the file for this patch.
	 * 
	 * @return
	 */
	protected long getPos()
	{
		return this.pos;
	}

	/**
	 * Is this patch open.
	 * 
	 * @return
	 */
	protected boolean isOpen()
	{
		return this.open;
	}

	/**
	 * Is this the init patch.
	 * 
	 * @return
	 */
	protected boolean isInit()
	{
		return this.init;
	}
}
