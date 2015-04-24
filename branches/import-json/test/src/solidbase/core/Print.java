/*--
 * Copyright 2006 René M. de Bloois
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

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.testng.annotations.Test;


@SuppressWarnings( "javadoc" )
public class Print
{
	@Test
	public void testPrint() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		UpgradeProcessor processor = Setup.setupUpgradeProcessor( "testpatch-print2.sql" );
		processor.upgrade( "1" );

		// Copied from Export.java
		PreparedStatement statement = processor.prepareStatement( "INSERT INTO TEMP1 (ID, TEXT) VALUES (?, ?)" );

		statement.setInt( 1, 1 );
		statement.setString( 2, "Dit is blob nummer 1" );
		statement.execute();

		statement.setInt( 1, 2 );
		statement.setCharacterStream( 2, new StringReader( "Dit is blob nummer 2" ) );
		statement.execute();

		processor.closeStatement( statement, true );

		processor.upgrade( "2" );

		processor.end();
	}
}
