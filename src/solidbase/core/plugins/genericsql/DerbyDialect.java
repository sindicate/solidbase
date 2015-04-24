package solidbase.core.plugins.genericsql;

public class DerbyDialect
{
	public String toSQLString( Statement statement )
	{
		if( statement instanceof CreateTableStatement )
			return generateCreateTable( (CreateTableStatement)statement );
		throw new UnsupportedOperationException( "Statement of type <" + statement.getClass().getName() + "> not supported" );
	}

	private String generateCreateTable( CreateTableStatement statement )
	{
		StringBuilder result = new StringBuilder( "CREATE TABLE " );
		result.append( statement.getTableName() );
		result.append( "\n(\n" );
		boolean first = true;
		for( ColumnSpec column : statement.getColumns() )
		{
			if( first )
				first = false;
			else
				result.append( ",\n" );
			result.append( '\t' );
			result.append( column.getName() );
			result.append( ' ' );
			result.append( column.getType() );
			if( column.isNotNull() )
				result.append( " NOT NULL" );
			if( column.isGenerated() )
				result.append( " GENERATED ALWAYS AS IDENTITY" );
			if( column.getPrimaryKeyName() != null )
				result.append( " CONSTRAINT " + column.getPrimaryKeyName() + " PRIMARY KEY" );
		}
		result.append( "\n)\n" );
		return result.toString();
	}
}
