package solidbase.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Detects the encoding of upgrade and SQL files.
 *
 * @author René de Bloois
 */
public class EncodingDetector implements solidstack.io.EncodingDetector
{
	static final Pattern ENCODING_PATTERN = Pattern.compile( "--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\".*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL );

	/**
	 * The singleton instance.
	 */
	static final public EncodingDetector INSTANCE = new EncodingDetector();


	private EncodingDetector()
	{
		// Singleton
	}

	/**
	 * Detects the encoding of an upgrade or SQL file from the first couple of bytes of the file. If not encoding is detected the default of ISO-8859-1 is returned.
	 */
	public String detect( byte[] bytes )
	{
		String result = CHARSET_ISO_8859_1; // TODO Or null for platform dependent? Think not. Or UTF-8?

		String first = solidstack.template.EncodingDetector.toAscii( bytes );
		Matcher matcher = ENCODING_PATTERN.matcher( first );
		if( matcher.matches() )
			result = matcher.group( 1 );

		return result;
	}
}
