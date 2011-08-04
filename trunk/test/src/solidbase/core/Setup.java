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


public class Setup
{
	static public UpgradeProcessor setupPatchProcessor( String fileName, String url )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", url, "sa", null, progress );
		UpgradeProcessor processor = new UpgradeProcessor( progress, database );
		UpgradeFile upgradeFile = Factory.openUpgradeFile( Factory.getResource( fileName ), progress );
		processor.setUpgradeFile( upgradeFile );
		processor.init();
		return processor;
	}

	static public UpgradeProcessor setupPatchProcessor( String fileName )
	{
		return setupPatchProcessor( fileName, "jdbc:hsqldb:mem:testdb" );
	}

	static public SQLProcessor setupSQLProcessor( String fileName )
	{
		TestProgressListener progress = new TestProgressListener();
		Database database = new Database( "default", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", null, progress );
		SQLProcessor processor = new SQLProcessor( progress, database );
		SQLFile sqlFile = Factory.openSQLFile( Factory.getResource( fileName ), progress );
		processor.setSQLSource( sqlFile.getSource() );
		return processor;
	}
}
