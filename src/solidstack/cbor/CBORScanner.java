package solidstack.cbor;

import java.io.IOException;
import java.io.InputStream;

import solidstack.io.FatalIOException;

public class CBORScanner
{
	static public enum TYPE { UINT, NINT, BSTRING, TSTRING, ARRAY, MAP, IBSTRING, ITSTRING, IARRAY, IMAP, TAG, BOOL, NULL, UNDEF, HFLOAT, SFLOAT, DFLOAT, BREAK, EOF };

	private InputStream in;


	public CBORScanner( InputStream in )
	{
		this.in = in;
	}

	public Token get()
	{
		InputStream in = this.in;
		try
		{
			int b = in.read();
			if( b == -1 )
				return new Token( TYPE.EOF );

			int major = b >>> 5;
			int minor = b & 0x1F;

			if( minor == 31 )
			{
				switch( major )
				{
					case 2:
						return new Token( TYPE.IBSTRING );
					case 3:
						return new Token( TYPE.ITSTRING );
					case 4:
						return new Token( TYPE.IARRAY );
					case 5:
						return new Token( TYPE.IMAP );
					case 7:
						return new Token( TYPE.BREAK );
					default:
						throw new CBORException( "Unsupported additonal info 31 for major: " + major );
				}
			}

			switch( major )
			{
				case 0:
					return readUInt( in, minor, TYPE.UINT );
				case 1:
					return readUInt( in, minor, TYPE.NINT );
				case 2:
					return readUInt( in, minor, TYPE.BSTRING );
				case 3:
					return readUInt( in, minor, TYPE.TSTRING );
				case 4:
					return readUInt( in, minor, TYPE.ARRAY );
				case 5:
					return readUInt( in, minor, TYPE.MAP );
				case 6:
					return readUInt( in, minor, TYPE.TAG );
				case 7:
					return readSimple( in, minor, TYPE.TAG );
				default:
					throw new CBORException( "Unsupported major type: " + major );
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	public void readBytes( byte[] bytes )
	{
		try
		{
			int read = this.in.read( bytes );
			if( read != bytes.length )
				throw new CBORException( "Not enough bytes read" );
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

	static private Token readSimple( InputStream in, int minor, TYPE type ) throws IOException
	{
		switch( minor )
		{
			case 20:
				return new Token( TYPE.BOOL, false );
			case 21:
				return new Token( TYPE.BOOL, true );
			case 22:
				return new Token( TYPE.NULL );
			case 23:
				return new Token( TYPE.UNDEF );
			case 25:
				//return new Token( TYPE.HFLOAT );
				throw new UnsupportedOperationException( "Half precision float not supported" );
			case 26:
				return new Token( TYPE.SFLOAT, Float.intBitsToFloat( readUInt4( in ) ) );
			case 27:
				return new Token( TYPE.DFLOAT, Double.longBitsToDouble( readUInt8( in ) ) );
			case 31:
				return new Token( TYPE.BREAK );
			default:
				throw new CBORException( "Unsupported additional info: " + minor );
		}
	}

	static private Token readUInt( InputStream in, int minor, TYPE type ) throws IOException
	{
		if( minor == 31 )
			new Token( type, -1 );
		return new Token( type, readUInt( in, minor ) );
	}

	static private long readUInt( InputStream in, int minor ) throws IOException
	{
		switch( minor )
		{
			case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
			case 10: case 11: case 12: case 13: case 14: case 15: case 16: case 17: case 18: case 19:
			case 20: case 21: case 22: case 23:
				return minor;
			case 24:
				return readUInt1( in );
			case 25:
				return readUInt2( in );
			case 26:
				return readUInt4( in );
			case 27:
				return readUInt8( in );
			default:
				throw new CBORException( "Unsupported additional info: " + minor );
		}
	}

	static private int readUInt1( InputStream in ) throws IOException
	{
		int result = in.read();
		if( result == -1 )
			throw new CBORException( "Unexpected EOF" );
		return result;
	}

	static private int readUInt2( InputStream in ) throws IOException
	{
		return readUInt1( in ) << 8 | readUInt1( in );
	}

	static private int readUInt4( InputStream in ) throws IOException
	{
		return readUInt2( in ) << 16 | readUInt2( in );
	}

	static private long readUInt8( InputStream in ) throws IOException
	{
		return (long)readUInt4( in ) << 32 | readUInt4( in );
	}

	static public class Token
	{
		private TYPE type;
		private long value;
		private boolean bool;
		private float flot;
		private double doble;

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

		public Token( TYPE type )
		{
			this.type = type;
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

		public TYPE getType()
		{
			return this.type;
		}

		public long getValue()
		{
			return this.value;
		}

		public int getLength()
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
				case BSTRING:
				case TSTRING:
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
				case IBSTRING:
				case ITSTRING:
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
//			if( hex.length() == 1 )
//				out.append( '0' );
			out.append( hex );
		}
	}
}
