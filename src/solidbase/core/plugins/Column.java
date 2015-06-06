package solidbase.core.plugins;

import solidbase.util.JDBCSupport;

public class Column
{
	private String name;
	private int type;
	private String table;
	private String schema;
	private String typeName;

	public Column( String name, int type, String table, String schema )
	{
		this.name = name;
		this.type = type;
		this.table = table;
		this.schema = schema;
		this.typeName = JDBCSupport.toTypeName( type );
	}

	public int getType()
	{
		return this.type;
	}

	public void setType( int type )
	{
		this.type = type;
	}

	public String getName()
	{
		return this.name;
	}

	public String getSchema()
	{
		return this.schema;
	}

	public String getTable()
	{
		return this.table;
	}

	public String getTypeName()
	{
		return this.typeName;
	}
}
