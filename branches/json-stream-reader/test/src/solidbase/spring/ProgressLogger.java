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

package solidbase.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solidbase.core.ProgressListener;

/**
 * An implementation of {@link ProgressListener} that logs all messages to the 'solidbase' logger.
 *
 * @author René M. de Bloois
 */
public class ProgressLogger extends ProgressListener
{
	static private final Logger logger = LoggerFactory.getLogger( "solidbase" );

	@Override
	public void cr()
	{
		//
	}

	@Override
	public void println( String message )
	{
		logger.info( message );
	}
}
