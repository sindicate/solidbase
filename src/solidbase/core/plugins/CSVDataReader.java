package solidbase.core.plugins;

import java.sql.SQLException;
import java.sql.Types;

import solidbase.core.SQLExecutionException;
import solidbase.util.CSVReader;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.lang.ThreadInterrupted;


public class CSVDataReader // TODO implements RecordSource
{
	private CSVReader reader;
	private boolean prependLineNumber;
	private ImportLogger counter;

	private RecordSink sink;
	private boolean done;


	public CSVDataReader( SourceReader sourceReader, boolean skipHeader, char separator, boolean escape, boolean ignoreWhiteSpace, boolean prependLineNumber, ImportLogger counter )
	{
		// Initialize csv reader & read first line (and skip header if needed)
		this.reader = new CSVReader( sourceReader, separator, escape, ignoreWhiteSpace );
		this.prependLineNumber = prependLineNumber;
		this.counter = counter;

		if( skipHeader )
		{
			String[] line = this.reader.getLine();
			if( line == null )
				this.done = true;
		}
	}

	public void setOutput( RecordSink output )
	{
		this.sink = output;
	}

	public void process() throws SQLException
	{
		boolean initDone = false;

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

			if( !initDone )
			{
				Column[] columns = new Column[ line.length ];
				for( int i = 0; i < columns.length; i++ )
					columns[ i ] = new Column( null, Types.VARCHAR, null, null );
				this.sink.init( columns );
				initDone = true;
			}

			this.sink.start();
			try
			{
				this.sink.process( line );
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
			this.sink.end();

			if( this.counter != null )
				this.counter.count();

			location = this.reader.getLocation();
			line = this.reader.getLine();
		}

		if( this.counter != null )
			this.counter.end();
	}
}
