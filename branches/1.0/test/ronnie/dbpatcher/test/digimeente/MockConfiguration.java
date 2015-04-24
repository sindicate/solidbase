package ronnie.dbpatcher.test.digimeente;

import java.io.File;

import mockit.Mock;


/**
 *
 * @author René M. de Bloois
 * @since Dec 13, 2008
 */
public class MockConfiguration
{
	protected String propertyFileName;

	protected MockConfiguration( String propertyFileName )
	{
		this.propertyFileName = propertyFileName;
	}

	@Mock
	public File getPropertiesFile()
	{
		return new File( this.propertyFileName );
	}
}
