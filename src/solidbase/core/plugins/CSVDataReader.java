package solidbase.core.plugins;

import java.sql.SQLException;

import solidbase.core.SQLExecutionException;
import solidbase.core.SourceException;
import solidbase.util.CSVReader;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.lang.ThreadInterrupted;

public class CSVDataReader
{
	private CSVReader reader;
	private boolean prependLineNumber;
	private ImportLogger counter;

	private DBWriter output;
	private boolean done;

	public CSVDataReader( SourceReader sourceReader, boolean skipHeader, char separator, boolean ignoreWhiteSpace, boolean prependLineNumber, ImportLogger counter )
	{
		// Initialize csv reader & read first line (and skip header if needed)
		this.reader = new CSVReader( sourceReader, separator, ignoreWhiteSpace );
		this.prependLineNumber = prependLineNumber;
		this.counter = counter;

		if( skipHeader )
		{
			String[] line = this.reader.getLine();
			if( line == null )
				this.done = true;
		}
	}

	public void setOutput( DBWriter output )
	{
		this.output = output;
	}

	public void process() throws SQLException
	{
		boolean commit = false;
		try
		{
			SourceLocation location = this.reader.getLocation();
			String[] line = this.reader.getLine();
			while( line != null )
			{
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				for( int i = 0; i < line.length; i++ )
					if( line[ i ].length() == 0 )
						line[ i ] = null;

				if( this.prependLineNumber )
				{
					String[] temp = line;
					line = new String[ temp.length + 1 ];
					line[ 0 ] = Integer.toString( location.getLineNumber() );
					System.arraycopy( temp, 0, line, 1, temp.length );
				}

				try
				{
					this.output.process( line );
				}
				catch( SourceException e )
				{
					e.setLocation( location );
					throw e;
				}
				catch( SQLExecutionException e )
				{
					e.setLocation( location );
					throw e;
				}

				if( this.counter != null )
					this.counter.count();

				location = this.reader.getLocation();
				line = this.reader.getLine();
			}

			commit = true;
		}
		finally
		{
			this.output.end( commit );
		}

		if( this.counter != null )
			this.counter.end();
	}
}
