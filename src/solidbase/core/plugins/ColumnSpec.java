package solidbase.core.plugins;

class ColumnSpec
{
	protected boolean skip;
	protected FileSpec toFile;

	protected ColumnSpec( boolean skip, FileSpec toFile )
	{
		this.skip = skip;
		this.toFile = toFile;
	}
}