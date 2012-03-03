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
import java.io.UnsupportedEncodingException;
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
import solidbase.util.CSVWriter;
import solidbase.util.JDBCSupport;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;
import solidstack.io.DeferringWriter;
import solidstack.io.FileResource;
import solidstack.io.Resource;
import solidstack.io.StringLineReader;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author René M. de Bloois
 * @since Aug 12, 2011
 */
public class ExportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "EXPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		Parsed parsed = parse( command );

		Resource csvResource = new FileResource( new File( parsed.fileName ) ); // Relative to current folder

		CSVWriter csvWriter;
		try
		{
			boolean extendedFormat = parsed.columns != null;
			csvWriter = new CSVWriter( csvResource, parsed.encoding, parsed.separator, extendedFormat );
		}
		catch( UnsupportedEncodingException e )
		{
			// toString() instead of getMessage(), the getMessage only returns the encoding string
			throw new CommandFileException( e.toString(), command.getLocation() );
		}
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

					if( parsed.withHeader )
					{
						for( int i = 0; i < count; i++ )
							if( !coalesce[ i ] || firstCoalesce == i )
								csvWriter.writeValue( names[ i ] );
						csvWriter.nextRecord();
					}

					try
					{
						while( result.next() )
						{
							Object coalescedValue = null;
							if( parsed.coalesce != null )
								for( int i = 0; i < count; i++ )
									if( coalesce[ i ] )
									{
										coalescedValue = JDBCSupport.getValue( result, types, i );
										if( coalescedValue != null )
											break;
									}

							for( int i = 0; i < count; i++ )
							{
								if( !coalesce[ i ] || firstCoalesce == i )
								{
									Object value = coalescedValue;
									if( firstCoalesce != i )
										value = JDBCSupport.getValue( result, types, i );

									// TODO Write null as ^NULL in extended format?
									if( value == null )
									{
										csvWriter.writeValue( (String)null );
										continue;
									}

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
												relFileName = fileResource.getPathFrom( csvResource );
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
												csvWriter.writeExtendedValue( "BIN(FILE=\"" + relFileName + "\")" ); // TODO Escape filename (Linux filenames are allowed to have double quotes)
											}
											else
												csvWriter.writeExtendedValue( "BIN(INDEX=" + startIndex + ",LENGTH=" + ( spec.index - startIndex ) + ")" );
										}
										else
										{
											if( spec.generator.isDynamic() )
											{
												String fileName = spec.generator.generateFileName( result );
												Resource fileResource = new FileResource( fileName );
												spec.writer = new DeferringWriter( spec.threshold, fileResource, parsed.encoding );
												relFileName = fileResource.getPathFrom( csvResource );
											}
											else
											{
												if( spec.writer == null )
												{
													String fileName = spec.generator.generateFileName( result );
													Resource fileResource = new FileResource( fileName );
													spec.writer = new OutputStreamWriter( fileResource.getOutputStream(), parsed.encoding );
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
													csvWriter.writeValue( writer.getData() );
												else
													csvWriter.writeExtendedValue( "TXT(FILE=\"" + relFileName + "\",ENCODING=\"" + parsed.encoding + "\")" ); // TODO Escape filename (Linux filenames are allowed to have double quotes)
												writer.close();
											}
											else
												csvWriter.writeExtendedValue( "TXT(INDEX=" + startIndex + ",LENGTH=" + ( spec.index - startIndex ) + ")" );
										}
									}
									else
									{
										if( value instanceof Clob )
										{
											Reader in = ( (Clob)value ).getCharacterStream();
											csvWriter.writeValue( in );
											in.close();
										}
										else if( value instanceof Blob )
										{
											InputStream in = ( (Blob)value ).getBinaryStream();
											csvWriter.writeValue( in );
											in.close();
										}
										else if( value instanceof byte[] )
										{
											csvWriter.writeValue( (byte[])value );
										}
										else
											csvWriter.writeValue( value.toString() );
									}
								}
							}

							csvWriter.nextRecord();
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
				csvWriter.close();
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
		tokenizer.get( "CSV" );

		Token t = tokenizer.get( "WITH", "SEPARATED", "COALESCE", "FILE" );
		if( t.eq( "WITH" ) )
		{
			tokenizer.get( "HEADER" );
			result.withHeader = true;

			t = tokenizer.get( "SEPARATED", "COALESCE", "FILE" );
		}

		if( t.eq( "SEPARATED" ) )
		{
			tokenizer.get( "BY" );
			t = tokenizer.get();
			if( t.eq( "TAB" ) )
				result.separator = '\t';
			else
			{
				if( t.length() != 1 )
					throw new CommandFileException( "Expecting [TAB] or one character, not [" + t + "]", tokenizer.getLocation() );
				result.separator = t.getValue().charAt( 0 );
			}

			t = tokenizer.get( "COALESCE", "FILE" );
		}

		if( t.eq( "COALESCE" ) )
		{
			result.coalesce = new HashSet< String >();
			do
			{
				t = tokenizer.get();
				result.coalesce.add( t.getValue().toUpperCase() );
				t = tokenizer.get();
			}
			while( t.eq( "," ) );
			tokenizer.push( t );

			tokenizer.get( "FILE" );
		}

		t = tokenizer.get();
		String file = t.getValue();
		if( !file.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		file = file.substring( 1, file.length() - 1 );

		t = tokenizer.get( "ENCODING" );
		t = tokenizer.get();
		String encoding = t.getValue();
		if( !encoding.startsWith( "\"" ) )
			throw new CommandFileException( "Expecting encoding enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
		encoding = encoding.substring( 1, encoding.length() - 1 );

		t = tokenizer.get();
		if( t.eq( "COLUMN" ) )
		{
			result.columns = new HashMap< String, FileSpec >();
			while( t.eq( "COLUMN" ) )
			{
				List< Token > columns = new ArrayList< Token >();
				columns.add( tokenizer.get() );
				t = tokenizer.get();
				while( t.eq( "," ) )
				{
					columns.add( tokenizer.get() );
					t = tokenizer.get();
				}
				tokenizer.push( t );

				tokenizer.get( "TO" );
				t = tokenizer.get( "BINARY", "TEXT" );
				boolean binary = t.eq( "BINARY" );
				tokenizer.get( "FILE" );
				t = tokenizer.get();
				String fileName = t.getValue();
				if( !fileName.startsWith( "\"" ) )
					throw new CommandFileException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
				fileName = fileName.substring( 1, fileName.length() - 1 );

				t = tokenizer.get();
				int threshold = 0;
				if( t.eq( "THRESHOLD" ) )
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
		result.encoding = encoding;
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
		protected boolean withHeader;

		/** The separator. */
		protected char separator = ',';

		/** The file path to export to */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;

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
