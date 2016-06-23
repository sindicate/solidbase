/*--
 * Copyright 2016 René M. de Bloois
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import funny.Symbol;
import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.core.FatalException;
import solidbase.util.FixedIntervalLogCounter;
import solidbase.util.LogCounter;
import solidbase.util.SQLTokenizer;
import solidbase.util.SQLTokenizer.Token;
import solidbase.util.TimeIntervalLogCounter;
import solidstack.io.FatalIOException;
import solidstack.io.FileResource;
import solidstack.io.Resource;
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
 */
public class DumpCBOR implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*DUMP\\s+CBOR\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	// TODO Escape dynamic file names, because illegal characters may be generated
	// TODO Export multiple tables to a single file. If no PK than sort on all columns. Schema name for import or not?
	// TODO COLUMN TO TEXT FILE with ENCODING
	@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		if( command.isTransient() )
		{
			/* --* DUMP CBOR DATE_CREATED ON | OFF */

			SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

			// TODO Maybe DUMP CBOR CONFIG or DUMP CBORSET
			// TODO What about other configuration settings?
			tokenizer.get( "DUMP" );
			tokenizer.get( "CBOR" );
			tokenizer.get( "DATE_CREATED" ); // FIXME This should be CREATED_DATE
			Token t = tokenizer.get( "ON", "OFF" );
			tokenizer.get( (String)null );

			// TODO I think we should have a scope that is restricted to the current file and a scope that gets inherited when running or including another file.
			Scope scope = processor.getContext().getScope();
			scope.setOrVar( Symbol.apply( "solidbase.dump_cbor.dateCreated" ), t.eq( "ON" ) ); // TODO Make this a constant

			return true;
		}

		if( skip )
			return true;

		Parsed parsed = parse( command );

		Scope scope = processor.getContext().getScope();
		boolean dateCreated;
		try
		{
			Object object = scope.get( Symbol.apply( "solidbase.dump_cbor.dateCreated" ) );
			dateCreated = object instanceof Boolean && (Boolean)object;
		}
		catch( UndefinedException e )
		{
			dateCreated = true;
		}

		Resource cborOutput = new FileResource( new File( parsed.fileName ) ); // Relative to current folder

		try
		{
			OutputStream out = new BufferedOutputStream( cborOutput.getOutputStream(), 0x1000 );
			if( parsed.gzip )
				out = new BufferedOutputStream( new GZIPOutputStream( out, 0x1000 ), 0x1000 );
			try
			{
				Statement statement = processor.createStatement();
				try
				{
					ResultSet result = statement.executeQuery( parsed.query );

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

					CBORDataWriter dataWriter = new CBORDataWriter( out, parsed.columns );
					try
					{
						source.setSink( dataWriter );
						reader.init();
						columns = source.getColumns();

						JSONObject properties = new JSONObject();
						properties.set( "version", 1 );
						properties.set( "description", "SolidBase CBOR Data Dump File" );
						properties.set( "createdBy", new JSONObject( "product", "SolidBase", "version", "2.0.0" ) );
						if( dateCreated )
							properties.set( "createdDate", new Date() );
						dataWriter.getCBOROutputStream().write( properties );

						properties = new JSONObject();
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
								field.set( "file", fileResource.getPathFrom( cborOutput ).toString() );
							}
							fields.add( field );
						}
						dataWriter.getCBOROutputStream().tagRefNS().write( properties );

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


	static public enum TOKEN { DATE, COALESCE, LOG, FILE, COLUMN, FROM };

	/**
	 * Parses the given command.
	 *
	 * @param command The command to be parsed.
	 * @return A structure representing the parsed command.
	 */
	// TODO Add NOBATCH
	static protected Parsed parse( Command command )
	{
		/*
		DUMP CBOR
		DATE AS TIMESTAMP
		COALESCE col1, col2
		LOG EVERY n (RECORDS|SECONDS)
		FILE "file" [GZIP]
		COLUMN col1, col2 ( TO (BINARY|TEXT) FILE "file" [THRESHOLD n] | SKIP )
		FROM
		*/

		Parsed result = new Parsed();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		EnumSet<TOKEN> expected = EnumSet.of( TOKEN.DATE, TOKEN.COALESCE, TOKEN.LOG, TOKEN.FILE, TOKEN.COLUMN, TOKEN.FROM );

		Token t = tokenizer.skip( "DUMP" ).skip( "CBOR" ).get();
		for( ;; )
			switch( tokenizer.expect( t, expected ) )
			{
				case DATE:
					t = tokenizer.skip( "AS" ).skip( "TIMESTAMP" ).get();
					result.dateAsTimestamp = true;
					expected.remove( TOKEN.DATE );
					break;

				case COALESCE:
					if( result.coalesce == null )
						result.coalesce = new ArrayList<List<String>>();

					List<String> cols;
					result.coalesce.add( cols = new ArrayList<String>() );

					do
					{
						t = tokenizer.get();
						if( t.isString() || t.isNewline() || t.isEndOfInput() || t.isNumber() )
							throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
						cols.add( t.value() );
						t = tokenizer.get();
					}
					while( t.eq( "," ) );

					if( cols.size() < 2 )
						throw new SourceException( "COALESCE needs more than 1 column name", tokenizer.getLocation() );
					break;

				case LOG:
					t = tokenizer.skip( "EVERY" ).get();
					if( !t.isNumber() )
						throw new SourceException( "Expecting a number, not [" + t + "]", tokenizer.getLocation() );
					int interval = Integer.parseInt( t.value() );

					if( tokenizer.get( "RECORDS", "SECONDS" ).eq( "RECORDS" ) )
						result.logRecords = interval;
					else
						result.logSeconds = interval;

					expected.remove( TOKEN.LOG );
					t = tokenizer.get();
					break;

				case FILE:
					t = tokenizer.get();
					if( !t.isString() )
						throw new SourceException( "Expecting filename enclosed in double quotes, not [" + t + "]", tokenizer.getLocation() );
					result.fileName = t.stripQuotes();

					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.gzip = true;
						t = tokenizer.get();
					}

					expected.remove( TOKEN.FILE );
					break;

				case COLUMN:
					if( result.columns == null )
						result.columns = new HashMap<String, ColumnSpec>();

					cols = new ArrayList<String>();
					do
					{
						t = tokenizer.get();
						if( t.isString() || t.isNewline() || t.isEndOfInput() || t.isNumber() )
							throw new SourceException( "Expecting a column name, not [" + t + "]", tokenizer.getLocation() );
						cols.add( t.value() );
						t = tokenizer.get();
					}
					while( t.eq( "," ) );
					tokenizer.expect( t, "SKIP" );

					ColumnSpec columnSpec = new ColumnSpec( true, null );
					for( String col : cols )
						result.columns.put( col, columnSpec );

					t = tokenizer.get();
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
