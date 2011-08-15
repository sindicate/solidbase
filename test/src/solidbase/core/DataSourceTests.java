/*--
 * Copyright 2011 René M. de Bloois
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

import java.sql.SQLException;
import java.util.Set;

import org.testng.annotations.Test;

import solidbase.util.DriverDataSource;
import solidbase.util.FileResource;

public class DataSourceTests
{
	@Test
	public void testWithDataSource() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		DriverDataSource dataSource = new DriverDataSource( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb2", "sa", null );
		Database database = new Database( "default", dataSource, progress );
		DatabaseContext databases = new DatabaseContext( database );
		UpgradeProcessor processor = new UpgradeProcessor( progress );
		processor.setDatabases( databases );
		UpgradeFile upgradeFile = Factory.openUpgradeFile( new FileResource( "testpatch1.sql" ), progress );
		processor.setUpgradeFile( upgradeFile );
		processor.init();

		Set< String > targets = processor.getTargets( false, null, false );
		assert targets.size() > 0;
		processor.upgrade( "1.0.2" );
		TestUtil.verifyVersion( processor, "1.0.2", null, 2, null );

		processor.end();
	}

	@Test
	public void testWithDataSourceAndUser() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );

		TestProgressListener progress = new TestProgressListener();
		DriverDataSource dataSource = new DriverDataSource( "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb2", "sa", null );
		Database database = new Database( "default", dataSource, "sa", null, progress );
		DatabaseContext databases = new DatabaseContext( database );
		UpgradeProcessor processor = new UpgradeProcessor( progress );
		processor.setDatabases( databases );
		UpgradeFile upgradeFile = Factory.openUpgradeFile( new FileResource( "testpatch1.sql" ), progress );
		processor.setUpgradeFile( upgradeFile );
		processor.init();

		Set< String > targets = processor.getTargets( false, null, false );
		assert targets.size() > 0;
		processor.upgrade( "1.0.2" );
		TestUtil.verifyVersion( processor, "1.0.2", null, 2, null );

		processor.end();
	}
}
