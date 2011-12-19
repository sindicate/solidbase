package solidbase.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Map.Entry;

import solidbase.core.SystemException;

public class JSONWriter
{
	private Writer out;

	public JSONWriter( Resource resource )
	{
		try
		{
			this.out = new OutputStreamWriter( resource.getOutputStream(), "UTF-8" );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}

	public void write( Object object )
	{
		try
		{
			if( object == null )
				writeNotString( "null" );
			else if( object instanceof JSONObject )
				writeObject( (JSONObject)object );
			else if( object instanceof JSONArray )
				writeArray( (JSONArray)object );
			else if( object instanceof String )
				writeString( (String)object );
			else if( object instanceof BigDecimal )
				writeNotString( ( (BigDecimal)object ).toString() );
			else if( object instanceof Boolean )
				writeNotString( ( (Boolean)object ).toString() );
			else
				throw new SystemException( "Unexpected object type: " + object.getClass().getName() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	private void writeString( String string ) throws IOException
	{
		this.out.write( '"' );
		this.out.write( string );
		this.out.write( '"' );
	}

	private void writeNotString( String number ) throws IOException
	{
		this.out.write( number );
	}

	private void writeObject( JSONObject object ) throws IOException
	{
		Writer out = this.out;

		out.write( '{' );

		boolean first = true;
		for( Entry< String, Object > entry : object )
		{
			if( first )
				first = false;
			else
				out.write( ',' );
			writeString( entry.getKey() );
			out.write( ':' );
			write( entry.getValue() );
		}

		out.write( '}' );
	}

	private void writeArray( JSONArray array ) throws IOException
	{
		Writer out = this.out;

		out.write( '[' );

		boolean first = true;
		for( Object object : array )
		{
			if( first )
				first = false;
			else
				out.write( ',' );
			write( object );
		}

		out.write( ']' );
	}

	public void close()
	{
		try
		{
			this.out.close();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
