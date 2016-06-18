package solidstack.cbor;

import java.io.IOException;

import solidstack.cbor.Token.TYPE;
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

	public void close()
	{
		this.in.close();
	}

	public Resource getResource()
	{
		return this.in.getResource();
	}

	public long getPos()
	{
		return this.in.getPos();
	}

	public SimpleToken get()
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
		return new String( result, CBORWriter.UTF8 );
	}

	static private SimpleToken readSimple( SourceInputStream in, long pos, int minor, TYPE type ) throws IOException
	{
		switch( minor )
		{
			case 20: return Token.FALSE;
			case 21: return Token.TRUE;
			case 22: return Token.NULL;
			case 23: return Token.UNDEF;
			case 25: throw new UnsupportedOperationException( "Half precision float not supported" );
			case 26: return SimpleToken.forFloatS( Float.intBitsToFloat( readUInt4( in ) ) );
			case 27: return SimpleToken.forFloatD( Double.longBitsToDouble( readUInt8( in ) ) );
			case 31: return Token.BREAK;
			default:
				throw new SourceException( "Unsupported additional info: " + minor, SourceLocation.forBinary( in.getResource(), pos ) );
		}
	}

	static private SimpleToken readUInt( SourceInputStream in, long pos, int minor, TYPE type ) throws IOException
	{
		return SimpleToken.forType( type, readUInt( in, pos, minor ) );
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
}
