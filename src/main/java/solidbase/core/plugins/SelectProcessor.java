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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;


public class SelectProcessor implements RecordSink, RecordSource
{
	private RecordSink sink;
	private Column[] outColumns;
	private int[] mapping;
	private Set<String> deselect = new HashSet<>();


	public void deselect( String name )
	{
		this.deselect.add( name );
	}

	public boolean hasDeselected()
	{
		return !this.deselect.isEmpty();
	}

	@Override
	public void init( Column[] columns )
	{
		// TODO Error when deselect contains an incorrect columnname

		List<Integer> temp = new ArrayList<>();
		for( int i = 0; i < columns.length; i++ )
			if( !this.deselect.contains( columns[ i ].getName() ) )
				temp.add( i );

		// TODO Google Guava has a Ints.toArray() ?
		int[] mapping = this.mapping = ArrayUtils.toPrimitive( temp.toArray( new Integer[ temp.size() ] ) );

		this.outColumns = new Column[ mapping.length ];
		for( int i = 0; i < mapping.length; i++ )
			this.outColumns[ i ] = columns[ mapping[ i ] ];

		this.sink.init( this.outColumns );
	}

	@Override
	public Column[] getColumns()
	{
		return this.outColumns;
	}

	@Override
	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	@Override
	public void start()
	{
		this.sink.start();
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		int[] mapping = this.mapping;
		Object[] mappedRecord = new Object[ mapping.length ];
		for( int i = 0; i < mapping.length; i++ )
			mappedRecord[ i ] = record[ mapping[ i ] ];
		this.sink.process( mappedRecord );
	}

	@Override
	public void end() throws SQLException
	{
		this.sink.end();
	}

	@Override
	public Object[] getCurrentRecord()
	{
		throw new UnsupportedOperationException();
	}
}
