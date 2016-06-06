package solidbase.core.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.util.JDBCSupport;
import solidstack.cbor.CBORReader;
import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;
import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.io.SegmentedInputStream;
import solidstack.io.SegmentedReader;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.lang.ThreadInterrupted;


public class CBORDataReader
{
	private CBORReader in;
	private ImportLogger counter;

	private DBWriter output;
	private boolean done;

	private String binaryFile;
	private Column[] columns;
	private String[] fieldNames;
	private String[] fileNames;
	private SegmentedInputStream[] streams;
	private SegmentedReader[] textStreams;


	public CBORDataReader( InputStream in, ImportLogger counter )
	{
		this.in = new CBORReader( in );
		this.counter = counter;

		// Read the header
		JSONObject properties = (JSONObject)this.in.readNoStream();

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

	// TODO Close the files

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
		this.output.init( this.columns );

		boolean commit = false; // boolean to see if we reached the end
		try
		{
			for( Token t = this.in.get(); t.getType() == TYPE.ARRAY; t = this.in.get() )
			{
				// Detect interruption
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				List<Object> values = new ArrayList<Object>( this.columns.length );
				for( Object value = this.in.read(); value != null; value = this.in.read() )
					values.add( value );

//					if( this.counter != null )
//						this.counter.end();
//
//					commit = true;
//					return;

//				for( int i = 0; i < values.size(); i++ )
//				{
//					int type = this.columns[ i ].getType();
//					Object value;
//					try
//					{
//						value = values.get( i );
//					}
//					catch( ArrayIndexOutOfBoundsException e )
//					{
//						throw new SourceException( "Value with index " + ( i + 1 ) + " does not exist, record has only " + values.size() + " values", null );
//					}
//
////						if( type == Types.CLOB )
////						{
////							if( values.get( index ) == null )
////								System.out.println( "NULL!" );
////							else if( ( (String)values.get( index ) ).length() == 0 )
////								System.out.println( "EMPTY!" );
////
////							// TODO What if it is a CLOB and the string value is too long?
////							// Oracle needs this because CLOBs can contain empty strings "", and setObject() makes that null BUT THIS DOES NOT WORK!
////							statement.setCharacterStream( pos++, new StringReader( (String)values.get( index ) ) );
////						}
////						else
//							// MonetDB complains when calling setObject with null value
////							Object v = values.get( index );
////						if( v != null )
//							values.set( i, value );
////						else
////							statement.setNull( pos++, type );
//				}

//				try
//				{
					this.output.process( values.toArray() );
//				}
//				catch( SourceException e )
//				{
//					throw e;
//				}
//				catch( SQLExecutionException e )
//				{
//					throw e;
//				}

				if( this.counter != null )
					this.counter.count();
			}

			if( this.counter != null )
				this.counter.end();

			commit = true;
		}
		finally
		{
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
