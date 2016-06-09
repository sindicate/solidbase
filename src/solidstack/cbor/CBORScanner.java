package solidstack.cbor;

import java.io.IOException;

import solidstack.cbor.CBORScanner.Token.TYPE;
import solidstack.io.FatalIOException;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceInputStream;
import solidstack.io.SourceLocation;

public class CBORScanner
{
	private SourceInputStream in;


	public CBORScanner( SourceInputStream in )
	{
		this.in = in;
	}

	public Resource getResource()
	{
		return this.in.getResource();
	}

	public long getPos()
	{
		return this.in.getPos();
	}

	public Token get()
	{
		SourceInputStream in = this.in;
		try
		{
			long pos = in.getPos();

			int b = in.read();
			if( b == -1 )
				return Token.EOF;

			int major = b >>> 5;
			int minor = b & 0x1F;

			if( minor == 31 )
			{
				switch( major )
				{
					case 2: return Token.IBSTRING;
					case 3: return Token.ITSTRING;
					case 4: return Token.IARRAY;
					case 5: return Token.IMAP;
					case 7: return Token.BREAK;
					default:
						throw new SourceException( "Unsupported additional info 31 for major type: " + major, SourceLocation.forBinary( in.getResource(), pos ) );
				}
			}

			switch( major )
			{
				case 0: return readUInt( in, pos, minor, TYPE.UINT );
				case 1: return readUInt( in, pos, minor, TYPE.NINT );
				case 2: return readUInt( in, pos, minor, TYPE.BYTES );
				case 3: return readUInt( in, pos, minor, TYPE.TEXT );
				case 4: return readUInt( in, pos, minor, TYPE.ARRAY );
				case 5: return readUInt( in, pos, minor, TYPE.MAP );
				case 6: return readUInt( in, pos, minor, TYPE.TAG );
				case 7: return readSimple( in, pos, minor, TYPE.TAG );
				default:
					throw new SourceException( "Unsupported major type: " + major, SourceLocation.forBinary( in.getResource(), pos ) );
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	public void readBytes( byte[] bytes )
	{
		SourceInputStream in = this.in;
		int remaining = bytes.length;
		try
		{
			int read = in.read( bytes );
			while( read < remaining )
			{
				if( read < 0 )
					throw new SourceException( "Unexpected EOF", SourceLocation.forBinary( in.getResource(), in.getPos() ) );
				if( read == 0 )
					throw new SourceException( "Zero bytes read", SourceLocation.forBinary( in.getResource(), in.getPos() ) );
				remaining -= read;
				read = in.read( bytes, bytes.length - remaining, remaining );
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	public String readString( int len )
	{
		byte[] result = new byte[ len ];
		readBytes( result );
		return new String( result );
	}

	static private Token readSimple( SourceInputStream in, long pos, int minor, TYPE type ) throws IOException
	{
		switch( minor )
		{
			case 20: return Token.FALSE;
			case 21: return Token.TRUE;
			case 22: return Token.NULL;
			case 23: return Token.UNDEF;
			case 25: throw new UnsupportedOperationException( "Half precision float not supported" );
			case 26: return new Token( TYPE.SFLOAT, Float.intBitsToFloat( readUInt4( in ) ) );
			case 27: return new Token( TYPE.DFLOAT, Double.longBitsToDouble( readUInt8( in ) ) );
			case 31: return Token.BREAK;
			default:
				throw new SourceException( "Unsupported additional info: " + minor, SourceLocation.forBinary( in.getResource(), pos ) );
		}
	}

	static private Token readUInt( SourceInputStream in, long pos, int minor, TYPE type ) throws IOException
	{
		return new Token( type, readUInt( in, pos, minor ) );
	}

	static private long readUInt( SourceInputStream in, long pos, int minor ) throws IOException
	{
		switch( minor )
		{
			case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
			case 10: case 11: case 12: case 13: case 14: case 15: case 16: case 17: case 18: case 19:
			case 20: case 21: case 22: case 23:
				return minor;
			case 24: return readUInt1( in );
			case 25: return readUInt2( in );
			case 26: return readUInt4( in );
			case 27: return readUInt8( in );
			default:
				throw new SourceException( "Unsupported additional info: " + minor, SourceLocation.forBinary( in.getResource(), pos ) );
		}
	}

	static private int readUInt1( SourceInputStream in ) throws IOException
	{
		int result = in.read();
		if( result == -1 )
			throw new SourceException( "Unexpected EOF", SourceLocation.forBinary( in.getResource(), in.getPos() ) );
		return result;
	}

	static private int readUInt2( SourceInputStream in ) throws IOException
	{
		return readUInt1( in ) << 8 | readUInt1( in );
	}

	static private int readUInt4( SourceInputStream in ) throws IOException
	{
		return readUInt2( in ) << 16 | readUInt2( in );
	}

	static private long readUInt8( SourceInputStream in ) throws IOException
	{
		return (long)readUInt4( in ) << 32 | readUInt4( in );
	}


	static public class Token
	{
		static public enum TYPE { UINT, NINT, BYTES, TEXT, ARRAY, MAP, IBYTES, ITEXT, IARRAY, IMAP, TAG, BOOL, NULL, UNDEF, HFLOAT, SFLOAT, DFLOAT, BREAK, EOF };

		static public Token EOF = new Token( TYPE.EOF );
		static public Token IBSTRING = new Token( TYPE.IBYTES );
		static public Token ITSTRING = new Token( TYPE.ITEXT );
		static public Token IARRAY = new Token( TYPE.IARRAY );
		static public Token IMAP = new Token( TYPE.IMAP );
		static public Token BREAK = new Token( TYPE.BREAK );
		static public Token FALSE = new Token( TYPE.BOOL, false );
		static public Token TRUE = new Token( TYPE.BOOL, true );
		static public Token NULL = new Token( TYPE.NULL );
		static public Token UNDEF = new Token( TYPE.UNDEF );

		private TYPE type;
		private long value;
		private boolean bool;
		private float flot;
		private double doble;

		public Token( TYPE type )
		{
			this.type = type;
		}

		public Token( TYPE type, long value )
		{
			this.type = type;
			this.value = value;
		}

		public Token( TYPE type, boolean value )
		{
			this.type = type;
			this.bool = value;
		}

		public Token( TYPE type, float value )
		{
			this.type = type;
			this.flot = value;
		}

		public Token( TYPE type, double value )
		{
			this.type = type;
			this.doble = value;
		}

		public TYPE type()
		{
			return this.type;
		}

		public long longValue()
		{
			return this.value;
		}

		public double doubleValue()
		{
			return this.doble;
		}

		public boolean booleanValue()
		{
			return this.bool;
		}

		public int length()
		{
			if( this.value < 0 || this.value > Integer.MAX_VALUE )
				throw new CBORException( "Invalid length: " + this.value );
			return (int)this.value;
		}

		@Override
		public String toString()
		{
			StringBuilder result = new StringBuilder( this.type.toString() );
			switch( this.type )
			{
				case UINT:
				case BYTES:
				case TEXT:
				case ARRAY:
				case MAP:
				case TAG:
					result.append( ' ' ); appendHex( result, this.value );
					break;
				case NINT:
					result.append( " -" ); appendHex( result, this.value ); result.append( "-1" );
					break;
				case BOOL:
					result.append( ' ' ).append( this.bool );
					break;
				case IBYTES:
				case ITEXT:
				case IARRAY:
				case IMAP:
				case NULL:
				case UNDEF:
				case BREAK:
				case EOF:
					break;
				case HFLOAT:
				case SFLOAT:
					result.append( ' ' ).append( Float.toString( this.flot ) );
					break;
				case DFLOAT:
					result.append( ' ' ).append( Double.toString( this.doble ) );
					break;
				default:
					throw new CBORException( "Unexpected type: " + this.type );
			}
			return result.toString();
		}

		private void appendHex( StringBuilder out, long value )
		{
			String hex = Long.toHexString( value ).toUpperCase();
			if( hex.length() % 2 == 1 )
				out.append( '0' );
			out.append( hex );
		}
	}
}
