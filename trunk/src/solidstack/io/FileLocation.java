package solidstack.io;


public class FileLocation
{
	private Resource resource;
	private int pos;

	public FileLocation( Resource resource, int pos )
	{
		this.resource = resource;
		this.pos = pos;
	}

	public Resource getResource()
	{
		return this.resource;
	}

	public int getLineNumber()
	{
		return this.pos;
	}

	public FileLocation previousLine()
	{
		if( this.pos <= 0 )
			throw new FatalIOException( "There is no previous line" );
		return new FileLocation( this.resource, this.pos - 1 );
	}

	public FileLocation lineNumber( int lineNumber )
	{
		return new FileLocation( this.resource, lineNumber );
	}

	@Override
	public String toString()
	{
		return "line " + this.pos + " of file " + this.resource;
	}
}
