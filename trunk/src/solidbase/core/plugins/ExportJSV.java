/*--
 * Copyright 2011 René M. de Bloois
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.SystemException;
import solidbase.util.DeferringWriter;
import solidbase.util.FileResource;
import solidbase.util.JSONArray;
import solidbase.util.JSONObject;
import solidbase.util.JSONWriter;
import solidbase.util.JdbcSupport;
import solidbase.util.Resource;
import solidbase.util.StringLineReader;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author René M. de Bloois
 * @since Aug 12, 2011
 */
public class ExportJSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "EXPORT\\s+JSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		Parsed parsed = parse( command );

		Resource jsvResource = new FileResource( new File( parsed.fileName ) ); // Relative to current folder

		JSONWriter jsonWriter = new JSONWriter( jsvResource );
		try
		{
			try
			{
				Statement statement = processor.createStatement();
				try
				{
					ResultSet result = statement.executeQuery( parsed.query );

					ResultSetMetaData metaData = result.getMetaData();
					int count = metaData.getColumnCount();
					int[] types = new int[ count ];
					String[] names = new String[ count ];
					boolean[] coalesce = new boolean[ count ];
					FileSpec[] fileSpecs = new FileSpec[ count ];
					int firstCoalesce = -1;

					for( int i = 0; i < count; i++ )
					{
						String name = metaData.getColumnName( i + 1 ).toUpperCase();
						types[ i ] = metaData.getColumnType( i + 1 );
						names[ i ] = name;
						if( parsed.columns != null )
							fileSpecs[ i ] = parsed.columns.get( name );
						if( parsed.coalesce != null && parsed.coalesce.contains( name ) )
						{
							coalesce[ i ] = true;
							if( firstCoalesce < 0 )
								firstCoalesce = i;
						}
					}

					JSONObject properties = new JSONObject();
					properties.set( "version", "1.0" );
					properties.set( "createdBy", "SolidBase 2.0.0" );
					properties.set( "contentType", "text/json-values" );

					JSONArray fields = new JSONArray();
					properties.set( "fields", fields );
					for( int i = 0; i < count; i++ )
						if( !coalesce[ i ] || firstCoalesce == i )
						{
							JSONObject field = new JSONObject();
							field.set( "name", names[ i ] );
							field.set( "type", JdbcSupport.getTypeName( types[ i ] ) );
							FileSpec spec = fileSpecs[ i ];
							if( spec != null && !spec.generator.isDynamic() )
							{
								Resource fileResource = new FileResource( spec.generator.fileName );
								field.set( "file", fileResource.getPathFrom( jsvResource ) );
							}
							fields.add( field );
						}

					jsonWriter.writeProperties( properties );

					try
					{
						while( result.next() )
						{
							Object coalescedValue = null;
							if( parsed.coalesce != null )
								for( int i = 0; i < count; i++ )
									if( coalesce[ i ] )
									{
										coalescedValue = JdbcSupport.getValue( result, types, i );
										if( coalescedValue != null )
											break;
									}

							JSONArray array = new JSONArray();

							for( int i = 0; i < count; i++ )
							{
								if( !coalesce[ i ] || firstCoalesce == i )
								{
									Object value = coalescedValue;
									if( firstCoalesce != i )
										value = JdbcSupport.getValue( result, types, i );

									// TODO 2 columns can't be written to the same dynamic filename

									FileSpec spec = fileSpecs[ i ];
									if( spec != null ) // The column is redirected to its own file
									{
										String relFileName = null;
										int startIndex;
										if( spec.binary )
										{
											if( spec.generator.isDynamic() )
											{
												String fileName = spec.generator.generateFileName( result );
												Resource fileResource = new FileResource( fileName );
												spec.out = fileResource.getOutputStream();
												relFileName = fileResource.getPathFrom( jsvResource );
											}
											else
											{
												if( spec.out == null )
												{
													String fileName = spec.generator.generateFileName( result );
													Resource fileResource = new FileResource( fileName );
													spec.out = fileResource.getOutputStream();
												}
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
												throw new CommandFileException( names[ i ] + " is not a binary column. Only binary columns like BLOB, RAW, BINARY VARYING can be written to a binary file", command.getLocation() );
											if( spec.generator.isDynamic() )
											{
												spec.out.close();
												JSONObject ref = new JSONObject();
												ref.set( "file", relFileName );
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
												String fileName = spec.generator.generateFileName( result );
												Resource fileResource = new FileResource( fileName );
												spec.writer = new DeferringWriter( spec.threshold, fileResource, jsonWriter.getEncoding() );
												relFileName = fileResource.getPathFrom( jsvResource );
											}
											else
											{
												if( spec.writer == null )
												{
													String fileName = spec.generator.generateFileName( result );
													Resource fileResource = new FileResource( fileName );
													spec.writer = new OutputStreamWriter( fileResource.getOutputStream(), jsonWriter.getEncoding() );
												}
											}
											if( value instanceof Blob || value instanceof byte[] )
												throw new CommandFileException( names[ i ] + " is a binary column. Binary columns like BLOB, RAW, BINARY VARYING cannot be written to a text file", command.getLocation() );
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
												if( writer.isInMemory() )
													array.add( writer.getData() );
												else
												{
													JSONObject ref = new JSONObject();
													ref.set( "file", relFileName );
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
									else
									{
										if( value instanceof Clob )
											array.add( ( (Clob)value ).getCharacterStream() );
										else
											array.add( value );
									}
								}
							}

							jsonWriter.writeValues( array );
						}
					}
					finally
					{
						// Close files that have been left open
						for( FileSpec fileSpec : fileSpecs )
							if( fileSpec != null )
							{
								if( fileSpec.out != null )
									fileSpec.out.close();
								if( fileSpec.writer != null )
									fileSpec.writer.close();
							}
					}
				}
				finally
				{
					processor.closeStatement( statement, true );
				}
			}
			finally
			{
				jsonWriter.close();
			}
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		return true;
	}


	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	static protected Parsed parse( Command command )
	{
		Parsed result = new Parsed();

		Tokenizer tokenizer = new Tokenizer( new StringLineReader( command.getCommand(), command.getLocation() ) );

		tokenizer.get( "EXPORT" );
		tokenizer.get( "JSV" );

		Token t = tokenizer.get( "COALESCE", "FILE" );
		if( t.equals( "COALESCE" ) )
		{
			result.coalesce = new HashSet< String >();
			do
			{
				t = tokenizer.get();
				result.coalesce.add( t.getValue().toUpperCase() );
				t = tokenizer.get();
			}
			while( t.equals( "," ) );
			tokenizer.push( t );

			tokenizer.get( "FILE" );
		}

		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get();
		if( t.equals( "COLUMN" ) )
		{
			result.columns = new HashMap< String, FileSpec >();
			while( t.equals( "COLUMN" ) )
			{
				List< Token > columns = new ArrayList< Token >();
				columns.add( tokenizer.get() );
				t = tokenizer.get();
				while( t.equals( "," ) )
				{
					columns.add( tokenizer.get() );
					t = tokenizer.get();
				}
				tokenizer.push( t );

				tokenizer.get( "TO" );
				t = tokenizer.get( "BINARY", "TEXT" );
				boolean binary = t.equals( "BINARY" );
				tokenizer.get( "FILE" );
				t = tokenizer.get();
				String fileName = t.getValue();
				if( !fileName.startsWith( "\"" ) )
					throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
				fileName = fileName.substring( 1, fileName.length() - 1 );

				t = tokenizer.get();
				int threshold = 0;
				if( t.equals( "THRESHOLD" ) )
				{
					t = tokenizer.get();
					threshold = Integer.parseInt( t.getValue() );

					t = tokenizer.get();
				}

				FileSpec fileSpec = new FileSpec( binary, fileName, threshold );
				for( Token column : columns )
					result.columns.put( column.getValue().toUpperCase(), fileSpec );
			}
		}
		tokenizer.push( t );

		String query = tokenizer.getRemaining();

		result.fileName = file;
		result.query = query;

		return result;
	}


	//@Override
	public void terminate()
	{
		// Nothing to clean up
	}


	/**
	 * A parsed command.
	 *
	 * @author René M. de Bloois
	 */
	static protected class Parsed
	{
		/** The file path to export to */
		protected String fileName;

		/** The query */
		protected String query;

		/** Which columns need to be coalesced */
		protected Set< String > coalesce;

		protected Map< String, FileSpec > columns;
	}


	static protected class FileSpec
	{
		protected boolean binary;
		protected int threshold;

		protected FileNameGenerator generator;
		protected OutputStream out;
		protected Writer writer;
		protected int index;

		protected FileSpec( boolean binary, String fileName, int threshold )
		{
			this.binary = binary;
			this.threshold = threshold;
			this.generator = new FileNameGenerator( fileName );
		}
	}


	static protected class FileNameGenerator
	{
		protected final Pattern pattern = Pattern.compile( "\\?(\\d+)" );
		protected String fileName;
		protected boolean generic;

		protected FileNameGenerator( String fileName )
		{
			this.fileName = fileName;
			this.generic = this.pattern.matcher( fileName ).find();
		}

		protected boolean isDynamic()
		{
			return this.generic;
		}

		protected String generateFileName( ResultSet resultSet )
		{

			Matcher matcher = this.pattern.matcher( this.fileName );
			StringBuffer result = new StringBuffer();
			while( matcher.find() )
			{
				int index = Integer.parseInt( matcher.group( 1 ) );
				try
				{
					matcher.appendReplacement( result, resultSet.getString( index ) );
				}
				catch( SQLException e )
				{
					throw new SystemException( e );
				}
			}
			matcher.appendTail( result );
			return result.toString();
		}
	}
}
