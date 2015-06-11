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

import java.io.OutputStream;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileSpec
{
	static private final Pattern pattern = Pattern.compile( "\\?(\\d+)" );

	boolean binary;
	int threshold;
	String fileName;
	private boolean parameterized;

	OutputStream out;
	Writer writer;
	int index;

	private RecordSource source;


	protected FileSpec( boolean binary, String fileName, int threshold )
	{
		this.binary = binary;
		this.fileName = fileName;
		this.threshold = threshold;

		this.parameterized = FileSpec.pattern.matcher( fileName ).find();
	}

	protected FileSpec( boolean binary, String fileName, int threshold, RecordSource source )
	{
		this( binary, fileName, threshold );
		this.source = source;
	}

	protected void setSource( RecordSource source )
	{
		this.source = source;
	}

	protected boolean isParameterized()
	{
		return this.parameterized;
	}

	protected String generateFileName()
	{
		Object[] values = this.source.getCurrentValues();

		Matcher matcher = FileSpec.pattern.matcher( this.fileName );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int index = Integer.parseInt( matcher.group( 1 ) );
			matcher.appendReplacement( result, toString( values[ index - 1 ] ) );
		}
		matcher.appendTail( result );
		return result.toString();
	}

	// TODO Does this work for every type?
	static public String toString( Object value )
	{
		if( value == null )
			return "null";
		return value.toString();
	}
}