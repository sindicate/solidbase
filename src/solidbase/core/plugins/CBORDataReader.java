package solidbase.core.plugins;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.util.JDBCSupport;
import solidstack.cbor.CBORReader;
import solidstack.cbor.Token;
import solidstack.cbor.Token.TYPE;
import solidstack.io.SourceException;
import solidstack.io.SourceInputStream;
import solidstack.io.SourceLocation;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.lang.ThreadInterrupted;


public class CBORDataReader implements RecordSource
{
	private CBORReader in;
	private ImportLogger counter;

	private RecordSink sink;

	private Column[] columns;
	private String[] fieldNames;
	private String[] fileNames;


	public CBORDataReader( SourceInputStream in, ImportLogger counter )
	{
		this.in = new CBORReader( in );
		this.counter = counter;

		// Read the header of the file
		long pos = in.getPos();
		JSONObject properties = (JSONObject)this.in.read();
		int version = properties.getNumber( "version" ).intValue();
		if( version != 1 )
			throw new SourceException( "Expected version 1", SourceLocation.forBinary( in.getResource(), pos ) );

		// Read the header of the data block
		properties = (JSONObject)this.in.read();

		// The fields
		JSONArray fields = properties.getArray( "fields" );
		int fieldCount = fields.size();

		this.columns = new Column[ fieldCount ];

		// Initialise the working arrays
		this.fieldNames = new String[ fieldCount ];
		this.fileNames = new String[ fieldCount ];

		for( int i = 0; i < fieldCount; i++ )
		{
			JSONObject field = (JSONObject)fields.get( i );
			this.fileNames[ i ] = field.findString( "file" );
			String name = this.fieldNames[ i ] = field.findString( "name" );
			this.columns[ i ] = new Column( name, JDBCSupport.fromTypeName( field.getString( "type" ) ), field.findString( "tableName" ), field.findString( "schemaName" ) );
		}
	}

	public void close()
	{
		this.in.close();
	}

	public String[] getFieldNames()
	{
		return this.fieldNames;
	}

	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	public void process() throws SQLException
	{
		this.sink.init( this.columns );
		this.sink.start();

		Token t;
		for( t = this.in.get(); t.type() == TYPE.IARRAY; t = this.in.get() )
		{
			for( t = this.in.get(); t.type() == TYPE.ARRAY; t = this.in.get() )
			{
				int len = t.length(); // TODO Check array length

				// Detect interruption
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				List<Object> values = new ArrayList<Object>( this.columns.length );
				for( int i = 0; i < len; i++ )
					values.add( this.in.read() );

				this.sink.process( values.toArray() );

				if( this.counter != null )
					this.counter.count();
			}

			if( t.type() != TYPE.BREAK )
				throw new SourceException( "Expected a BREAK, not " + t, null );
		}

		if( this.counter != null )
			this.counter.end();

		this.sink.end();
	}

	@Override
	public Column[] getColumns()
	{
		return this.columns;
	}

	@Override
	public Object[] getCurrentRecord()
	{
		throw new UnsupportedOperationException();
	}
}
