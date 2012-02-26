/*--
 * Copyright 2011 Ren� M. de Bloois
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

import java.io.InputStream;

/**
 * An input stream resource.
 *
 * @author Ren� M. de Bloois
 */
public class InputStreamResource extends ResourceAdapter
{
	/**
	 * The input stream.
	 */
	protected InputStream inputStream;

	/**
	 * Constructor.
	 *
	 * @param inputStream The input stream.
	 */
	public InputStreamResource( InputStream inputStream )
	{
		if( inputStream == null )
			throw new IllegalArgumentException( "inputStream should not be null" );
		this.inputStream = inputStream;
	}

	@Override
	public InputStream getInputStream()
	{
		if( this.inputStream == null )
			throw new IllegalStateException( "inputStream has been accessed earlier" );
		InputStream result = this.inputStream;
		this.inputStream = null;
		return result;
	}
}
