package solidbase.core.plugins;

import java.io.OutputStream;
import java.io.Writer;

class FileSpec
{
	protected boolean binary;
	protected int threshold;

	protected FileNameGenerator generator;
	protected OutputStream out;
	protected Writer writer;
	protected int index;

	protected FileSpec( boolean binary, String fileName, int threshold )
	{
		this.binary = binary;
		this.threshold = threshold;
		this.generator = new FileNameGenerator( fileName );
	}
}