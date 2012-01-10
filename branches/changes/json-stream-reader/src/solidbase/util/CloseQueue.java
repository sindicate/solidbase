package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.SystemException;

public class CloseQueue
{
	private List< Object > files = new ArrayList< Object >();

	public void add( InputStream in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	public void add( Reader in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

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
