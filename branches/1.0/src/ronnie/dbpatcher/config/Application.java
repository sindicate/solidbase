/**
 * 
 */
package ronnie.dbpatcher.config;

public class Application
{
	protected String name;
	protected String description;
	protected String userName;
	protected String patchFile;

	protected Application( String name, String description, String userName, String patchFile )
	{
		this.name = name;
		this.description = description;
		this.userName = userName;
		this.patchFile = patchFile;
	}

	public String getName()
	{
		return this.name;
	}

	public String getDescription()
	{
		return this.description;
	}

	public String getUserName()
	{
		return this.userName;
	}

	public String getPatchFile()
	{
		return this.patchFile;
	}

	static public class Comparator implements java.util.Comparator< Application >
	{
		public int compare( Application application1, Application application2 )
		{
			return application1.name.compareTo( application2.name );
		}
	}
}
