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

	public void init( Column[] columns )
	{
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
