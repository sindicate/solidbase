package solidbase.core.plugins;

import java.sql.SQLException;

public class SelectProcessor implements DataProcessor, RecordSource
{
	private DataProcessor output;
	private Column[] inColumns;
	private Column[] outColumns;
	private int[] mapping;

	public SelectProcessor( Column[] columns )
	{
		this.inColumns = this.outColumns = columns;
		int[] mapping = this.mapping = new int[ columns.length ];
		for( int i = 0; i < mapping.length; i++ )
			mapping[ i ] = i;
	}

	public void deselect( int column )
	{
		int[] mapping = this.mapping;
		for( int i = 0; i < mapping.length; i++ )
			if( mapping[ i ] == column )
			{
				this.mapping = new int[ mapping.length - 1 ];
				System.arraycopy( mapping, 0, this.mapping, 0, i );
				System.arraycopy( mapping, i + 1, this.mapping, i, mapping.length - i - 1 );

				Column[] columns = this.outColumns;
				this.outColumns = new Column[ mapping.length - 1 ];
				System.arraycopy( columns, 0, this.outColumns, 0, i );
				System.arraycopy( columns, i + 1, this.outColumns, i, mapping.length - i - 1 );
				return;
			}
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

	public void init( String[] names )
	{
		this.output.init( names );
	}

	public void process( Object[] inValues ) throws SQLException
	{
		int[] mapping = this.mapping;
		Object[] outValues = new Object[ mapping.length ];
		for( int i = 0; i < mapping.length; i++ )
			outValues[ i ] = inValues[ mapping[ i ] ];
		this.output.process( outValues );
	}
}
