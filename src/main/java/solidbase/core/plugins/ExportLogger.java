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

import solidbase.core.ProgressListener;
import solidbase.util.LogCounter;


public class ExportLogger
{
	private LogCounter counter;
	private ProgressListener listener;


	public ExportLogger( LogCounter counter, ProgressListener progressListener )
	{
		this.counter = counter;
		this.listener = progressListener;
	}

	public void count()
	{
		if( this.counter.next() )
			this.listener.println( "Exported " + this.counter.total() + " records." );
	}

	public void end()
	{
		if( this.counter.needFinal() )
			this.listener.println( "Exported " + this.counter.total() + " records." );
	}
}
