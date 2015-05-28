package solidbase.core.plugins;

import java.sql.SQLException;

abstract public class DataProcessor
{
	abstract public void process( Object[] values ) throws SQLException;
}
