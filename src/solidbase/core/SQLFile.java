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

import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import solidstack.io.BOMDetectingLineReader;
import solidstack.io.Resource;


/**
 * This class manages an SQL file's contents. It detects the encoding and reads commands from it.
 *
 * @author René M. de Bloois
 * @since Apr 2010
 */
public class SQLFile
{
	static final Pattern ENCODING_PATTERN = Pattern.compile( "^--\\*[ \t]*ENCODING[ \t]+\"([^\"]*)\"[ \t]*$", Pattern.CASE_INSENSITIVE );

	/**
	 * The underlying file.
	 */
	protected BOMDetectingLineReader reader;


	/**
	 * Creates an new instance of an SQL file.
	 *
	 * @param resource The resource containing this SQL file.
	 */
	protected SQLFile( Resource resource )
	{
		try
		{
			this.reader = new BOMDetectingLineReader( resource, ENCODING_PATTERN );
		}
		catch( FileNotFoundException e )
		{
			throw new FatalException( e.toString() ); // TODO e or e.toString()?
		}
	}

	/**
	 * Close the SQL file. This will also close all underlying streams.
	 */
	protected void close()
	{
		if( this.reader != null )
		{
			this.reader.close();
			this.reader = null;
		}
	}

	/**
	 * Gets the encoding of the SQL file.
	 *
	 * @return The encoding of the SQL file.
	 */
	public String getEncoding()
	{
		return this.reader.getEncoding();
	}

	/**
	 * Returns a source for the SQL.
	 *
	 * @return A source for the SQL.
	 */
	public SQLSource getSource()
	{
		return new SQLSource( this.reader );
	}
}
