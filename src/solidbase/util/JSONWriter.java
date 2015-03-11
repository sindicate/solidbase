package solidbase.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Map.Entry;

import solidbase.core.SystemException;
import solidstack.io.Resource;

public class JSONWriter
{
	static private final String ENCODING = "UTF-8";

	private Writer out;

	// Needed for formatted output
	private boolean format;
	private int maxLength;
	private int index;
	private BitSet bits;
	private int indent;
	private String tabs = "\t\t\t\t\t\t\t\t\t\t";

	public JSONWriter( Resource resource )
	{
		this( resource.getOutputStream() );
	}

	public JSONWriter( OutputStream out )
	{
		try
		{
			this.out = new OutputStreamWriter( out, ENCODING );
		}
		catch( UnsupportedEncodingException e )
		{
			throw new SystemException( e );
		}
	}

	public String getEncoding()
	{
		return ENCODING;
	}

	public Writer getWriter()
	{
		return this.out;
	}

	public void write( Object object )
	{
		this.format = false;
		writeInternal( object );
	}

	// TODO More types to implement here?
	private void writeInternal( Object object )
	{
		try
		{
			if( object == null )
				writeNotString( "null" );
			else if( object instanceof String )
				writeString( (String)object );
			else if( object instanceof BigDecimal )
				writeNotString( ( (BigDecimal)object ).toString() );
			else if( object instanceof Integer )
				writeNotString( ( (Integer)object ).toString() );
			else if( object instanceof Long )
				writeNotString( ( (Long)object ).toString() );
			else if( object instanceof JSONObject )
				writeObject( (JSONObject)object );
			else if( object instanceof JSONArray )
				writeArray( (JSONArray)object );
			else if( object instanceof Boolean )
				writeNotString( ( (Boolean)object ).toString() );
			else if( object instanceof Reader )
				writeReader( (Reader)object );
			else if( object instanceof CustomWriter )
				( (CustomWriter)object ).writeTo( this );
			else
				throw new SystemException( "Unexpected object type: " + object.getClass().getName() );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void writeFormatted( Object object, int maxLength )
	{
		this.maxLength = maxLength;
		this.index = 0;
		this.bits = new BitSet();

		if( object instanceof JSONObject )
			prepareObject( (JSONObject)object );
		else if( object instanceof JSONArray )
			prepareArray( (JSONArray)object );

		this.format = true;
		this.index = 0;
		this.indent = 0;

		writeInternal( object );
	}

	private int prepareObject( JSONObject object )
	{
		int index = this.index++;
		int len = 2; // { + space
		for( Entry< String, Object > entry : object )
			len += entry.getKey().length() + getLength( entry.getValue() ) + 6; // "" + : + space + , + space (or space + })
		if( len > this.maxLength )
			this.bits.set( index );
		return len;
	}

	private int prepareArray( JSONArray object )
	{
		int index = this.index++;
		int len = 2; // [ + space
		for( Object value : object )
			len += getLength( value ) + 2; // , + space or space + ]
		if( len > this.maxLength )
			this.bits.set( index );
		return len;
	}

	private int getLength( Object object )
	{
		if( object == null )
			return 4;
		if( object instanceof JSONObject )
			return prepareObject( (JSONObject)object );
		if( object instanceof JSONArray )
			return prepareArray( (JSONArray)object );
		if( object instanceof String )
			return ( (String)object ).length();
		if( object instanceof BigDecimal )
			return ( (BigDecimal)object ).toString().length();
		if( object instanceof Boolean )
			return ( (Boolean)object ).booleanValue() ? 4 : 5;
		if( object instanceof Integer )
			return ( (Integer)object ).toString().length();
		if( object instanceof CustomWriter )
			return ( (CustomWriter)object ).length();
		throw new SystemException( "Unexpected object type: " + object.getClass().getName() );
	}

	private void writeChars( char[] chars, int len ) throws IOException
	{
		Writer out = this.out;
		for( int i = 0; i < len; i++ )
		{
			char ch = chars[ i ];
			switch( ch )
			{
				case '"': out.write( "\\\"" ); break;
				case '\\': out.write( "\\\\" ); break;
				case '\b': out.write( "\\b" ); break;
				case '\f': out.write( "\\f" ); break;
				case '\n': out.write( "\\n" ); break;
				case '\r': out.write( "\\r" ); break;
				case '\t': out.write( "\\t" ); break;
				case 0x00: out.write( "\\u0000" ); break;
				case 0x01: out.write( "\\u0001" ); break;
				case 0x02: out.write( "\\u0002" ); break;
				case 0x03: out.write( "\\u0003" ); break;
				case 0x04: out.write( "\\u0004" ); break;
				case 0x05: out.write( "\\u0005" ); break;
				case 0x06: out.write( "\\u0006" ); break;
				case 0x07: out.write( "\\u0007" ); break;
				case 0x0B: out.write( "\\u000B" ); break;
				case 0x0E: out.write( "\\u000E" ); break;
				case 0x0F: out.write( "\\u000F" ); break;
				case 0x10: out.write( "\\u0010" ); break;
				case 0x11: out.write( "\\u0011" ); break;
				case 0x12: out.write( "\\u0012" ); break;
				case 0x13: out.write( "\\u0013" ); break;
				case 0x14: out.write( "\\u0014" ); break;
				case 0x15: out.write( "\\u0015" ); break;
				case 0x16: out.write( "\\u0016" ); break;
				case 0x17: out.write( "\\u0017" ); break;
				case 0x18: out.write( "\\u0018" ); break;
				case 0x19: out.write( "\\u0019" ); break;
				case 0x1A: out.write( "\\u001A" ); break;
				case 0x1B: out.write( "\\u001B" ); break;
				case 0x1C: out.write( "\\u001C" ); break;
				case 0x1D: out.write( "\\u001D" ); break;
				case 0x1E: out.write( "\\u001E" ); break;
				case 0x1F: out.write( "\\u001F" ); break;
				// According to ECMA-262 the characters below are not allowed too
				case 0x2028: out.write( "\\u2028" ); break; // Line separator
				case 0x2029: out.write( "\\u2029" ); break; // Paragraph separator
				default:
					out.write( ch );
			}
		}
	}

	private void writeString( String string ) throws IOException
	{
		this.out.write( '"' );
		writeChars( string.toCharArray(), string.length() );
		this.out.write( '"' );
	}

	private void writeReader( Reader reader ) throws IOException
	{
		this.out.write( '"' );
		char[] buf = new char[ 4096 ];
		for( int read = reader.read( buf ); read >= 0; read = reader.read( buf ) )
			writeChars( buf, read );
		reader.close();
		this.out.write( '"' );
	}

	private void writeNotString( String number ) throws IOException
	{
		this.out.write( number );
	}

	private void writeObject( JSONObject object ) throws IOException
	{
		boolean breakup = this.format && this.bits.get( this.index++ );

		Writer out = this.out;

		out.write( '{' );
		this.indent ++;
		if( breakup )
		{
			out.write( '\n' );
			while( this.tabs.length() < this.indent )
				this.tabs += this.tabs;
			out.write( this.tabs, 0, this.indent );
		}
		else if( this.format )
			out.write( ' ' );

		boolean first = true;
		for( Entry< String, Object > entry : object )
		{
			if( first )
				first = false;
			else
			{
				out.write( ',' );
				if( breakup )
				{
					out.write( '\n' );
					out.write( this.tabs, 0, this.indent );
				}
				else if( this.format )
					out.write( ' ' );
			}
			writeString( entry.getKey() );
			out.write( ':' );
			if( this.format )
				out.write( ' ' );
			writeInternal( entry.getValue() );
		}

		this.indent --;
		if( breakup )
		{
			out.write( '\n' );
			out.write( this.tabs, 0, this.indent );
		}
		else if( this.format )
			out.write( ' ' );
		out.write( '}' );
	}

	public void writeProperties( JSONObject properties )
	{
		Writer out = this.out;

		try
		{
			for( Entry< String, Object > entry : properties )
			{
				writeString( entry.getKey() );
				out.write( ": " );
				writeFormatted( entry.getValue(), 80 );
				out.write( '\n' );
			}

			out.write( '\n' );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	private void writeArray( JSONArray array ) throws IOException
	{
		boolean breakup = this.format && this.bits.get( this.index++ );

		Writer out = this.out;

		out.write( '[' );
		this.indent ++;
		if( breakup )
		{
			out.write( '\n' );
			while( this.tabs.length() < this.indent )
				this.tabs += this.tabs;
			out.write( this.tabs, 0, this.indent );
		}
		else if( this.format )
			out.write( ' ' );

		writeValuesInternal( array, breakup );

		this.indent --;
		if( breakup )
		{
			out.write( '\n' );
			out.write( this.tabs, 0, this.indent );
		}
		else if( this.format )
			out.write( ' ' );
		out.write( ']' );
	}

	public void writeValues( JSONArray array )
	{
		this.format = false;
		try
		{
			writeValuesInternal( array, false );
			this.out.write( '\n' );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	private void writeValuesInternal( JSONArray array, boolean breakup ) throws IOException
	{
		Writer out = this.out;

		boolean first = true;
		for( Object object : array )
		{
			if( first )
				first = false;
			else
			{
				out.write( ',' );
				if( breakup )
				{
					out.write( '\n' );
					out.write( this.tabs, 0, this.indent );
				}
				else if( this.format )
					out.write( ' ' );
			}
			writeInternal( object );
		}
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

	public interface CustomWriter
	{
		void writeTo( JSONWriter out );
		int length();
	}
}
