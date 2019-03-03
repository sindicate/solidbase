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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.FatalException;


public class CoalescerProcessor implements RecordSink, RecordSource
{
	private RecordSink sink;

	private List<List<String>> names;
	private Mapping[] mapping;
	private Column[] columns;


	public CoalescerProcessor( List<List<String>> names )
	{
		this.names = names;
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
	public void init( Column[] columns )
	{
		int cols = columns.length;
		boolean next[] = new boolean[ cols ];
		Mapping[] mapping = new Mapping[ cols ];

		for( int i = 0; i < this.names.size(); i++ )
		{
			Mapping current = null;
			List<String> nams = this.names.get( i );
			for( int j = 0; j < nams.size(); j++ )
			{
				String name = nams.get( j );
				int found = -1;
				for( int k = 0; k < cols; k++ )
					if( name.equals( columns[ k ].getName() ) )
					{
						found = k;
						break;
					}
				if( found < 0 )
					throw new FatalException( "Coalesce column " + name + " not in result set" ); // TODO Should be sourceexception
				if( j != 0 )
					next[ found ] = true;
				if( current == null )
					current = mapping[ found ] = new Mapping( found );
				else
					current = current.next = new Mapping( found );
			}
		}

		List<Column> newCols = new ArrayList<>();
		List<Mapping> newMapping = new ArrayList<>();

		int j = 0;
		for( int i = 0; i < cols; i++ )
			if( mapping[ i ] != null )
			{
				newCols.add( columns[ i ] );
				newMapping.add( mapping[ i ] );
			}
			else if( !next[ i ] )
			{
				newCols.add( columns[ i ] );
				newMapping.add( new Mapping( i ) );
			}

		this.columns = newCols.toArray( new Column[ newCols.size() ] );
		this.mapping = newMapping.toArray( new Mapping[ newCols.size() ] );

		this.sink.init( this.columns );
	}

	@Override
	public void start()
	{
		this.sink.start();
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		int count = this.mapping.length;
		Object[] newValues = new Object[ count ];

		for( int i = 0; i < count; i++ )
		{
			Mapping mapping = this.mapping[ i ];
			Object value = record[ mapping.index ];
			while( value == null && mapping.next != null )
			{
				mapping = mapping.next;
				value = record[ mapping.index ];
			}
			newValues[ i ] = value;
		}

		this.sink.process( newValues );
	}

	@Override
	public void end() throws SQLException
	{
		this.sink.end();
	}

	static class Mapping
	{
		int index;
		Mapping next;
		Mapping( int index )
		{
			this.index = index;
		}
	}

	@Override
	public Object[] getCurrentRecord()
	{
		throw new UnsupportedOperationException();
	}
}
