package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.SystemException;


/**
 * A queue of input streams and readers that need to be closed.
 *
 * @author René de Bloois
 */
public class CloseQueue
{
	private List< Object > files = new ArrayList< Object >();

	/**
	 * Add an input stream.
	 *
	 * @param in An input stream.
	 */
	public void add( InputStream in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	/**
	 * Add a reader.
	 *
	 * @param in A reader.
	 */
	public void add( Reader in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	/**
	 * Close all registered input streams and readers.
	 */
	public void closeAll()
	{
		try
		{
			for( Object file : this.files )
			{
				if( file instanceof InputStream )
					( (InputStream)file ).close();
				else
					( (Reader)file ).close();
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		this.files.clear();
	}
}
