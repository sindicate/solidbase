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

package solidstack.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An input stream resource.
 *
 * @author René M. de Bloois
 */
public class SystemInOutResource extends ResourceAdapter
{
	/**
	 * The {@link System#in}. Registered at the time of creation of this resource.
	 */
	protected InputStream in = System.in;

	/**
	 * The {@link System#out}. Registered at the time of creation of this resource.
	 */
	protected OutputStream out = System.out;


	@Override
	public InputStream getInputStream()
	{
		if( this.in == null )
			throw new IllegalStateException( "inputStream has been accessed earlier" );
		InputStream result = new NonClosingInputStream( this.in );
		this.in = null;
		return result;
	}

	@Override
	public OutputStream getOutputStream()
	{
		if( this.out == null )
			throw new IllegalStateException( "outputStream has been accessed earlier" );
		OutputStream result = new NonClosingOutputStream( this.out );
		this.out = null;
		return result;
	}
}
