package solidbase.core.plugins.genericsql;

public class ColumnSpec
{
	private String name;
	private Type type;
	private boolean notNull;
	private boolean generated;
	private String primaryKeyName;
	private String uniqueKeyName;

	public void setName( String name )
	{
		this.name = name;
	}

	public void setType( Type type )
	{
		this.type = type;
	}

	public void setNotNull( boolean notNull )
	{
		this.notNull = notNull;
	}

	public void setGenerated( boolean value )
	{
		this.generated = value;
	}

	public void setPrimaryKey( String name )
	{
		this.primaryKeyName = name;
	}

	public void setUniqueKey( String name )
	{
		this.uniqueKeyName = name;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder( this.name );
		result.append( ' ' );
		result.append( this.type );
		if( this.notNull )
			result.append( " NOT NULL" );
		return result.toString();
	}

	public String getName()
	{
		return this.name;
	}

	public Type getType()
	{
		return this.type;
	}

	public boolean isNotNull()
	{
		return this.notNull;
	}

	public boolean isGenerated()
	{
		return this.generated;
	}

	public Object getPrimaryKeyName()
	{
		return this.primaryKeyName;
	}

	public Object getUniqueKeyName()
	{
		return this.uniqueKeyName;
	}
}
