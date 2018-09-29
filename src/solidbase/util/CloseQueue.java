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

package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import solidstack.io.FatalIOException;


/**
 * A queue of input streams and readers that need to be closed.
 *
 * @author René de Bloois
 */
public class CloseQueue
{
	private List< Object > files = new ArrayList<>();

	/**
	 * Add an input stream.
	 *
	 * @param in An input stream.
	 */
	public void add( InputStream in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	/**
	 * Add a reader.
	 *
	 * @param in A reader.
	 */
	public void add( Reader in )
	{
		Assert.notNull( in );
		this.files.add( in );
	}

	/**
	 * Close all registered input streams and readers.
	 */
	public void closeAll()
	{
		try
		{
			for( Object file : this.files )
			{
				if( file instanceof InputStream )
					( (InputStream)file ).close();
				else
					( (Reader)file ).close();
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
		this.files.clear();
	}
}
