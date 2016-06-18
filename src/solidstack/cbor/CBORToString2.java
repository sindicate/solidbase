package solidstack.cbor;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import solidstack.cbor.Token.TYPE;
import solidstack.io.FatalIOException;
import solidstack.io.SourceInputStream;

public class CBORToString2
{
	private CBORScanner in;
	private String spaces = "                ";

	public CBORToString2( CBORScanner in )
	{
		this.in = in;
	}

	public CBORToString2( byte[] bytes )
	{
		this( new CBORScanner( new SourceInputStream( new ByteArrayInputStream( bytes ), null, 0 ) ) );
	}

	public CBORToString2( InputStream in )
	{
		this( new CBORScanner( new SourceInputStream( in, null, 0 ) ) );
	}

	@Override
	public String toString()
	{
		CharArrayWriter out = new CharArrayWriter();
		toString( out );
		return out.toString();
	}

	public void toString( Writer out )
	{
		CBORScanner in = this.in;
		int indent = 0;

		try
		{
			boolean eof = toString( in, out, indent );
			while( !eof )
				eof = toString( in, out, indent );
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	private boolean toString( CBORScanner in, Writer out, int indent ) throws IOException
	{
		Token t = in.get();
		if( t.type() == TYPE.EOF )
			return true;
		if( t.type() == TYPE.BREAK )
		{
			toString( t, in, out, indent - 4 );
			return true;
		}
		toString( t, in, out, indent );
		return false;
	}

	private void toString( Token token, CBORScanner in, Writer out, int indent ) throws IOException
	{
		if( indent > 0 )
		{
			if( indent > this.spaces.length() )
				this.spaces += this.spaces;
			out.append( this.spaces.substring( 0, indent ) );
		}

		out.append( token.toString() );

		TYPE type = token.type();
		switch( type )
		{
			case BYTES:
				byte[] bytes = new byte[ token.length() ];
				in.readBytes( bytes );
				out.append( ':' );
				appendHex( out, bytes );
				out.append( '\n' );
				break;

			case TEXT:
				out.append( ": \"" );
				appendString( out, in.readString( token.length() ) );
				out.append( "\"\n" );
				break;

			case TAG:
				out.append( ' ' );
				toString( in, out, 0 );
				break;

			case ARRAY:
				out.append( '\n' );
				int len = token.length();
				indent += 4;
				for( int i = 0; i < len; i++ )
					toString( in, out, indent );
				break;

			case IARRAY:
				out.append( '\n' );
				indent += 4;
				boolean eof = toString( in, out, indent );
				while( !eof )
					eof = toString( in, out, indent );
				break;

			case MAP:
				out.append( '\n' );
				len = token.length() * 2;
				indent += 4;
				for( int i = 0; i < len; i++ )
					toString( in, out, indent );
				break;

			case IMAP:
				out.append( '\n' );
				indent += 4;
				eof = toString( in, out, indent );
				while( !eof )
					eof = toString( in, out, indent );
				break;

			case IBYTES:
				out.append( '\n' );
				indent += 4;
				eof = toString( in, out, indent );
				while( !eof )
					eof = toString( in, out, indent );
				break;

			case ITEXT:
				out.append( '\n' );
				indent += 4;
				eof = toString( in, out, indent );
				while( !eof )
					eof = toString( in, out, indent );
				break;

			default:
				out.append( '\n' );
		}
	}

	private void appendHex( Writer out, byte[] bytes ) throws IOException
	{
		for( byte b : bytes )
		{
			String hex = Integer.toHexString( b & 0xFF ).toUpperCase();
			out.append( ' ' );
			if( hex.length() == 1 )
				out.append( '0' );
			out.append( hex );
		}
	}

	private void appendString( Writer out, String s ) throws IOException
	{
		for( char ch : s.toCharArray() )
		{
			switch( ch )
			{
				case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
				case 0x0B: case 0x0E: case 0x0F:
				case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
				case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
				case 0x7F:
				case 0x80: case 0x81: case 0x82: case 0x83: case 0x84: case 0x85: case 0x86: case 0x87:
				case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8E: case 0x8F:
				case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
				case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
				case 0x2028: // Line separator
				case 0x2029: // Paragraph separator
					out.append( escape( ch ) ); break;
				case '\b': out.append( "\\b" ); break;
				case '\t': out.append( "\\t" ); break;
				case '\n': out.append( "\\n" ); break;
				case '\f': out.append( "\\f" ); break;
				case '\r': out.append( "\\r" ); break;
				case '"': out.append( "\\\"" ); break;
				case '\\': out.append( "\\\\" ); break;
				default: out.append( ch );
			}
		}
	}

	private String escape( int ch )
	{
		String hex = Integer.toHexString( ch ).toUpperCase();
		return "\\u" + ( "0000" + hex ).substring( hex.length() );
	}
}
