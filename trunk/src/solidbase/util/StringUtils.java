/*--
 * Copyright 2012 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.util;

import java.util.Random;


/**
 * String utilities.
 *
 * @author René de Bloois
 */
public class StringUtils
{
	/**
	 * Randomizes the given string.
	 *
	 * @param random The random class containing the seed.
	 * @param s The string to randomize.
	 * @return A randomized string.
	 */
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
