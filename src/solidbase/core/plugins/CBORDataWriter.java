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
import java.sql.SQLException;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

import solidstack.cbor.CBORWriter;


public class CBORDataWriter implements RecordSink
{
	static public final int MAX_DICTIONARY_SIZE = 0x4000000;

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

	@Override
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

	@Override
	public void start()
	{
		this.out.tagSlidingRefNS( 10000, 0x1000 );
		this.out.startArray();
	}

	static public enum Types { BIGDECIMAL, BLOB, BOOLEAN, BYTES, CLOB, DATE, DOUBLE, FLOAT, INTEGER, LONG, STRING, SQLDATE, SQLTIME, SQLTIMESTAMP }

	static public Map<Class<?>, Types> types;
	static
	{
		types = new IdentityHashMap<>();
		types.put( BigDecimal.class, Types.BIGDECIMAL );
		types.put( Boolean.class, Types.BOOLEAN );
		types.put( byte[].class, Types.BYTES );
		types.put( Date.class, Types.DATE );
		types.put( Double.class, Types.DOUBLE );
		types.put( Float.class, Types.FLOAT );
		types.put( Integer.class, Types.INTEGER );
		types.put( Long.class, Types.LONG );
		types.put( String.class, Types.STRING );
		types.put( java.sql.Date.class, Types.SQLDATE );
		types.put( java.sql.Time.class, Types.SQLTIME );
		types.put( java.sql.Timestamp.class, Types.SQLTIMESTAMP );
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		CBORWriter out = this.out;

		int columns = record.length;
		if( this.columns.length != columns )
			throw new IllegalStateException( "Column count mismatch" );

		out.startArray( columns );

		// TODO Use map and switch for types
		for( int i = 0; i < columns; i++ )
		{
			Object value = record[ i ];
			if( value == null )
				out.writeNull();
			else
			{
				// TODO RowId? UUID?
				Types type = types.get( value.getClass() );
				if( type == null )
					if( value instanceof Blob )
						type = Types.BLOB;
					else if( value instanceof Clob )
						type = Types.CLOB;
					else
						throw new UnsupportedOperationException( "Type not supported: " + value.getClass().getName() );
				switch( type )
				{
					// TODO Need to close these?
					case BIGDECIMAL: out.writeText( value.toString() ); break; // TODO Is there another CBOR type (tag) ?
					case BLOB: out.writeBytes( ( (Blob)value ).getBinaryStream() ); break;
					case BOOLEAN: out.writeBoolean( (Boolean)value ); break;
					case BYTES: out.writeBytes( (byte[])value ); break;
					case CLOB: out.writeText( ( (Clob)value ).getCharacterStream() ); break;
					case DOUBLE: out.writeFloatD( (Double)value ); break;
					case FLOAT: out.writeFloatS( (Float)value ); break;
					case INTEGER: out.writeInt( (Integer)value ); break;
					case LONG: out.writeLong( (Long)value ); break;
					case STRING: out.writeText( (String)value ); break;
					case SQLDATE:
					case SQLTIME:
					case SQLTIMESTAMP: out.writeDateTime( (Date)value ); break; // FIXME We need to write string because of the timezone
					default:
						throw new UnsupportedOperationException( "Unexpected type: " + type );
				}
			}
		}

		out.end();
	}

	@Override
	public void end()
	{
		this.out.end();
	}
}
