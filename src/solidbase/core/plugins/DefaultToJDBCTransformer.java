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


public class DefaultToJDBCTransformer implements RecordSink, RecordSource
{
	private RecordSink sink;
	private Column[] columns;


	public DefaultToJDBCTransformer( RecordSink sink )
	{
		this.sink = sink;
	}

	@Override
	public void init( Column[] columns )
	{
		this.columns = columns;
		this.sink.init( columns );
	}

	@Override
	public void start()
	{
		this.sink.start();
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		// Convert the strings to date, time and timestamps
		if( this.columns != null )
			for( int i = 0; i < record.length; i++ )
			{
				Object value = record[ i ];
				if( value != null && value instanceof String )
					try
					{
						// TODO Time zones, is there a default way of putting times and dates in a text file? For example whats in a HTTP header?
						// TODO Use internet formats (XML)
						switch( this.columns[ i ].getType() )
						{
							case Types.DATE:
								record[ i ] = java.sql.Date.valueOf( (String)value );
								break;
							case Types.TIMESTAMP:
								record[ i ] = java.sql.Timestamp.valueOf( (String)value );
								break;
							case Types.TIME:
								record[ i ] = java.sql.Time.valueOf( (String)value );
								break;
						}
					}
					catch( IllegalArgumentException e )
					{
						// TODO Add test? C:\_WORK\SAO-20150612\build.xml:32: The following error occurred while executing this line:
						// C:\_WORK\SAO-20150612\build.xml:13: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff], at line 17 of file C:/_WORK/SAO-20150612/SYSTEEM/sca.JSON.GZ
						throw new ProcessException( e ).addProcess( "trying to convert " + value + " to " + this.columns[ i ].getType() );
					}
			}

		this.sink.process( record );
	}

	@Override
	public void end() throws SQLException
	{
		this.sink.end();
	}

	@Override
	public Column[] getColumns()
	{
		return this.columns;
	}

	@Override
	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	@Override
	public Object[] getCurrentRecord()
	{
		throw new UnsupportedOperationException();
	}
}
