package solidbase.core.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Types;

import solidbase.core.SQLExecutionException;
import solidbase.util.CloseQueue;
import solidbase.util.JDBCSupport;
import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.io.SegmentedInputStream;
import solidstack.io.SegmentedReader;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.json.JSONReader;
import solidstack.lang.Assert;
import solidstack.lang.SystemException;
import solidstack.lang.ThreadInterrupted;


// TODO BufferedInputStreams?
public class JSONDataReader // TODO implements RecordSource
{
	static public Charset UTF8 = Charset.forName( "UTF-8" );

	private JSONReader reader;
	private boolean prependLineNumber;
	private ImportLogger counter;

	private RecordSink sink;

	private String binaryFile;
	private Column[] columns;
	private String[] fieldNames;
	private String[] fileNames;
	private SegmentedInputStream[] streams;
	private SegmentedReader[] textStreams;

	private JSONArray firstRecord;


	public JSONDataReader( SourceReader reader, boolean prependLineNumber, boolean emptyLineIsEOF, ImportLogger counter )
	{
		this.reader = new JSONReader( reader, emptyLineIsEOF );
		this.prependLineNumber = prependLineNumber;
		this.counter = counter;

		// Read the header
		Object object = this.reader.read();
		if( object instanceof JSONObject )
		{
			JSONObject properties = (JSONObject)object;

			// The default binary file
			this.binaryFile = properties.findString( "binaryFile" );

			// The fields
			JSONArray fields = properties.getArray( "fields" );
			int fieldCount = fields.size();

			this.columns = new Column[ fieldCount ];

			// Initialise the working arrays
			this.fieldNames = new String[ fieldCount ];
			this.fileNames = new String[ fieldCount ];
			this.streams = new SegmentedInputStream[ fieldCount ];
			this.textStreams = new SegmentedReader[ fieldCount ];

			for( int i = 0; i < fieldCount; i++ )
			{
				JSONObject field = (JSONObject)fields.get( i );
				this.fileNames[ i ] = field.findString( "file" );
				String name = this.fieldNames[ i ] = field.findString( "name" );
				this.columns[ i ] = new Column( name, JDBCSupport.fromTypeName( field.getString( "type" ) ), field.findString( "tableName" ), field.findString( "schemaName" ) );
			}
		}
		else if( object != null )
			this.firstRecord = (JSONArray)object;
	}

	public String[] getFieldNames()
	{
		return this.fieldNames;
	}

	public void setOutput( RecordSink output )
	{
		this.sink = output;
	}

	public void process() throws SQLException
	{
		this.sink.init( this.columns );
		this.sink.start();

		// Queues that will remember the files we need to close
		CloseQueue outerCloser = new CloseQueue();
		CloseQueue closer = new CloseQueue();

		try
		{
			while( true )
			{
				// Detect interruption
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				// Read a record
				JSONArray array;
				if( this.firstRecord != null )
				{
					array = this.firstRecord;
					this.firstRecord = null;
				}
				else
					array = (JSONArray)this.reader.read();
				if( array == null )
				{
					// End of file, finalize things
					Assert.isTrue( this.reader.isEOF() );

					this.sink.end();

					if( this.counter != null )
						this.counter.end();

					return;
				}

				SourceLocation location = this.reader.getLocation();

				Object[] values = new Object[ array.size() + ( this.prependLineNumber ? 1 : 0 ) ];
				int pos = 0;
				if( this.prependLineNumber )
					values[ pos++ ] = location.getLineNumber();

				for( int i = 0; i < array.size(); i++ )
				{
					Object value;
					try
					{
						value = array.get( i );
					}
					catch( ArrayIndexOutOfBoundsException e )
					{
						throw new SourceException( "Value with index " + ( i + 1 ) + " does not exist, record has only " + array.size() + " values", this.reader.getLocation() );
					}

					if( value instanceof JSONObject )
					{
						if( this.columns == null )
							throw new SourceException( "File refs only supported with a JSON header object that defines the field types", this.reader.getLocation() );

						int type = this.columns[ i ].getType();

						// Value of parameter is in a separate file
						JSONObject object = (JSONObject)value;
						String filename = object.findString( "file" );
						if( filename != null )
						{
							// One file per record
							if( type == Types.BLOB || type == Types.VARBINARY )
							{
								try
								{
									// TODO Fix the input stream size given the size in the JSON file
									Resource r = this.reader.getResource().resolve( filename );
									Number filesize = object.findNumber( "size" );
									if( filesize == null || filesize.intValue() > 10240 ) // TODO Whats a good size here? Should it be a long?
									{
										// Some databases read the stream directly (Oracle), others read it later (HSQLDB).
										// TODO Do we need to decrease the batch size when files are being kept open?
										// TODO We could detect that the database has read the stream already, and close the file
										InputStream in = r.newInputStream();
										values[ pos++ ] = in;
										closer.add( in );
									}
									else
										values[ pos++ ] = readBytes( r ); // TODO Do a speed test
								}
								catch( FileNotFoundException e )
								{
									throw new SourceException( e.getMessage(), this.reader.getLocation() );
								}
							}
							else
								Assert.fail( "Unexpected field type for external file: " + JDBCSupport.toTypeName( type ) );
						}
						else
						{
							// One file for all records
							Number lobIndex = object.getNumber( "index" ); // TODO Use findNumber
							if( lobIndex == null )
								throw new SourceException( "Expected a 'file' or 'index' attribute", this.reader.getLocation() );
							Number lobLength = object.getNumber( "length" );
							if( lobLength == null )
								throw new SourceException( "Expected a 'length' attribute", this.reader.getLocation() );

							if( type == Types.BLOB || type == Types.VARBINARY )
							{
								// Get the input stream
								SegmentedInputStream in = this.streams[ i ];
								if( in == null )
								{
									// File not opened yet, open it
									String fileName = this.fileNames[ i ];
									if( fileName == null )
										fileName = this.binaryFile;
									if( fileName == null )
										throw new SourceException( "No file or default binary file configured", this.reader.getLocation() );
									Resource r = this.reader.getResource().resolve( fileName );
									try
									{
										in = new SegmentedInputStream( r.newInputStream() );
										outerCloser.add( in ); // Close at the final end
										this.streams[ i ] = in;
									}
									catch( FileNotFoundException e )
									{
										throw new SourceException( e.getMessage(), this.reader.getLocation() );
									}
								}
								// TODO Maybe use the limited setBinaryStream instead (see DBWriter)
								values[ pos++ ] = in.getSegmentInputStream( lobIndex.longValue(), lobLength.longValue() );
							}
							else if( type == Types.CLOB )
							{
								// Get the reader
								SegmentedReader in = this.textStreams[ i ];
								if( in == null )
								{
									// File not opened yet, open it
									if( this.fileNames[ i ] == null )
										throw new SourceException( "No file configured", this.reader.getLocation() );
									Resource r = this.reader.getResource().resolve( this.fileNames[ i ] );
									try
									{
										try
										{
											in = new SegmentedReader( new InputStreamReader( r.newInputStream(), "UTF-8" ) );
										}
										catch( UnsupportedEncodingException e )
										{
											throw new SystemException( e );
										}
										outerCloser.add( in ); // Close at the final end
										this.textStreams[ i ] = in;
									}
									catch( FileNotFoundException e )
									{
										throw new SourceException( e.getMessage(), this.reader.getLocation() );
									}
								}
								values[ pos++ ] = in.getSegmentReader( lobIndex.longValue(), lobLength.longValue() );
							}
							else
								Assert.fail( "Unexpected field type for external file: " + JDBCSupport.toTypeName( type ) );
						}
					}
					else
					{
//						if( type == Types.CLOB )
//						{
//							if( values.get( index ) == null )
//								System.out.println( "NULL!" );
//							else if( ( (String)values.get( index ) ).length() == 0 )
//								System.out.println( "EMPTY!" );
//
//							// TODO What if it is a CLOB and the string value is too long?
//							// Oracle needs this because CLOBs can contain empty strings "", and setObject() makes that null BUT THIS DOES NOT WORK!
//							statement.setCharacterStream( pos++, new StringReader( (String)values.get( index ) ) );
//						}
//						else
							// MonetDB complains when calling setObject with null value
//							Object v = values.get( index );
//						if( v != null )
							values[ pos++ ] = array.get( i );
//						else
//							statement.setNull( pos++, type );
					}
				}

				try
				{
					this.sink.process( values );
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

				closer.closeAll();

				if( this.counter != null )
					this.counter.count();
			}
		}
		finally
		{
			outerCloser.closeAll();
			closer.closeAll();
		}
	}

	static byte[] readBytes( Resource resource ) throws FileNotFoundException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[ 4096 ];
		try
		{
			InputStream in = resource.newInputStream();
			try
			{
				int read;
				while( ( read = in.read( buffer ) ) >= 0 )
					bytes.write( buffer, 0, read );
			}
			finally
			{
				in.close();
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
		return bytes.toByteArray();
	}
}
