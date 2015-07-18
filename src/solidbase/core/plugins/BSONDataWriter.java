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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Map;

import solidstack.bson.BSONWriter;
import solidstack.io.Resource;
import solidstack.io.SourceLocation;
import solidstack.json.JSONArray;

public class BSONDataWriter implements DataProcessor
{
	private Resource resource;
	private BSONWriter bsonWriter;
	private FileSpec[] fileSpecs;
	private Column[] columns;
	private SourceLocation location;
	private Map<String, ColumnSpec> columnSpecs;

	public BSONDataWriter( Resource resource, OutputStream out, Map<String, ColumnSpec> columnSpecs, SourceLocation location )
	{
		this.resource = resource;
		this.bsonWriter = new BSONWriter( out );
		this.columnSpecs = columnSpecs;
		this.location = location;
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

	public void process( Object[] values ) throws SQLException
	{
		int columns = values.length;

		JSONArray array = new JSONArray();
		for( int i = 0; i < columns; i++ )
		{
			Object value = values[ i ];
			if( value == null )
				array.add( null );
			else if( value instanceof Clob )
				array.add( ( (Clob)value ).getCharacterStream() );
			else if( value instanceof Blob )
				array.add( ( (Blob)value ).getBinaryStream() );
			else
				array.add( value );
		}

//			for( ListIterator< Object > i = array.iterator(); i.hasNext(); )
//			{
//				Object value = i.next();
//				if( value instanceof java.sql.Date || value instanceof java.sql.Time || value instanceof java.sql.Timestamp || value instanceof java.sql.RowId )
//					i.set( value.toString() );
//			}

		this.bsonWriter.write( array );
	}

	public void close()
	{
		this.bsonWriter.close();
	}

	public BSONWriter getBSONWriter()
	{
		return this.bsonWriter;
	}
}
