package solidbase.test.maven;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;
import solidbase.maven.Parameter;
import solidbase.maven.SQLMojo;
import solidbase.maven.UpgradeMojo;

@SuppressWarnings( "javadoc" )
public class MavenTest
{
	@Test
	static public void testSql()
	{
		String output = TestUtil.capture( new Runnable()
		{
			public void run()
			{
				SQLMojo sql = new SQLMojo();

				sql.project = new MavenProject();
				sql.project.setFile( new File( "pom.xml" ).getAbsoluteFile() );

				sql.driver = "org.hsqldb.jdbcDriver";
				sql.url = "jdbc:hsqldb:mem:MavenTest1";
				sql.username = "sa";
				sql.sqlfile = "testsql-parameter2.sql";

				sql.parameters = new Parameter[] { new Parameter( "par1", "val1" ), new Parameter( "par2", null ) };

				try
				{
					sql.execute();
				}
				catch( MojoFailureException e )
				{
					throw new RuntimeException( e );
				}
			}
		} );

		output = TestUtil.generalizeOutput( output );
		Assert.assertEquals( output, "[info] SolidBase v1.5.x (http://solidbase.org)\n" +
				"[info] \n" +
				"[info] Opening file 'X:/.../testsql-parameter2.sql'\n" +
				"[info]     Encoding is 'ISO-8859-1'\n" +
				"[info] Connecting to database...\n" +
				"[info] val1\n" +
				"[info] Execution complete.\n" +
				"[info] \n"
				);
	}

	@Test
	static public void testUpgrade()
	{
		String output = TestUtil.capture( new Runnable()
		{
			public void run()
			{
				UpgradeMojo upgrade = new UpgradeMojo();

				upgrade.project = new MavenProject();
				upgrade.project.setFile( new File( "pom.xml" ).getAbsoluteFile() );

				upgrade.driver = "org.hsqldb.jdbcDriver";
				upgrade.url = "jdbc:hsqldb:mem:MavenTest2";
				upgrade.username = "sa";
				upgrade.upgradefile = "testpatch-parameter2.sql";

				upgrade.parameters = new Parameter[] { new Parameter( "par1", "val1" ), new Parameter( "par2", null ) };

				try
				{
					upgrade.execute();
				}
				catch( MojoFailureException e )
				{
					throw new RuntimeException( e );
				}
			}
		} );

		output = TestUtil.generalizeOutput( output );
		Assert.assertEquals( output, "[info] SolidBase v1.5.x (http://solidbase.org)\n" +
				"[info] \n" +
				"[info] Opening file 'X:/.../testpatch-parameter2.sql'\n" +
				"[info]     Encoding is 'ISO-8859-1'\n" +
				"[info] Connecting to database...\n" +
				"[info] The database is unmanaged.\n" +
				"[info] Setting up control tables to \"1.1\"\n" +
				"[info] Opening file 'X:/.../setup-1.1.sql'\n" +
				"[info]     Encoding is 'ISO-8859-1'\n" +
				"[info] Upgrading to \"1\"\n" +
				"[info] val1\n" +
				"[debug] DEBUG: version=null, target=1, statements=3\n" +
				"[info] \n" +
				"[info] Current database version is \"1\".\n" +
				"[info] Upgrade complete.\n"
				);
	}
}
