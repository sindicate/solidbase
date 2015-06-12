/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidstack.io.EncodingUtils;


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
		String result = CHARSET_ISO_8859_1; // FIXME Or null for platform dependent? Think not. Or UTF-8?

		String first = EncodingUtils.filter7bit( bytes );
		Matcher matcher = ENCODING_PATTERN.matcher( first );
		if( matcher.matches() )
			result = matcher.group( 1 );

		return result;
	}
}
