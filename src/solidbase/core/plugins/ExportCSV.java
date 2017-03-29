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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
import solidstack.io.Resource;
import solidstack.io.Resources;
import solidstack.io.SourceException;
import solidstack.io.SourceReaders;


/**
 * This plugin executes EXPORT CSV statements.
 *
 * @author René M. de Bloois
 * @since Aug 12, 2011
 */
// TODO Escape with \ instead of doubling double quotes. This means also \n \t \r. ESCAPE DQ CR LF TAB WITH \
public class ExportCSV implements CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*EXPORT\\s+CSV\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );


	//@Override
	@Override
	public boolean execute( CommandProcessor processor, Command command, boolean skip ) throws SQLException
	{
		if( command.isAnnotation() )
			return false;

		if( !triggerPattern.matcher( command.getCommand() ).matches() )
			return false;

		if( skip )
			return true;

		Parsed parsed = parse( command );

		Resource csvOutput = Resources.getResource( parsed.fileName ); // Relative to current folder
		csvOutput.setGZip( parsed.gzip );
		try
		{
			OutputStream out = csvOutput.newOutputStream();
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

					// TODO The UnsupportedEncodingException should be a SourceException
					CSVDataWriter dataWriter = new CSVDataWriter( new OutputStreamWriter( out, parsed.encoding ), parsed.separator, parsed.withHeader );
					try
					{
						source.setSink( dataWriter );
						reader.init();
						reader.process();
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
	// TODO We need database connection scope, like default settings for example DATE AS TIMESTAMP
	static protected Parsed parse( Command command )
	{
		/*
		EXPORT CSV
		FILE "<file>" ENCODING "<encoding>" [ GZIP ]
		[ WITH HEADER ]
		[ SEPARATED BY ( TAB | SPACE | <character> ) ]
		[ DATE AS TIMESTAMP ]
		[ COALESCE <col>, <col> [ , <col> ] ]
		[ LOG EVERY n ( RECORDS | SECONDS ) ]
		[ COLUMN <col> [ , <col> ] SKIP ]
		FROM <sqlstatement>
		*/

		Parsed result = new Parsed();

		SQLTokenizer tokenizer = new SQLTokenizer( SourceReaders.forString( command.getCommand(), command.getLocation() ) );

		EnumSet<Tokens> expected = EnumSet.of( Tokens.DATE, Tokens.COALESCE, Tokens.LOG, Tokens.FILE, Tokens.COLUMN, Tokens.FROM, Tokens.WITH, Tokens.SEPARATED );

		Token t = tokenizer.skip( "EXPORT" ).skip( "CSV" ).get();
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
						result.coalesce = new ArrayList<List<String>>();
					result.coalesce.add( cols = new ArrayList<String>() );
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
					result.fileName = tokenizer.getString().stripQuotes();
					result.encoding = tokenizer.skip( "ENCODING" ).getString().stripQuotes();
					t = tokenizer.get();
					if( t.eq( "GZIP" ) )
					{
						result.gzip = true;
						t = tokenizer.get();
					}
					expected.remove( Tokens.FILE );
					break;

				case COLUMN:
					if( result.columns == null )
						result.columns = new HashMap<String, ColumnSpec>();
					cols = new ArrayList<String>();
					do
					{
						cols.add( tokenizer.getIdentifier().value() );
						t = tokenizer.get();
					}
					while( t.eq( "," ) );
					tokenizer.expect( t, "SKIP" );
					ColumnSpec columnSpec = new ColumnSpec( true, null );
					for( String col : cols )
						result.columns.put( col, columnSpec );
					t = tokenizer.get();
					break;

				case WITH:
					t = tokenizer.skip( "HEADER" ).get();
					result.withHeader = true;
					expected.remove( Tokens.WITH );
					break;

				case SEPARATED:
					t = tokenizer.skip( "BY" ).get();
					if( t.eq( "TAB" ) )
						result.separator = '\t';
					else if( t.eq( "SPACE" ) )
						result.separator = ' ';
					else
					{
						if( t.length() != 1 )
							throw new SourceException( "Expecting [TAB], [SPACE] or a single character, not [" + t + "]", tokenizer.getLocation() );
						result.separator = t.value().charAt( 0 );
					}
					t = tokenizer.get();
					expected.remove( Tokens.SEPARATED );
					break;

				case FROM:
					result.query = tokenizer.getRemaining();
					return result;

				default:
					throw new FatalException( "Unexpected token: " + t );
			}
	}


	//@Override
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
		protected boolean withHeader;

		/** The separator. */
		protected char separator = ',';

		/** The file path to export to */
		protected String fileName;

		/** The encoding of the file */
		protected String encoding;

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
