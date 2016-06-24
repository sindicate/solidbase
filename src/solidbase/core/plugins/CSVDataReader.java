/*--
 * Copyright 2016 René M. de Bloois
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

import java.sql.SQLException;
import java.sql.Types;

import solidbase.core.ProcessException;
import solidbase.util.CSVReader;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.lang.ThreadInterrupted;


public class CSVDataReader // TODO implements RecordSource
{
	private CSVReader reader;
	private boolean prependLineNumber;
	private ImportLogger counter;

	private RecordSink sink;
	private boolean done;


	public CSVDataReader( SourceReader sourceReader, boolean skipHeader, char separator, boolean escape, boolean ignoreWhiteSpace, boolean prependLineNumber, ImportLogger counter )
	{
		// Initialize csv reader & read first line (and skip header if needed)
		this.reader = new CSVReader( sourceReader, separator, escape, ignoreWhiteSpace );
		this.prependLineNumber = prependLineNumber;
		this.counter = counter;

		// TODO Move to start of process()
		if( skipHeader )
		{
			String[] line = this.reader.getLine();
			if( line == null )
				this.done = true;
		}
	}

	public void setOutput( RecordSink output )
	{
		this.sink = output;
	}

	public void process() throws SQLException
	{
		boolean initDone = false;

		SourceLocation loc = this.reader.getLocation();
		String[] line = this.reader.getLine();
		while( line != null )
		{
			if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
				throw new ThreadInterrupted();

			for( int i = 0; i < line.length; i++ )
				if( line[ i ].length() == 0 )
					line[ i ] = null;

			if( this.prependLineNumber )
			{
				String[] temp = line;
				line = new String[ temp.length + 1 ];
				line[ 0 ] = Integer.toString( loc.getLineNumber() );
				System.arraycopy( temp, 0, line, 1, temp.length );
			}

			if( !initDone )
			{
				Column[] columns = new Column[ line.length ];
				for( int i = 0; i < columns.length; i++ )
					columns[ i ] = new Column( null, Types.VARCHAR, null, null );
				this.sink.init( columns );
				initDone = true;
			}

			this.sink.start();
			try
			{
				this.sink.process( line );
			}
			catch( ProcessException e )
			{
				throw new ProcessException( e ).addLocation( loc );
			}
			this.sink.end();

			if( this.counter != null )
				this.counter.count();

			loc = this.reader.getLocation();
			line = this.reader.getLine();
		}

		if( this.counter != null )
			this.counter.end();
	}
}
