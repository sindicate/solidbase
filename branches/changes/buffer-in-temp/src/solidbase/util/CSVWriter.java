package solidbase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.regex.Pattern;

import solidbase.core.SystemException;
import solidstack.io.Resource;

public class CSVWriter
{
	static private final char[] HEX = "0123456789ABCDEF".toCharArray();
	static private final String HEX_ENCODING = "^HEX:"; // TODO Final decision

	private Writer out;
	private char separator;
	private Pattern needQuotesPattern;
	private boolean extendedFormat;
	private boolean valueWritten;

	public CSVWriter( Resource resource, String encoding, char separator, boolean extendedFormat ) throws UnsupportedEncodingException
	{
		Assert.isFalse( separator == '"', "Double quote (\") not allowed as value separator" );
		if( extendedFormat )
			Assert.isFalse( separator == '^', "Caret (^) not allowed as value separator when extended format is enabled" );

		this.out = new OutputStreamWriter( resource.getOutputStream(), encoding );
		this.separator = separator;
		this.extendedFormat = extendedFormat;

		// Pattern: ", CR, NL or parsed.separator, or ^ when extended format is enabled
		this.needQuotesPattern = Pattern.compile( "\"|\r|\n|" + Pattern.quote( Character.toString( separator ) ) + ( extendedFormat ? "|^\\^" : "" ) );
	}

	public void writeValue( String value )
	{
		writeSeparatorIfNeeded();
		internalWriteValue( value );
	}

	private void internalWriteValue( String value )
	{
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
		writeSeparatorIfNeeded();
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
		writeSeparatorIfNeeded();
		try
		{
			if( this.extendedFormat )
				this.out.write( HEX_ENCODING );

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
		writeSeparatorIfNeeded();
		if( value == null )
			return;
		try
		{
			if( this.extendedFormat )
				this.out.write( HEX_ENCODING );

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

	private void nextValue()
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

	private void writeSeparatorIfNeeded()
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

	public void writeExtendedValue( String value )
	{
		Assert.notNull( value );
		Assert.isTrue( this.extendedFormat );

		writeSeparatorIfNeeded();
		try
		{
			this.out.write( '^' );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
		internalWriteValue( value );
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
