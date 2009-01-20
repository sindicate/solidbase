package ronnie.dbpatcher.config;

import java.util.List;

public interface DatabasesConfiguration
{
	void init( Configuration configuration );
	List< Database > getDatabases();
}
