package solidbase.test.spring;

import java.sql.SQLException;

import org.springframework.beans.BeansException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;

public class SpringBeanTest
{
	@Test(groups="new")
	public void testSpringUpgrade() throws BeansException, SQLException
	{
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb", "sa", null );
		TestUtil.dropHSQLDBSchema( "jdbc:hsqldb:mem:testdb2", "sa", null );
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext( "spring-upgrade.xml" );
	}
}
