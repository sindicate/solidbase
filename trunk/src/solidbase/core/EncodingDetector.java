package solidbase.core;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidstack.lang.SystemException;

public class EncodingDetector implements solidstack.io.EncodingDetector
{
	static final Pattern ENCODING_PATTERN = Pattern.compile( "--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\".*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL );

	/**
	 * Constant for the ISO-8859-1 character set.
	 */
	static final public String CHARSET_ISO = "ISO-8859-1";

	/**
	 * Constant for the UTF-8 character set.
	 */
	static final public String CHARSET_UTF8 = "UTF-8";

	static final public EncodingDetector INSTANCE = new EncodingDetector();


	private EncodingDetector()
	{
		// Singleton
	}

	public String detect( byte[] bytes, int len )
	{
		String result = CHARSET_ISO; // TODO Or null for platform dependent? Think not. Or UTF-8?

		String first = toAscii( bytes, len );
		Matcher matcher = ENCODING_PATTERN.matcher( first );
		if( matcher.matches() )
			result = matcher.group( 1 );

		return result;
	}

	static private String toAscii( byte[] chars, int len )
	{
		int j = 0;
		byte[] result = new byte[ len ];
		for( int i = 0; i < len; i++ )
		{
			byte ch = chars[ i ];
			if( ch > 0 && ch < 128 )
				result[ j++ ] = ch;
		}
		try
		{
			return new String( result, 0, j, CHARSET_ISO );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}
}
