package solidbase.core.plugins;

import java.sql.SQLException;

public interface DataProcessor
{
	void process( Object[] values ) throws SQLException;
	void init( String[] names );
}
