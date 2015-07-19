package solidbase.core.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ListIterator;

import solidbase.core.SQLExecutionException;
import solidbase.core.SourceException;
import solidbase.util.CloseQueue;
import solidbase.util.JDBCSupport;
import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.io.SegmentedInputStream;
import solidstack.io.SegmentedReader;
import solidstack.io.SourceLocation;
import solidstack.io.SourceReader;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.json.JSONReader;
import solidstack.lang.Assert;
import solidstack.lang.SystemException;
import solidstack.lang.ThreadInterrupted;

public class JSONDataReader
{
	private JSONReader reader;
	private boolean prependLineNumber;
	private ImportLogger counter;

	private DBWriter output;
	private boolean done;

	private String binaryFile;
	private String[] fieldNames;
	private int[] fieldTypes;
	private String[] fileNames;
	private SegmentedInputStream[] streams;
	private SegmentedReader[] textStreams;

	public JSONDataReader( SourceReader reader, boolean prependLineNumber, ImportLogger counter )
	{
		this.reader = new JSONReader( reader );
		this.prependLineNumber = prependLineNumber;
		this.counter = counter;

		// Read the header
		JSONObject properties = (JSONObject)this.reader.read();

		// The default binary file
		this.binaryFile = properties.findString( "binaryFile" );

		// The fields
		JSONArray fields = properties.getArray( "fields" );
		int fieldCount = fields.size();

		// Initialise the working arrays
		this.fieldTypes = new int[ fieldCount ];
		this.fieldNames = new String[ fieldCount ];
		this.fileNames = new String[ fieldCount ];
		this.streams = new SegmentedInputStream[ fieldCount ];
		this.textStreams = new SegmentedReader[ fieldCount ];

		for( int i = 0; i < fieldCount; i++ )
		{
			JSONObject field = (JSONObject)fields.get( i );
			this.fieldTypes[ i ] = JDBCSupport.fromTypeName( field.getString( "type" ) );
			this.fieldNames[ i ] = field.findString( "name" );
			this.fileNames[ i ] = field.findString( "file" );
		}
	}

	public String[] getFieldNames()
	{
		return this.fieldNames;
	}

	public void setOutput( DBWriter output )
	{
		this.output = output;
	}

	public void process() throws SQLException
	{
		// Queues the will remember the files we need to close
		CloseQueue outerCloser = new CloseQueue();
		CloseQueue closer = new CloseQueue();

		boolean commit = false; // boolean to see if we reached the end
		try
		{
			while( true )
			{
				// Detect interruption
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				// Read a record
				JSONArray array = (JSONArray)this.reader.read();
				if( array == null )
				{
					// End of file, finalize things
					Assert.isTrue( this.reader.isEOF() );

					if( this.counter != null )
						this.counter.end();

					commit = true;
					return;
				}

				SourceLocation location = this.reader.getLocation();

				try
				{
					// Convert the strings to date, time and timestamps
					// TODO Time zones, is there a default way of putting times and dates in a text file? For example whats in a HTTP header?
					int i = 0;
					for( ListIterator< Object > it = array.iterator(); it.hasNext(); )
					{
						Object value = it.next();
						if( value != null )
						{
							// TODO Switch statement
							if( this.fieldTypes[ i ] == Types.DATE )
								it.set( java.sql.Date.valueOf( (String)value ) );
							else if( this.fieldTypes[ i ] == Types.TIMESTAMP )
								it.set( java.sql.Timestamp.valueOf( (String)value ) );
							else if( this.fieldTypes[ i ] == Types.TIME )
								it.set( java.sql.Time.valueOf( (String)value ) );
						}
						i++;
					}
				}
				catch( IllegalArgumentException e )
				{
					// TODO Add test? C:\_WORK\SAO-20150612\build.xml:32: The following error occurred while executing this line:
					// C:\_WORK\SAO-20150612\build.xml:13: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff], at line 17 of file C:/_WORK/SAO-20150612/SYSTEEM/sca.JSON.GZ
					throw new SourceException( e.getMessage(), this.reader.getLocation() );
				}

				Object[] values = new Object[ this.fieldNames.length + ( this.prependLineNumber ? 1 : 0 ) ];
				int pos = 0;
				if( this.prependLineNumber )
					values[ pos++ ] = location.getLineNumber();

				for( int i = 0; i < array.size(); i++ )
				{
					int type = this.fieldTypes[ i ];
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
									BigDecimal filesize = object.findNumber( "size" );
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
							BigDecimal lobIndex = object.getNumber( "index" ); // TODO Use findNumber
							if( lobIndex == null )
								throw new SourceException( "Expected a 'file' or 'index' attribute", this.reader.getLocation() );
							BigDecimal lobLength = object.getNumber( "length" );
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

					try
					{
						this.output.process( values );
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
			this.output.end( commit );
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
