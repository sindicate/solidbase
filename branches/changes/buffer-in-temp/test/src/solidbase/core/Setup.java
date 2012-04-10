/*--
 * Copyright 2010 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core;

import solidstack.io.Resources;


public class Setup
{
	static public final String defaultdb = "jdbc:hsqldb:mem:testdb";

	static public UpgradeProcessor setupUpgradeProcessor( String fileName, String driver, String url, String username )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", driver, url, username, null, progress );
		UpgradeProcessor processor = new UpgradeProcessor( progress );
		DatabaseContext databases = new DatabaseContext( database );
		processor.setDatabases( databases );
		UpgradeFile upgradeFile = Factory.openUpgradeFile( Resources.getResource( fileName ), progress );
		processor.setUpgradeFile( upgradeFile );
		processor.init();
		return processor;
	}

	static public UpgradeProcessor setupUpgradeProcessor( String fileName, String url )
	{
		return setupUpgradeProcessor( fileName, "org.hsqldb.jdbcDriver", url, "sa" );
	}

	static public UpgradeProcessor setupUpgradeProcessor( String fileName )
	{
		return setupUpgradeProcessor( fileName, defaultdb );
	}

	static public UpgradeProcessor setupDerbyUpgradeProcessor( String fileName )
	{
		return setupUpgradeProcessor( fileName, "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:test;create=true", "app" );
	}

	static public SQLProcessor setupSQLProcessor( String fileName )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", defaultdb, "sa", null, progress );
		SQLProcessor processor = new SQLProcessor( progress );
		SQLFile sqlFile = Factory.openSQLFile( Resources.getResource( fileName ), progress );
		DatabaseContext databases = new DatabaseContext( database );
		SQLContext context = new SQLContext( sqlFile.getSource() );
		context.setDatabases( databases );
		processor.setContext( context );
		return processor;
	}
}
