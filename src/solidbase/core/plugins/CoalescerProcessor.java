package solidbase.core.plugins;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.FatalException;

public class CoalescerProcessor implements DataProcessor
{
	private DataProcessor processor;

	private List<List<String>> names = new ArrayList<List<String>>();
	private List<List<Integer>> indexes = new ArrayList<List<Integer>>();


	public CoalescerProcessor( List<List<String>> names, DataProcessor processor )
	{
		this.names = names;
		this.processor = processor;
	}

	public void init( String[] names )
	{
		for( int i = 0; i < this.names.size(); i++ )
		{
			List<String> nams = this.names.get( i );
			List<Integer> indexes = this.indexes.get( i );
			for( int j = 0; j < nams.size(); j++ )
			{
				String name = nams.get( j );
				int found = -1;
				for( int k = 0; k < names.length; k++ )
					if( name.equals( names[ k ] ) )
					{
						found = k;
						break;
					}
				if( found < 0 )
					throw new FatalException( "Coalesce column " + name + " not in result set" ); // TODO Should be sourceexception
				indexes.set( j, found );
			}
		}
	}

	public void process( Object[] values ) throws SQLException
	{
		for( int i = 0; i < this.indexes.size(); i++ )
		{
			List< Integer > indexes = this.indexes.get( i );
			int firstIndex = indexes.get( 0 );
			if( values[ firstIndex ] == null )
			{
				Object found = null;
				for( int j = 1; j < indexes.size(); j++ )
				{
					found = values[ indexes.get( j ) ];
					if( found != null )
						break;
				}
				values[ firstIndex ] = found;
			}
		}

		this.processor.process( values );
	}
}
