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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Map;

import solidstack.io.DeferringWriter;
import solidstack.io.FatalIOException;
import solidstack.io.FileResource;
import solidstack.io.HexInputStreamReader;
import solidstack.io.Resource;
import solidstack.io.SourceException;
import solidstack.io.SourceLocation;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.json.JSONWriter;


// TODO BufferedOutputStreams?
public class JSONDataWriter implements RecordSink
{
	private Resource resource;
	private JSONWriter jsonWriter;
	private FileSpec[] fileSpecs;
	private Column[] columns;
	private FileSpec binaryFile;
	private boolean binaryGZip;
	private SourceLocation location;
	private Map<String, ColumnSpec> columnSpecs;


	public JSONDataWriter( Resource resource, OutputStream out, Map<String, ColumnSpec> columnSpecs, FileSpec binaryFile, boolean binaryGZip, SourceLocation location )
	{
		this.resource = resource;
		this.jsonWriter = new JSONWriter( out );
		this.columnSpecs = columnSpecs;
		this.binaryFile = binaryFile;
		this.binaryGZip = binaryGZip;
		this.location = location;
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

	@Override
	public void start()
	{
	}

	@Override
	public void process( Object[] record ) throws SQLException
	{
		try
		{
			int columns = record.length;

			JSONArray array = new JSONArray();
			for( int i = 0; i < columns; i++ )
			{
				Object value = record[ i ];
				if( value == null )
				{
					array.add( null );
					continue;
				}

				// TODO 2 columns can't be written to the same dynamic filename

				FileSpec spec = this.fileSpecs != null ? this.fileSpecs[ i ] : null;
				if( spec != null ) // The column is redirected to its own file
				{
					String relFileName = null;
					int startIndex;
					if( spec.binary )
					{
						if( spec.isParameterized() )
						{
							String fileName = spec.generateFileName();
							Resource fileResource = new FileResource( fileName );
							spec.out = fileResource.newOutputStream();
							spec.index = 0;
							relFileName = fileResource.getPathFrom( this.resource ).toString();
						}
						else if( spec.out == null )
						{
							String fileName = spec.generateFileName();
							Resource fileResource = new FileResource( fileName );
							spec.out = fileResource.newOutputStream();
						}
						if( value instanceof Blob )
						{
							InputStream in = ( (Blob)value ).getBinaryStream();
							startIndex = spec.index;
							byte[] buf = new byte[ 4096 ];
							for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
							{
								spec.out.write( buf, 0, read );
								spec.index += read;
							}
							in.close();
						}
						else if( value instanceof byte[] )
						{
							startIndex = spec.index;
							spec.out.write( (byte[])value );
							spec.index += ( (byte[])value ).length;
						}
						else
							throw new SourceException( this.columns[ i ].getName() + " (" + value.getClass().getName() + ") is not a binary column. Only binary columns like BLOB, RAW, BINARY VARYING can be written to a binary file", this.location );
						if( spec.isParameterized() )
						{
							spec.out.close();
							JSONObject ref = new JSONObject();
							ref.set( "file", relFileName );
							ref.set( "size", spec.index - startIndex );
							array.add( ref );
						}
						else
						{
							JSONObject ref = new JSONObject();
							ref.set( "index", startIndex );
							ref.set( "length", spec.index - startIndex );
							array.add( ref );
						}
					}
					else
					{
						if( spec.isParameterized() )
						{
							String fileName = spec.generateFileName();
							Resource fileResource = new FileResource( fileName );
							spec.writer = new DeferringWriter( spec.threshold, fileResource, this.jsonWriter.getEncoding() );
							spec.index = 0;
							relFileName = fileResource.getPathFrom( this.resource ).toString();
						}
						else if( spec.writer == null )
						{
							String fileName = spec.generateFileName();
							Resource fileResource = new FileResource( fileName );
							spec.writer = new OutputStreamWriter( fileResource.newOutputStream(), this.jsonWriter.getEncoding() );
						}
						if( value instanceof Blob || value instanceof byte[] )
							throw new SourceException( this.columns[ i ].getName() + " is a binary column. Binary columns like BLOB, RAW, BINARY VARYING cannot be written to a text file", this.location );
						if( value instanceof Clob )
						{
							Reader in = ( (Clob)value ).getCharacterStream();
							startIndex = spec.index;
							char[] buf = new char[ 4096 ];
							for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
							{
								spec.writer.write( buf, 0, read );
								spec.index += read;
							}
							in.close();
						}
						else
						{
							String val = value.toString();
							startIndex = spec.index;
							spec.writer.write( val );
							spec.index += val.length();
						}
						if( spec.isParameterized() )
						{
							DeferringWriter writer = (DeferringWriter)spec.writer;
							if( writer.isBuffered() )
								array.add( writer.clearBuffer() );
							else
							{
								JSONObject ref = new JSONObject();
								ref.set( "file", relFileName );
								ref.set( "size", spec.index - startIndex );
								array.add( ref );
							}
							writer.close();
						}
						else
						{
							JSONObject ref = new JSONObject();
							ref.set( "index", startIndex );
							ref.set( "length", spec.index - startIndex );
							array.add( ref );
						}
					}
				}
				else if( value instanceof Clob )
					array.add( ( (Clob)value ).getCharacterStream() );
				else if( this.binaryFile == null && value instanceof Blob )
					array.add( new HexInputStreamReader( ( (Blob)value ).getBinaryStream() ) );
				else if( this.binaryFile == null && value instanceof byte[] )
					array.add( new HexInputStreamReader( new ByteArrayInputStream( (byte[])value ) ) );
				else if( this.binaryFile != null && ( value instanceof Blob || value instanceof byte[] ) )
				{
					// TODO Exception when binaryFile is not set, or hexadecimal
					if( this.binaryFile.out == null )
					{
						String fileName = this.binaryFile.generateFileName();
						Resource fileResource = new FileResource( fileName ); // TODO use the factory
						fileResource.setGZip( this.binaryGZip );
						this.binaryFile.out = fileResource.newOutputStream(); // TODO Ctrl-C, close the outputstream?
					}
					int startIndex = this.binaryFile.index;
					if( value instanceof Blob )
					{
						InputStream in = ( (Blob)value ).getBinaryStream();
						byte[] buf = new byte[ 4096 ];
						for( int read = in.read( buf ); read >= 0; read = in.read( buf ) )
						{
							this.binaryFile.out.write( buf, 0, read );
							this.binaryFile.index += read;
						}
						in.close();
					}
					else
					{
						this.binaryFile.out.write( (byte[])value );
						this.binaryFile.index += ( (byte[])value ).length;
					}
					JSONObject ref = new JSONObject();
					ref.set( "index", startIndex );
					ref.set( "length", this.binaryFile.index - startIndex );
					array.add( ref );
				}
				else
					array.add( value );
			}

			for( ListIterator< Object > i = array.iterator(); i.hasNext(); )
			{
				Object value = i.next();
				if( value instanceof java.sql.Date || value instanceof java.sql.Time || value instanceof java.sql.Timestamp || value instanceof java.sql.RowId )
					i.set( value.toString() );
			}
			this.jsonWriter.write( array );
			this.jsonWriter.getWriter().write( '\n' );
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
		this.jsonWriter.close();
	}

	public JSONWriter getJSONWriter()
	{
		return this.jsonWriter;
	}
}
