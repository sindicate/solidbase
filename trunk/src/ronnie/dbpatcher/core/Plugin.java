package ronnie.dbpatcher.core;

import java.sql.SQLException;

abstract public class Plugin
{
	abstract public boolean execute( String sql ) throws SQLException;
}
