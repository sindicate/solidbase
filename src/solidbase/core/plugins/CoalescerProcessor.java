package solidbase.core.plugins;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.FatalException;

public class CoalescerProcessor implements DataProcessor, RecordSource
{
	private DataProcessor output;

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
	public void setOutput( DataProcessor output )
	{
		this.output = output;
	}

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

		List<Column> newCols = new ArrayList<Column>();
		List<Mapping> newMapping = new ArrayList<Mapping>();

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

		this.output.init( this.columns );
	}

	public void process( Object[] values ) throws SQLException
	{
		int count = this.mapping.length;
		Object[] newValues = new Object[ count ];

		for( int i = 0; i < count; i++ )
		{
			Mapping mapping = this.mapping[ i ];
			Object value = values[ mapping.index ];
			while( value == null && mapping.next != null )
			{
				mapping = mapping.next;
				value = values[ mapping.index ];
			}
			newValues[ i ] = value;
		}

		this.output.process( newValues );
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
	public Object[] getCurrentValues()
	{
		throw new UnsupportedOperationException();
	}
}
