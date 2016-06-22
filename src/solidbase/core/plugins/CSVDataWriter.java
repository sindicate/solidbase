/*--
 * Copyright 2015 René M. de Bloois
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

package solidbase.core.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import solidbase.util.CSVWriter;
import solidstack.io.FatalIOException;


public class CSVDataWriter implements RecordSink
{
	private CSVWriter csvWriter;
	private boolean writeHeader;


	public CSVDataWriter( Writer out, char separator, boolean writeHeader )
	{
		this.csvWriter = new CSVWriter( out, separator, false );
		this.writeHeader = writeHeader;
	}

	public void init( Column[] columns )
	{
		if( this.writeHeader )
		{
			for( Column column : columns )
				this.csvWriter.writeValue( column.getName() );
			this.csvWriter.nextRecord();
		}
	}

	@Override
	public void start()
	{
	}

	public void process( Object[] record ) throws SQLException
	{
		try
		{
			for( Object value : record )
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

			this.csvWriter.nextRecord();
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}
	}

	@Override
	public void end()
	{
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
