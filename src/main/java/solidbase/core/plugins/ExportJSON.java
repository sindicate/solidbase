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
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import funny.Symbol;
import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.FatalException;
import solidbase.core.ProcessException;
import solidbase.util.FixedIntervalLogCounter;
import solidbase.util.LogCounter;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidbase.util.TimeIntervalLogCounter;
import solidstack.io.FatalIOException;
import solidstack.io.FileResource;
import solidstack.io.Resource;
import solidstack.io.Resources;
import solidstack.io.SourceException;
import solidstack.io.SourceReaders;
import solidstack.json.JSONArray;
import solidstack.json.JSONObject;
import solidstack.script.scopes.Scope;
import solidstack.script.scopes.UndefinedException;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author René M. de Bloois
 * @since Aug 12, 2011
 */
public class ExportJSON implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*EXPORT\\s+JSON\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	// TODO Escape dynamic file names, because illegal characters may be generated
	// TODO Export multiple tables to a single file. If no PK than sort on all columns. Schema name for import or not?
	// TODO COLUMN TO TEXT FILE with ENCODING
	@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		if( command.isAnnotation() )
		{
			/* --* EXPORT JSON SET ADD_CREATED_DATE = ON | OFF */

			SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

			// TODO What about other configuration settings?
			Token t = tokenizer.skip( "EXPORT" ).skip( "JSON" ).skip( "SET" ).skip( "ADD_CREATED_DATE" ).skip( "=" ).get( "ON", "OFF" );
			tokenizer.skip( (String)null );

			// TODO I think we should have a scope that is restricted to the current file and a scope that gets inherited when running or including another file.
			Scope scope = processor.getContext().getScope();
			scope.setOrVar( Symbol.apply( "solidbase.export.json.addCreatedDate" ), t.eq( "ON" ) );

			return true;
		}

		if( skip )
			return true;

		Parsed parsed = parse( command );

		Scope scope = processor.getContext().getScope();
		boolean createdDate;
		try
		{
			Object object = scope.get( Symbol.apply( "solidbase.export.json.addCreatedDate" ) );
			createdDate = object instanceof Boolean && (Boolean)object;
		}
		catch( UndefinedException e )
		{
			createdDate = true;
		}

		Resource jsonOutput = new FileResource( new File( parsed.fileName ) ); // Relative to current folder
		jsonOutput.setGZip( parsed.gzip );
		try
		{
			OutputStream out = jsonOutput.newOutputStream();
			try
			{
				Statement statement = processor.createStatement();
				try
				{
					statement.setFetchSize( 1000 );
					ResultSet result;
					try
					{
						result = statement.executeQuery( parsed.query );
					}
					catch( SQLException e )
					{
						throw new ProcessException( e ).addProcess( "executing: " + parsed.query );
					}

					LogCounter counter = null;
					if( parsed.logRecords > 0 )
						counter = new FixedIntervalLogCounter( parsed.logRecords );
					else if( parsed.logSeconds > 0 )
						counter = new TimeIntervalLogCounter( parsed.logSeconds );

					DBReader reader = new DBReader( result, counter != null ? new ExportLogger( counter, processor.getProgressListener() ) : null, parsed.dateAsTimestamp );
					DefaultFromJDBCTransformer trans = new DefaultFromJDBCTransformer();
					RecordSource source = trans;
					reader.setSink( trans );

					Column[] columns = reader.getColumns();
					int count = columns.length;

					FileSpec[] fileSpecs = new FileSpec[ count ];

					// Analyze columns

					SelectProcessor selector = new SelectProcessor();
					if( parsed.columns != null )
						for( Entry<String, ColumnSpec> entry : parsed.columns.entrySet() )
							if( entry.getValue().skip )
								selector.deselect( entry.getKey() );
					for( Column column : columns )
					{
						int type = column.getType();
						// TODO STRUCT serialize
						// TODO This must be optional and not the default
						if( type == 2002 || column.getTypeName() == null )
							selector.deselect( column.getName() );
					}

					if( parsed.columns != null )
						for( int i = 0; i < count; i++ )
						{
							ColumnSpec columnSpec = parsed.columns.get( columns[ i ].getName() );
							if( columnSpec != null )
								fileSpecs[ i ] = columnSpec.toFile;
						}

					// Connect FileSpecs with the DBReader for the original values
					for( FileSpec fileSpec : fileSpecs )
						if( fileSpec != null )
							fileSpec.setSource( trans );

					if( parsed.coalesce != null )
					{
						CoalescerProcessor coalescer = new CoalescerProcessor( parsed.coalesce );
						source.setSink( coalescer );
						source = coalescer;
					}

					if( selector.hasDeselected() )
					{
						source.setSink( selector );
						source = selector;
					}

					FileSpec binaryFile = parsed.binaryFileName != null ? new FileSpec( true, parsed.binaryFileName, 0, trans ) : null;
					JSONDataWriter dataWriter = new JSONDataWriter( jsonOutput, out, parsed.columns, binaryFile, parsed.binaryGzip, command.getLocation() );
					try
					{
						source.setSink( dataWriter );
						reader.init();
						columns = source.getColumns();

						// Write header

						JSONObject properties = new JSONObject();
						properties.set( "version", "1.0" );
						properties.set( "format", "record-stream" );
						properties.set( "description", "SolidBase JSON Data Dump File" );
						properties.set( "createdBy", new JSONObject( "product", "SolidBase", "version", "2.0.0" ) );

						if( createdDate )
						{
							// TODO Use internet format
							SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
							properties.set( "createdDate", format.format( new Date() ) );
						}

						if( parsed.binaryFileName != null )
						{
							// TODO FIXME Should be wrapped in a SourceException: solidbase.solidstack.io.FatalURISyntaxException: java.net.URISyntaxException: Illegal character in path at index 1: &{folder}/JIADHOCCH
							Resource binResource = Resources.getResource( parsed.binaryFileName );
							Resource resource = Resources.getResource( parsed.fileName );
							properties.set( "binaryFile", binResource.getPathFrom( resource ).toString() );
						}

						JSONArray fields = new JSONArray();
						properties.set( "fields", fields );
						for( int i = 0; i < columns.length; i++ )
						{
							Column column = columns[ i ];
							JSONObject field = new JSONObject();
							field.set( "schemaName", column.getSchema() );
							field.set( "tableName", column.getTable() );
							field.set( "name", column.getName() );
							field.set( "type", column.getTypeName() ); // TODO Better error message when type is not recognized, for example Oracle's 2007 for a user type
							FileSpec spec = fileSpecs[ i ];
							if( spec != null && !spec.isParameterized() )
							{
								Resource fileResource = new FileResource( spec.fileName );
								field.set( "file", fileResource.getPathFrom( jsonOutput ).toString() );
							}
							fields.add( field );
						}

						dataWriter.getJSONWriter().writeFormatted( properties, 120 );
						dataWriter.getJSONWriter().getWriter().write( '\n' );

						try
						{
							reader.process();
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
							if( binaryFile != null && binaryFile.out != null )
								binaryFile.out.close();
						}
					}
					finally
					{
						dataWriter.close();
					}
				}
				finally
				{
					processor.closeStatement( statement, true );
				}
			}
			finally
			{
				out.close();
			}
		}
		catch( IOException e )
		{
			throw new FatalIOException( e );
		}

		return true;
	}


	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	// TODO The headers should be like CBOR
	// TODO Add WITH HEADER
	// FIXME Add ENCODING to the TEXT file
	// FIXME and how should we detect it when reading?
	static protected Parsed parse( Command command )
	{
		/*
		EXPORT JSON
		FILE "<file>" [ GZIP ]
		[ BINARY FILE "file" [ GZIP ] ]
		[ WITH HEADER ]
		[ SEPARATED BY ( TAB | SPACE | <character> ) ]
		[ DATE AS TIMESTAMP ]
		[ COALESCE <col>, <col> [ , <col> ] ]
		[ LOG EVERY n ( RECORDS | SECONDS ) ]
		[ COLUMN <col> [ , <col> ] ( TO ( BINARY | TEXT ) FILE "<file>" [ THRESHOLD n ] | SKIP ) ]
		FROM <sqlstatement>
		*/

		Parsed result = new Parsed();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		EnumSet<Tokens> expected = EnumSet.of( Tokens.DATE, Tokens.COALESCE, Tokens.LOG, Tokens.FILE, Tokens.COLUMN, Tokens.BINARY, Tokens.FROM );

		Token t = tokenizer.skip( "EXPORT" ).skip( "JSON" ).get();
		for( ;; )
			switch( tokenizer.expect( t, expected ) )
			{
				case DATE:
					t = tokenizer.skip( "AS" ).skip( "TIMESTAMP" ).get();
					result.dateAsTimestamp = true;
					expected.remove( Tokens.DATE );
					break;

				case COALESCE:
					List<String> cols;
					if( result.coalesce == null )
						result.coalesce = new ArrayList<>();
					result.coalesce.add( cols = new ArrayList<>() );
					do
					{
						cols.add( tokenizer.getIdentifier().value() );
						t = tokenizer.get();
					}
					while( t.eq( "," ) );
					if( cols.size() < 2 )
						throw new SourceException( "COALESCE needs more than 1 column name", tokenizer.getLocation() );
					break;

				case LOG:
					int interval = Integer.parseInt( tokenizer.skip( "EVERY" ).getNumber().value() );
					if( tokenizer.get( "RECORDS", "SECONDS" ).eq( "RECORDS" ) )
						result.logRecords = interval;
					else
						result.logSeconds = interval;
					expected.remove( Tokens.LOG );
					t = tokenizer.get();
					break;

				case FILE:
					// TODO "TO FILE"
					result.fileName = tokenizer.getString().stripQuotes();
					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.gzip = true;
						t = tokenizer.get();
					}
					expected.remove( Tokens.FILE );
					break;

				case BINARY:
					result.binaryFileName = tokenizer.skip( "FILE" ).getString().stripQuotes();
					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.binaryGzip = true;
						t = tokenizer.get();
					}
					expected.remove( Tokens.BINARY );
					break;

				case COLUMN:
					if( result.columns == null )
						result.columns = new HashMap<>();
					cols = new ArrayList<>();
					do
					{
						cols.add( tokenizer.getIdentifier().value() );
						t = tokenizer.get();
					}
					while( t.eq( "," ) );
					tokenizer.expect( t, "TO", "SKIP" );

					ColumnSpec columnSpec;
					if( t.eq( "TO" ) )
					{
						boolean binary = tokenizer.get( "BINARY", "TEXT" ).eq( "BINARY" );
						String fileName = tokenizer.skip( "FILE" ).getString().stripQuotes();
						t = tokenizer.get();
						int threshold = 0;
						if( t.eq( "THRESHOLD" ) )
						{
							threshold = Integer.parseInt( tokenizer.getNumber().value() );
							t = tokenizer.get();
						}
						columnSpec = new ColumnSpec( false, new FileSpec( binary, fileName, threshold ) );
					}
					else
					{
						columnSpec = new ColumnSpec( true, null );
						t = tokenizer.get();
					}
					for( String col : cols )
						result.columns.put( col, columnSpec );
					break;

				case FROM:
					result.query = tokenizer.getRemaining();
					return result;

				default:
					throw new FatalException( "Unexpected token: " + t );
			}
	}


	@Override
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
		protected boolean gzip;

		protected String binaryFileName;
		protected boolean binaryGzip;

		/** The query */
		protected String query;

		protected boolean dateAsTimestamp;

		/** Which columns need to be coalesced */
		protected List<List<String>> coalesce;

		protected int logRecords;
		protected int logSeconds;

		protected Map<String, ColumnSpec> columns;
	}
}
