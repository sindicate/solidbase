package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.SystemException;

public class CloseQueue
{
	private List< InputStream > files = new ArrayList< InputStream >();

	public void add( InputStream in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	public void closeAll()
	{
		try
		{
			for( Object file : this.files )
				( (InputStream)file ).close();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		this.files.clear();
	}
}
