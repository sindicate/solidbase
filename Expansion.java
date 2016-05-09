package solidbase.core.script;

import solidbase.core.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.io.SourceReaders;

public class Expansion
{
	/**
	 * Parses a string as a processed string.
	 *
	 * @param s The processed string to parse.
	 * @param location The start location of the processed string.
	 * @return An expression.
	 */
	static public StringExpression parseString( String s, SourceLocation location )
	{
		SourceReader in = SourceReaders.forString( s, location );

		ProcessedStringTokenizer t = new ProcessedStringTokenizer( in );
		ScriptScanner parser = new ScriptScanner( in );

		StringExpression result = new StringExpression();

		Fragment fragment = t.getFragment(); // TODO Fragment object not needed anymore
		if( fragment.length() != 0 )
			result.appendFragment( fragment );
		while( t.foundExpression() )
		{
			String expression = parser.scan();
			int ch = in.read();
			if( ch != '}' )
				throw new SourceException( "Unexpected character " + ch + ", missing }", in.getLocation() );
			result.append( expression );
			fragment = t.getFragment();
			if( fragment.length() != 0 )
				result.appendFragment( fragment );
		}

		return result;
	}
}
