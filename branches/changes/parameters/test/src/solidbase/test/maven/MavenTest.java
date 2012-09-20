package solidbase.test.maven;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;
import org.testng.annotations.Test;

import solidbase.core.TestUtil;
import solidbase.maven.Parameter;
import solidbase.maven.SQLMojo;

public class MavenTest
{
	@Test
	static public void test()
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
}
