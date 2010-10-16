package solidbase.util;

import java.util.ArrayList;
import java.util.List;

import solidbase.util.Tokenizer.Token;

public class CSVReader
{
	protected Tokenizer tokenizer;
	protected String separator;

	public CSVReader( Tokenizer tokenizer, char separator )
	{
		this.tokenizer = tokenizer;
		this.separator = String.valueOf( separator );
	}

	public String[] getLine()
	{
		Tokenizer tokenizer = this.tokenizer;

		List< String > values = new ArrayList< String >();
		Token token = tokenizer.get();
//		System.out.println( "Token: " + token );
		while( !token.isEndOfInput() && !token.isNewline() )
		{
			if( token.equals( this.separator ) )
			{
				values.add( "" );
				token = tokenizer.get();
//				System.out.println( "Token: " + token );
			}
			else
			{
				values.add( token.getValue() );
				token = tokenizer.get( this.separator, "\n", null );
//				System.out.println( "Token: " + token );
				if( token.equals( this.separator ) )
				{
					token = tokenizer.get();
//					System.out.println( "Token: " + token );
				}
			}
		}
		if( values.isEmpty() )
			return null;
		return values.toArray( new String[ values.size() ] );
	}

	public int getLineNumber()
	{
		return this.tokenizer.getLineNumber();
	}
}
