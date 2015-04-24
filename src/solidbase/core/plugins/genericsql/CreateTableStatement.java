package solidbase.core.plugins.genericsql;

import java.util.ArrayList;
import java.util.List;

public class CreateTableStatement extends Statement
{
	private String tableName;
	private List< ColumnSpec > columns = new ArrayList< ColumnSpec >();

	public void setTableName( String tableName )
	{
		this.tableName = tableName;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder( "CREATE TABLE " );
		result.append( this.tableName );
		result.append( "(" );
		boolean first = true;
		for( ColumnSpec column : this.columns )
		{
			if( first )
				first = false;
			else
				result.append( ", " );
			result.append( column );
		}
		result.append( ")" );
		return result.toString();
	}

	public void addColumn( ColumnSpec column )
	{
		this.columns.add( column );
	}

	public Object getTableName()
	{
		return this.tableName;
	}

	public List< ColumnSpec > getColumns()
	{
		return this.columns;
	}
}
