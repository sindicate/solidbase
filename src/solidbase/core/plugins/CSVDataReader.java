package solidbase.core.plugins;

import solidbase.util.CSVReader;
import solidstack.io.SourceReader;
import solidstack.lang.ThreadInterrupted;

public class CSVDataReader
{
	private CSVReader reader;
	private DBWriter output;
	private ImportLogger counter;

	private boolean done;

	public CSVDataReader( SourceReader sourceReader, boolean skipHeader, char separator, boolean ignoreWhiteSpace, boolean prependLineNumber, ImportLogger counter )
	{
		// Initialize csv reader & read first line (and skip header if needed)
		this.reader = new CSVReader( sourceReader, separator, ignoreWhiteSpace );
		this.counter = counter;

		if( skipHeader )
		{
			String[] line = this.reader.getLine();
			if( line == null )
				this.done = true;
		}
//		int lineNumber = reader.getLineNumber();
//		String[] line = reader.getLine();
//		if( line == null )
//			return true;
	}

	public void setOutput( DBWriter output )
	{
		this.output = output;
	}

	public void process()
	{
		int lineNumber = this.reader.getLineNumber();
		String[] line = this.reader.getLine();
		while( line != null )
		{
			if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
				throw new ThreadInterrupted();

			for( int i = 0; i < line.length; i++ )
				if( line[ i ].length() == 0 )
					line[ i ] = null;

			this.output.process( line );

			if( this.counter != null )
				this.counter.count();

			lineNumber = this.reader.getLineNumber();
			line = this.reader.getLine();
		}

		this.output.end();

		if( this.counter != null )
			this.counter.end();
	}

	public void setOuput( DBWriter writer )
	{
		// TODO Auto-generated method stub
	}
}
