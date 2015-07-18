/*--
 * Copyright 2010 René M. de Bloois
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

package solidstack.bson;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Map.Entry;

import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;

public class BSONWriter
{
	static private final Charset utf8 = Charset.forName( "UTF-8" );

	static private int DOUBLE = 0x01;
	static private int UTF8 = 0x02;
	static private int DOCUMENT = 0x03;
	static private int ARRAY = 0x04;
//	static private int BINARY = 0x05;
//	static private int OBJECTID = 0x07;
	static private int BOOLEAN = 0x08;
//	static private int UTCDATETIME = 0x09;
	static private int NULL = 0x0A;
//	static private int REGEXP = 0x0B;
//	static private int JAVASCRIPT = 0x0D;
//	static private int JAVASCRIPT_WITH_SCOPE = 0x0F;
	static private int INTEGER = 0x10;
//	static private int TIMESTAMP = 0x11;
	static private int LONG = 0x12;
//	static private int MINKEY = 0xFF;
//	static private int MAXKEY = 0x7F;

	private OutputStream out;


	public BSONWriter( Resource resource )
	{
		this( resource.getOutputStream() );
	}

	public BSONWriter( OutputStream out )
	{
		this.out = out;
	}

	public OutputStream getOutputStream()
	{
		return this.out;
	}

	public void write( Object object )
	{
		try
		{
			writeInternal( object );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	// TODO More types to implement here?
	// TODO Tests?
	// TODO Dates, binary
	private void writeInternal( Object object ) throws IOException
	{
		if( object == null )
			this.out.write( NULL );
		else if( object instanceof String )
			writeString( (String)object );
		else if( object instanceof BigDecimal )
			writeString( ( (BigDecimal)object ).toString() );
		else if( object instanceof Integer )
			writeInt( (Integer)object );
		else if( object instanceof Long )
			writeLong( (Long)object );
		else if( object instanceof Float )
			writeDouble( (Float)object );
		else if( object instanceof Double )
			writeDouble( (Double)object );
		else if( object instanceof JSONObject )
			writeObject( (JSONObject)object );
		else if( object instanceof JSONArray )
			writeArray( (JSONArray)object );
		else if( object instanceof Boolean )
			writeBoolean( (Boolean)object );
		else
			throw new ClassCastException( "Unexpected object type: " + object.getClass().getName() );
	}

	private void writeBareInt( int i ) throws IOException
	{
		this.out.write( i >>> 0 & 0xFF );
		this.out.write( i >>> 8 & 0xFF );
		this.out.write( i >>> 16 & 0xFF );
		this.out.write( i >>> 24 & 0xFF );
	}

	private void writeString( String string ) throws IOException
	{
		this.out.write( UTF8 );
		writeBareInt( string.length() );
		this.out.write( string.getBytes( utf8 ) );
	}

	private void writeInt( int i ) throws IOException
	{
		this.out.write( INTEGER );
		writeBareInt( i );
	}

	private void writeLong( long l ) throws IOException
	{
		this.out.write( LONG );
		writeBareInt( (int)l );
		writeBareInt( (int)( l >>> 32 ) );
	}

	private void writeDouble( double d ) throws IOException
	{
		this.out.write( DOUBLE );
		writeLong( Double.doubleToLongBits( d ) );
	}

	private void writeBoolean( boolean b ) throws IOException
	{
		this.out.write( BOOLEAN );
		this.out.write( b ? 0x01 : 0x00 );
	}

	private void writeObject( JSONObject object ) throws IOException
	{
		this.out.write( DOCUMENT );
		for( Entry< String, Object > entry : object )
		{
			writeString( entry.getKey() );
			writeInternal( entry.getValue() );
		}
		this.out.write( 0x00 );
	}

	private void writeArray( JSONArray array ) throws IOException
	{
		this.out.write( ARRAY );
		for( Object object : array )
			writeInternal( object );
		this.out.write( 0x00 );
	}

	public void close()
	{
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}
}
