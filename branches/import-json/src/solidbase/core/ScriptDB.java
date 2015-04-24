package solidbase.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import solidstack.script.objects.Tuple;

public class ScriptDB
{
	private CommandContext context;

	public ScriptDB( CommandContext context )
	{
		this.context = context;
	}

	public Object selectFirst( String sql ) throws SQLException
	{
		Connection connection = this.context.getCurrentDatabase().getConnection();
		ResultSet result = connection.createStatement().executeQuery( sql );
		boolean record = result.next();

		int count = result.getMetaData().getColumnCount();
		if( count == 1 )
			return record ? result.getObject( 1 ) : null;

		Tuple tuple = new Tuple(); // TODO This is not language independent
		for( int i = 1; i <= count; i++ )
			tuple.append( record ? result.getObject( i ) : null );
		return tuple;
	}
}
