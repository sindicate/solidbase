package solidbase.core.plugins;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.zip.GZIPOutputStream;

import solidbase.core.SourceException;
import solidbase.core.SystemException;
import solidbase.core.plugins.DumpJSON.FileSpec;
import solidbase.util.JSONArray;
import solidbase.util.JSONObject;
import solidbase.util.JSONWriter;
import solidstack.io.DeferringWriter;
import solidstack.io.FileResource;
import solidstack.io.Resource;
import solidstack.io.SourceLocation;

public class JSONDataWriter extends DataProcessor
{
	private Resource resource;
	private JSONWriter jsonWriter;
	private FileSpec[] fileSpecs;
	private String[] names;
	private FileSpec binaryFile;
	private boolean binaryGZip;
	private SourceLocation location;

	public JSONDataWriter( Resource resource, OutputStream out, FileSpec[] fileSpecs, String[] names, FileSpec binaryFile, boolean binaryGZip, SourceLocation location )
	{
		this.resource = resource;
		this.jsonWriter = new JSONWriter( out );
		this.fileSpecs = fileSpecs;
		this.names = names;
		this.binaryFile = binaryFile;
		this.binaryGZip = binaryGZip;
		this.location = location;
	}

	@Override
	public void process( Object[] values ) throws SQLException
	{
		try
		{
			int columns = values.length;

			JSONArray array = new JSONArray();
			for( int i = 0; i < columns; i++ )
			{
				Object value = values[ i ];
				if( value == null )
				{
					array.add( null );
					continue;
				}

				// TODO 2 columns can't be written to the same dynamic filename

				FileSpec spec = this.fileSpecs[ i ];
				if( spec != null ) // The column is redirected to its own file
				{
					String relFileName = null;
					int startIndex;
					if( spec.binary )
					{
						if( spec.generator.isDynamic() )
						{
							String fileName = spec.generator.generateFileName( values );
							Resource fileResource = new FileResource( fileName );
							spec.out = fileResource.getOutputStream();
							spec.index = 0;
							relFileName = fileResource.getPathFrom( this.resource ).toString();
						}
						else if( spec.out == null )
						{
							String fileName = spec.generator.generateFileName( values );
							Resource fileResource = new FileResource( fileName );
							spec.out = fileResource.getOutputStream();
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
							throw new SourceException( this.names[ i ] + " (" + value.getClass().getName() + ") is not a binary column. Only binary columns like BLOB, RAW, BINARY VARYING can be written to a binary file", this.location );
						if( spec.generator.isDynamic() )
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
						if( spec.generator.isDynamic() )
						{
							String fileName = spec.generator.generateFileName( values );
							Resource fileResource = new FileResource( fileName );
							spec.writer = new DeferringWriter( spec.threshold, fileResource, this.jsonWriter.getEncoding() );
							spec.index = 0;
							relFileName = fileResource.getPathFrom( this.resource ).toString();
						}
						else if( spec.writer == null )
						{
							String fileName = spec.generator.generateFileName( values );
							Resource fileResource = new FileResource( fileName );
							spec.writer = new OutputStreamWriter( fileResource.getOutputStream(), this.jsonWriter.getEncoding() );
						}
						if( value instanceof Blob || value instanceof byte[] )
							throw new SourceException( this.names[ i ] + " is a binary column. Binary columns like BLOB, RAW, BINARY VARYING cannot be written to a text file", this.location );
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
						if( spec.generator.isDynamic() )
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
				else if( this.binaryFile != null && ( value instanceof Blob || value instanceof byte[] ) )
				{
					if( this.binaryFile.out == null )
					{
						String fileName = this.binaryFile.generator.generateFileName( null );
						Resource fileResource = new FileResource( fileName );
						this.binaryFile.out = fileResource.getOutputStream();
						if( this.binaryGZip )
							this.binaryFile.out = new BufferedOutputStream( new GZIPOutputStream( this.binaryFile.out, 65536 ), 65536 ); // TODO Ctrl-C, close the outputstream?
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
			throw new SystemException( e );
		}
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
