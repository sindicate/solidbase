package solidbase.core.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Generates filenames from values.
 *
 * @author René de Bloois
 * @since 2015
 */
class FileNameGenerator
{
	protected final Pattern pattern = Pattern.compile( "\\?(\\d+)" );
	protected String fileName;
	protected boolean parameterized;

	protected FileNameGenerator( String fileName )
	{
		this.fileName = fileName;
		this.parameterized = this.pattern.matcher( fileName ).find();
	}

	protected boolean isParameterized()
	{
		return this.parameterized;
	}

	protected String generateFileName( Object[] values )
	{
		Matcher matcher = this.pattern.matcher( this.fileName );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int index = Integer.parseInt( matcher.group( 1 ) );
			matcher.appendReplacement( result, values[ index - 1 ].toString() ); // TODO Does this work for every type?
		}
		matcher.appendTail( result );
		return result.toString();
	}
}
