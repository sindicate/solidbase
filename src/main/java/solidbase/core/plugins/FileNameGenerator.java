/*--
 * Copyright 2015 René M. de Bloois
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
