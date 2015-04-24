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
 * A logging counter.
 *
 * @author René de Bloois
 */
abstract public class Counter
{
	private long total;
	private boolean logged = false;

	/**
	 * Count one.
	 *
	 * @return True if logging is needed.
	 */
	public boolean next()
	{
		this.total ++;
		return this.logged = logNow();
	}

	/**
	 * @return True if logging is needed.
	 */
	abstract protected boolean logNow();

	/**
	 * @return The total.
	 */
	public long total()
	{
		return this.total;
	}

	/**
	 * @return True if a final logging is needed.
	 */
	public boolean needFinal()
	{
		return !this.logged;
	}
}
