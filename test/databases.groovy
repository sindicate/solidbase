// use 'configuration' to refer to the Configuration object 

import ronnie.dbpatcher.config.Database;

// Get the location of the slot properties files
def location = configuration.getProperty( "databases.config.folder" );
assert location : "Property 'databases.config.folder' not set.";
location = new File( location );

def result = [];

// Loop through the client folders
location.eachDir
{
	folder ->

	// Want to know the number of slots in advance
	def total = 0;
	folder.eachFileMatch( ~/.*\.properties/ ) { total++; }
	
	folder.eachFileMatch( ~/.*\.properties/ )
	{
		file ->
		
		// Load the properties file
		def properties = new Properties();
		file.withInputStream { stream -> properties.load( stream ); }

		// Get and test the required properties
		def description = properties."conf.description";
		def type = properties."conf.database.type";
		def hostname = properties."conf.database.hostname";
		def port = properties."conf.database.port";
		def name = properties."conf.database.name";
		def username = properties."conf.database.username";

		assert type : "Property 'config.database.type' not found in: ${file.absolutePath}";
		assert hostname : "Property 'config.database.hostname' not found in: ${file.absolutePath}";
		assert port : "Property 'config.database.port' not found in: ${file.absolutePath}";
		assert name : "Property 'config.database.name' not found in: ${file.absolutePath}";
		assert username : "Property 'config.database.username' not found in: ${file.absolutePath}";

		// Determine driver, url & patchfile
		String driver;
		String url;
		String patchFile;
		if( type == "oracle" )
		{
			driver = "oracle.jdbc.OracleDriver";
			url = "jdbc:oracle:thin:@${hostname}:${port}:${name}";
			patchFile = "dbpatch-oracle-example.sql";
		}
		else if( type == "mysql" )
		{
			driver = "com.mysql.jdbc.Driver";
			url = "jdbc:mysql://${hostname}:${port}/${name}";
			patchFile = "dbpatch-mysql-example.sql";
		}
		else if( type == "hsqldb" )
		{
			driver = "org.hsqldb.jdbcDriver";
			url = "jdbc:hsqldb:mem:${name}";
			patchFile = "dbpatch-hsqldb-example.sql";
		}
		else
			throw new RuntimeException( "Unknown database type: ${type}" );

		// Define the database & application
		String databaseName = folder.name;
		if( total > 1 )
			databaseName += "-" + ( file.name - ".properties" );
		Database database = new Database( databaseName, description, driver, url );
		database.addApplication( "midoffice", null, username, patchFile );

		result.add( database );
	}
}

return result;
