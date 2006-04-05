package ronnie.dbpatcher.core;

import java.sql.SQLException;

/**
 * 
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:13:28 PM
 */
abstract public class Plugin
{
	abstract public boolean execute( Command command ) throws SQLException;
}
