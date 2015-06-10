package solidbase.core.plugins;

import java.io.OutputStream;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileSpec
{
	static private final Pattern pattern = Pattern.compile( "\\?(\\d+)" );

	boolean binary;
	int threshold;
	String fileName;
	private boolean parameterized;

	OutputStream out;
	Writer writer;
	int index;

	private RecordSource source;


	protected FileSpec( boolean binary, String fileName, int threshold )
	{
		this.binary = binary;
		this.fileName = fileName;
		this.threshold = threshold;

		this.parameterized = FileSpec.pattern.matcher( fileName ).find();
	}

	protected void setSource( RecordSource source )
	{
		this.source = source;
	}

	protected boolean isParameterized()
	{
		return this.parameterized;
	}

	protected String generateFileName()
	{
		Object[] values = this.source.getCurrentValues();

		Matcher matcher = FileSpec.pattern.matcher( this.fileName );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int index = Integer.parseInt( matcher.group( 1 ) );
			matcher.appendReplacement( result, values[ index - 1 ].toString() ); // TODO Does this work for every type?
		}
		matcher.appendTail( result );
		return result.toString();
	}
}