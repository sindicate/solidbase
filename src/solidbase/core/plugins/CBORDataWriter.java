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

import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.RowId;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import solidstack.cbor.CBORWriter;


public class CBORDataWriter implements RecordSink
{
	private CBORWriter out;
	private Map<String, ColumnSpec> columnSpecs;

	private FileSpec[] fileSpecs;
	private Column[] columns;


	public CBORDataWriter( OutputStream out, Map<String, ColumnSpec> columnSpecs )
	{
		this.out = new CBORWriter( out );
		this.columnSpecs = columnSpecs;
	}

	public CBORWriter getCBOROutputStream()
	{
		return this.out;
	}

	public void init( Column[] columns )
	{
		this.columns = columns;

		if( this.columnSpecs != null )
		{
			this.fileSpecs = new FileSpec[ columns.length ];
			for( int i = 0; i < columns.length; i++ )
				{
					ColumnSpec columnSpec = this.columnSpecs.get( columns[ i ].getName() );
					if( columnSpec != null )
						this.fileSpecs[ i ] = columnSpec.toFile;
				}
		}
	}

	public void close()
	{
		this.out.close();
	}

	public void process( Object[] record ) throws SQLException
	{
		int columns = record.length;
		if( this.columns.length != columns )
			throw new IllegalStateException( "Column count mismatch" );

		CBORWriter out = this.out;

		out.startArray( columns );

		// TODO Stringrefs & global array
		for( int i = 0; i < columns; i++ )
		{
			Object value = record[ i ];
			if( value == null )
				out.writeNull();
			else if( value instanceof Clob )
				out.writeText( ( (Clob)value ).getCharacterStream() ); // TODO Need to close these?
			else if( value instanceof Blob )
				out.writeBytes( ( (Blob)value ).getBinaryStream() ); // TODO Need to close these?
			else if( value instanceof java.sql.Date || value instanceof java.sql.Time || value instanceof java.sql.Timestamp )
				out.writeDateTime( (Date)value ); // FIXME We need to write string because of the timezone
			else if( value instanceof RowId )
				out.writeBytes( ( (RowId)value ).getBytes() ); // TODO Need a tag for this?
			else if( value instanceof Integer )
			{
				int v = (Integer)value;
				if( v < 0 )
					out.writeIntN( -( v + 1 ) );
				else
					out.writeIntU( v );
			}
			else if( value instanceof Long )
			{
				long v = (Long)value;
				if( v < 0 )
					out.writeIntN( -( v + 1 ) );
				else
					out.writeIntU( v );
			}
			else if( value instanceof Float )
				out.writeFloatS( (Float)value );
			else if( value instanceof Double )
				out.writeFloatD( (Double)value );
			else if( value instanceof BigDecimal )
				out.writeText( value.toString() ); // TODO Is there another CBOR type (tag) ?
			else if( value instanceof Boolean )
				out.writeBoolean( (Boolean)value );
			else if( value instanceof String )
				out.writeText( (String)value );
			else if( value instanceof byte[] )
				out.writeBytes( (byte[])value );
			else
				throw new UnsupportedOperationException( "Type not supported: " + value.getClass().getName() );
		}

		out.end();
	}
}
