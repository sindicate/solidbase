/*--
 * Copyright 2016 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core.plugins;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidbase.core.ProcessException;
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

	private JSONArray firstRecord;


	public CBORDataReader( SourceInputStream in, ImportLogger counter )
	{
		this.in = new CBORReader( in );
		this.counter = counter;
	}

	public void close()
	{
		this.in.close();
	}

	public String[] getFieldNames()
	{
		return this.fieldNames;
	}

	@Override
	public void setSink( RecordSink sink )
	{
		this.sink = sink;
	}

	public void process() throws SQLException
	{
		SourceLocation loc = this.in.getLocation();

		Token token = this.in.peek();
		if( token.type() == TYPE.MAP )
		{
			// Read the header of the file
			JSONObject properties = (JSONObject)this.in.read();
			if( properties.has( "version" ) )
			{
				// TODO java.lang.ClassCastException: Attribute 'version' is not a Number ---> SourceException
				int version = properties.getNumber( "version" ).intValue();
				if( version != 1 )
					throw new SourceException( "Expected version 1", loc );

				// Read next object
				if( this.in.peek().type() == TYPE.MAP )
					properties = (JSONObject)this.in.read();
				else
					properties = null;
			}

			// Read the header of the data block
			if( properties != null )
			{
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
		}
		else if( token.type() != TYPE.EOF )
			if( token.type() != TYPE.IARRAY )
				throw new SourceException( "Expected MAP or IARRAY, not " + token, loc );

		this.sink.init( this.columns );
		this.sink.start();

		Token t;
		loc = this.in.getLocation();
		for( t = this.in.get(); t.type() != TYPE.EOF; t = this.in.get() )
		{
			if( t.type() != TYPE.IARRAY )
				throw new SourceException( "Expected IARRAY, not " + t, loc );

			loc = this.in.getLocation();
			for( t = this.in.get(); t.type() == TYPE.ARRAY; t = this.in.get() )
			{
				int len = t.length(); // TODO Check array length

				// Detect interruption
				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
					throw new ThreadInterrupted();

				loc = this.in.getLocation();

				List<Object> values = new ArrayList<>( len );
				for( int i = 0; i < len; i++ )
					values.add( this.in.read() );

				try
				{
					this.sink.process( values.toArray() );
				}
				catch( ProcessException e )
				{
					throw new ProcessException( e ).addLocation( loc );
				}

				if( this.counter != null )
					this.counter.count();
			}

			if( t.type() != TYPE.BREAK )
				throw new SourceException( "Expected ARRAY or BREAK, not " + t, loc );
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
