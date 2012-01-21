/*--
 * Copyright 2011 René M. de Bloois
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

package solidbase.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream wrapper that will ignore the call to close().
 *
 * @author René M. de Bloois
 */
public class NonClosingInputStream extends FilterInputStream
{
	/**
	 * Constructor.
	 *
	 * @param in The real input stream.
	 */
	public NonClosingInputStream( InputStream in )
	{
		super( in );
	}

	@Override
	public void close() throws IOException
	{
		// Ignore the close()
	}
}
