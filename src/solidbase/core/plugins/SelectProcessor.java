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

public class SelectProcessor implements DataProcessor, RecordSource
{
	private DataProcessor output;
	private Column[] outColumns;
	private int[] mapping;
	private Set<String> deselect = new HashSet<String>();


	public void deselect( String name )
	{
		this.deselect.add( name );
	}

	public boolean hasDeselected()
	{
		return !this.deselect.isEmpty();
	}

	public void init( Column[] columns )
	{
		// TODO Error when deselect contains an incorrect columnname

		List<Integer> temp = new ArrayList<Integer>();
		for( int i = 0; i < columns.length; i++ )
			if( !this.deselect.contains( columns[ i ].getName() ) )
				temp.add( i );

		// TODO Google Guava has a Ints.toArray() ?
		int[] mapping = this.mapping = ArrayUtils.toPrimitive( temp.toArray( new Integer[ temp.size() ] ) );

		this.outColumns = new Column[ mapping.length ];
		for( int i = 0; i < mapping.length; i++ )
			this.outColumns[ i ] = columns[ mapping[ i ] ];

		this.output.init( this.outColumns );
	}

	@Override
	public Column[] getColumns()
	{
		return this.outColumns;
	}

	@Override
	public void setOutput( DataProcessor output )
	{
		this.output = output;
	}

	public void process( Object[] inValues ) throws SQLException
	{
		int[] mapping = this.mapping;
		Object[] outValues = new Object[ mapping.length ];
		for( int i = 0; i < mapping.length; i++ )
			outValues[ i ] = inValues[ mapping[ i ] ];
		this.output.process( outValues );
	}

	@Override
	public Object[] getCurrentValues()
	{
		throw new UnsupportedOperationException();
	}
}
