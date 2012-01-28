package solidbase.util;

import java.util.Random;

public class StringUtils
{
	static public String randomize( Random random, String s )
	{
		if( s == null )
			return null;
		char[] chars = s.toCharArray();
		for( int j = 0; j < chars.length; j++ )
		{
			char c = chars[ j ];
			if( c >= 'A' && c <= 'Z' )
				chars[ j ] = (char)( random.nextInt( 26 ) + 'A' );
			else if( c >= 'a' && c <= 'z' )
				chars[ j ] = (char)( random.nextInt( 26 ) + 'a' );
			else if( c >= '0' && c <= '9' )
				chars[ j ] = (char)( random.nextInt( 10 ) + '0' );
		}
		return new String( chars );
	}
		}
