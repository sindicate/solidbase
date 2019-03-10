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

package solidbase.test.scripting;

import java.sql.SQLException;

import org.testng.annotations.Test;

import solidbase.core.SQLProcessor;
import solidbase.core.Setup;
import solidbase.core.TestUtil;

public class Scripting
{
	@Test
	public void testParameter1() throws SQLException
	{
		TestUtil.dropHSQLDBSchema( Setup.defaultdb, "sa", null );
		SQLProcessor processor = Setup.setupSQLProcessor( "scripting/testsql-parameter1.sql" );
		// TODO Check the output

		try
		{
			processor.process();
		}
		finally // TODO Need this finally in the other tests too
		{
			processor.end();
		}
	}
}
