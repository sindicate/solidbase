package solidbase.util;

import java.io.InputStream;
import java.net.URL;

public interface Resource
{
	boolean supportsURL();
	URL getURL();
	InputStream getInputStream();
	Resource createRelative( String path );
}
