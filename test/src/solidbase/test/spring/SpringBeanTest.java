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

package solidbase.test.spring;

import java.sql.SQLException;

import org.springframework.beans.BeansException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;

public class SpringBeanTest
{
	@Test
	public void testSpringUpgrade() throws BeansException, SQLException
	{
		System.out.println( "1111" );
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		System.out.println( "2222" );
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );
		System.out.println( "3333" );
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext( "spring-upgrade.xml" );
		System.out.println( "4444" );
	}
}
