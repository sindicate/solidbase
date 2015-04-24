package solidbase.core.plugins.genericsql;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.Command;
import solidbase.core.CommandListener;
import solidbase.core.CommandProcessor;
import solidbase.util.Tokenizer;
import solidbase.util.Tokenizer.Token;

public class GenericSQL extends CommandListener
{
	static private final Pattern triggerPattern = Pattern.compile( "\\s*ANSI\\s+.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

	@Override
	public boolean execute( CommandProcessor processor, Command command ) throws SQLException
	{
		if( command.isTransient() )
			return false;

		Matcher matcher = triggerPattern.matcher( command.getCommand() );
		if( !matcher.matches() )
			return false;

		Statement statement = parse( command );

		System.out.println( "Statement: " + statement );

		String sql = new DerbyDialect().toSQLString( statement );
		System.out.println( sql );

		command.setCommand( sql );
		processor.execute( command );

		return true;
	}

	/**
	 * Parses the given command.
	 * 
	 * @param command The command to be parsed.
	 */
	static protected Statement parse( Command command )
	{
		Tokenizer tokenizer = new Tokenizer( new StringReader( command.getCommand() ), command.getLineNumber() );

		tokenizer.get( "ANSI" );

		tokenizer.get( "CREATE" );

		return parseCreate( tokenizer );
	}

	/**
	 * Parses the given command.
	 * 
	 */
	static protected Statement parseCreate( Tokenizer tokenizer )
	{
		tokenizer.get( "TABLE" );

		return parseCreateTable( tokenizer );
	}

	/**
	 * Parses the given command.
	 * 
	 */
	static protected CreateTableStatement parseCreateTable( Tokenizer tokenizer )
	{
		CreateTableStatement result = new CreateTableStatement();

		result.setTableName( tokenizer.get().getValue() );

		tokenizer.get( "(" );

		Token t;
		do
		{
			t = tokenizer.get();
			if( t.equals( "CONSTRAINT" ) )
			{
				Token name = tokenizer.get();
				tokenizer.get( "PRIMARY" );
				tokenizer.get( "KEY" );

				// TODO

				t = tokenizer.get( ",", ")" );
			}
			else
			{
				ColumnSpec column = new ColumnSpec();
				result.addColumn( column );
				column.setName( t.getValue() );

				Type type = new Type();
				column.setType( type );
				type.setType( tokenizer.get().getValue() );

				t = tokenizer.get( ",", ")", "(", "NOT", "GENERATED", "CONSTRAINT" );
				if( t.equals( "(" ) )
				{
					Token length = tokenizer.get();
					type.setLength( length.getValue() );
					tokenizer.get( ")" );
					t = tokenizer.get( ",", ")", "NOT", "GENERATED", "CONSTRAINT" );
				}

				if( t.equals( "NOT" ) )
				{
					tokenizer.get( "NULL" );
					t = tokenizer.get( ",", ")", "GENERATED", "CONSTRAINT" );
					column.setNotNull( true );
				}

				if( t.equals( "GENERATED" ) )
				{
					tokenizer.get( "ALWAYS" );
					tokenizer.get( "AS" );
					tokenizer.get( "IDENTITY" );
					t = tokenizer.get( ",", ")", "CONSTRAINT" );
					column.setGenerated( true );
				}

				if( t.equals( "CONSTRAINT" ) )
				{
					Token name = tokenizer.get();
					t = tokenizer.get( "PRIMARY", "UNIQUE" );
					if( t.equals( "PRIMARY" ) )
					{
						tokenizer.get( "KEY" );
						column.setPrimaryKey( name.getValue() );
					}
					else
						column.setUniqueKey( name.getValue() );
					t = tokenizer.get( ",", ")" );
				}
			}
		}
		while( t.equals( "," ) );

		return result;
	}
}
