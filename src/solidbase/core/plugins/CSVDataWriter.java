package solidbase.core.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import solidbase.core.SystemException;
import solidbase.util.CSVWriter;

public class CSVDataWriter implements DataProcessor
{
	private CSVWriter csvWriter;

	public CSVDataWriter( Writer out, char separator )
	{
		this.csvWriter = new CSVWriter( out, separator, false );
	}

	public void init( Column[] columns )
	{
	}

	public void process( Object[] values ) throws SQLException
	{
		try
		{
			int columns = values.length;

			for( int i = 0; i < columns; i++ )
			{
				Object value = values[ i ];
				if( value == null )
					this.csvWriter.writeValue( (String)null );
				else if( value instanceof Clob )
				{
					Reader in = ( (Clob)value ).getCharacterStream();
					this.csvWriter.writeValue( in );
					in.close();
				}
				else if( value instanceof Blob )
				{
					InputStream in = ( (Blob)value ).getBinaryStream();
					this.csvWriter.writeValue( in );
					in.close();
				}
				else if( value instanceof byte[] )
					this.csvWriter.writeValue( (byte[])value );
				else
					this.csvWriter.writeValue( value.toString() );
			}

			this.csvWriter.nextRecord();
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}

	public void close()
	{
		this.csvWriter.close();
	}

	public CSVWriter getCSVWriter()
	{
		return this.csvWriter;
	}
}
