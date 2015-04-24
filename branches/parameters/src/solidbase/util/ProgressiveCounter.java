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
 * A counter that logs progressively.
 *
 * @author René de Bloois
 */
public class ProgressiveCounter extends RecordCounter
{
	private int percent;
	private long min;
	private long max;

	/**
	 * Constructor.
	 *
	 * @param percent Percentage of current count which determines the next interval.
	 * @param min Minimum interval.
	 * @param max Maximum interval.
	 */
	public ProgressiveCounter( int percent, long min, long max )
	{
		this.percent = percent;
		this.min = min;
		this.max = max;
		init();
	}

	@Override
	protected long getNext( long count )
	{
		long delta = count * this.percent / 100;
		if( delta < this.min )
			delta = this.min;
		if( delta > this.max )
			delta = this.max;
		if( delta > 0 )
		{
			long pow = (long)Math.pow( 10, Math.floor( Math.log10( delta ) ) );
			long next = count + delta;
			return next / pow * pow;
		}
		return count;
	}
}
