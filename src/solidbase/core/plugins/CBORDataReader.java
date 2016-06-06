package solidbase.core.plugins;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.util.JDBCSupport;
import solidstack.cbor.CBORReader;
import solidstack.cbor.CBORScanner.TYPE;
import solidstack.cbor.CBORScanner.Token;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.lang.ThreadInterrupted;


public class CBORDataReader // TODO implements RecordSource
{
	private CBORReader in;
	private ImportLogger counter;

	private DBWriter output;

	private Column[] columns;
	private String[] fieldNames;
	private String[] fileNames;


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

		for( Token t = this.in.get(); t.getType() == TYPE.ARRAY; t = this.in.get() )
		{
			// Detect interruption
			if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
				throw new ThreadInterrupted();

			List<Object> values = new ArrayList<Object>( this.columns.length );
			for( Object value = this.in.read(); value != null; value = this.in.read() )
				values.add( value );

			this.output.process( values.toArray() );

			if( this.counter != null )
				this.counter.count();
		}

		if( this.counter != null )
			this.counter.end();
	}
}
