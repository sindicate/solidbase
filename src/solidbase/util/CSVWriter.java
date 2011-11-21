package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.regex.Pattern;

import solidbase.core.SystemException;

public class CSVWriter
{
	static private final char[] HEX = "0123456789ABCDEF".toCharArray();

	private Writer out;
	private char separator;
	private Pattern needQuotesPattern;
	private boolean valueWritten;

	public CSVWriter( Resource resource, String encoding, char separator ) throws UnsupportedEncodingException
	{
		this.out = new OutputStreamWriter( resource.getOutputStream(), encoding );
		this.separator = separator;
		// Pattern: ", CR, NL or parsed.separator
		this.needQuotesPattern = Pattern.compile( "\"|\r|\n|" + Pattern.quote( Character.toString( separator ) ) );
	}

	public void writeValue( String value )
	{
		writeValue0();
		if( value == null )
			return;
		try
		{
			boolean needQuotes = this.needQuotesPattern.matcher( value ).find();
			if( needQuotes )
			{
				this.out.write( '"' );
				int len = value.length();
				for( int i = 0; i < len; i++ )
				{
					char c = value.charAt( i );
					if( c == '"' )
						this.out.write( c );
					this.out.write( c );
				}
				this.out.write( '"' );
			}
			else
				this.out.write( value );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void writeValue( Reader reader )
	{
		writeValue0();
		try
		{
			this.out.write( '"' );
			char[] buf = new char[ 4096 ];
			for( int read = reader.read( buf ); read >= 0; read = reader.read( buf ) )
				this.out.write( new String( buf, 0, read ).replace( "\"", "\"\"" ) );
			this.out.write( '"' );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void writeValue( InputStream in )
	{
		writeValue0();
		try
		{
			byte[] buf = new byte[ 4096 ];
			for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
			{
				for( int j = 0; j < read; j++ )
				{
					int b = buf[ j ];
					this.out.write( HEX[ ( b >> 4 ) & 15 ] );
					this.out.write( HEX[ b & 15 ] );
				}
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void writeValue( byte[] value )
	{
		writeValue0();
		if( value == null )
			return;
		try
		{
			for( int b : value )
			{
				this.out.write( HEX[ ( b >> 4 ) & 15 ] );
				this.out.write( HEX[ b & 15 ] );
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void nextValue()
	{
		try
		{
			this.out.write( this.separator );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	private void writeValue0()
	{
		if( this.valueWritten )
			nextValue();
		this.valueWritten = true;
	}

	public void nextRecord()
	{
		this.valueWritten = false;
		try
		{
			this.out.write( '\n' );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
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
}
