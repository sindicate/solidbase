package solidbase.util;

import java.io.InputStream;
import java.net.URL;

public interface Resource
{
	URL getURL();
	Resource createRelative( String path );
	InputStream getInputStream();
}
