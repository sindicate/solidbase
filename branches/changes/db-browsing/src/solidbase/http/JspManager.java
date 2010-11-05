package solidbase.http;

import java.util.HashMap;
import java.util.Map;

import solidbase.core.SystemException;

public class JspManager
{
	static protected Map< String, Class< Servlet > > cache = new HashMap< String, Class< Servlet > >();

	static public void call( String name, RequestContext context )
	{
		Class< Servlet > jsp = cache.get( name );
		if( jsp == null )
		{
			try
			{
				jsp = ( Class< Servlet > )JspManager.class.getClassLoader().loadClass( name );
			}
			catch( ClassNotFoundException e )
			{
				throw new SystemException( e );
			}
			cache.put( name, jsp );
		}
		Servlet servlet;
		try
		{
			servlet = jsp.newInstance();
		}
		catch( InstantiationException e )
		{
			throw new SystemException( e );
		}
		catch( IllegalAccessException e )
		{
			throw new SystemException( e );
		}
		servlet.call( context );
	}
}
