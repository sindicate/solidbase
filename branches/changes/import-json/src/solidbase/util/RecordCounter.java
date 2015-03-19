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


/**
 * A logging counter that logs depending on the count.
 *
 * @author René de Bloois
 */
abstract public class RecordCounter extends Counter
{
	private long next;

	/**
	 * Initialize.
	 */
	public void init()
	{
		this.next = getNext( 0 );
	}

	@Override
	protected boolean logNow()
	{
		long total = total();
		if( total >= this.next )
		{
			this.next = getNext( total );
			return true;
		}
		return false;
	}

	/**
	 * @param count The current count.
	 * @return The next threshold for logging.
	 */
	abstract protected long getNext( long count );
}
