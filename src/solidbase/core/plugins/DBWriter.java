package solidbase.core.plugins;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solidbase.core.SystemException;

public class DBWriter
{
	static private final Pattern parameterPattern = Pattern.compile( ":(\\d+)" );


	public DBWriter( String sql, String tableName, String[] columns, String[] values )
	{
		// TODO Auto-generated constructor stub
	}

	private void createSQL()
	{
//		String sql;
//		List< Integer > parameterMap = new ArrayList< Integer >();
//
//		if( parsed.sql != null )
//		{
//			sql = parsed.sql;
//			sql = translateArgument( sql, parameterMap );
//		}
//		else
//		{
//			StringBuilder sql1 = new StringBuilder( "INSERT INTO " );
//			sql1.append( parsed.tableName );
//			if( parsed.columns != null )
//			{
//				sql1.append( " (" );
//				for( int i = 0; i < parsed.columns.length; i++ )
//				{
//					if( i > 0 )
//						sql1.append( ',' );
//					sql1.append( parsed.columns[ i ] );
//				}
//				sql1.append( ')' );
//			}
//			if( parsed.values != null )
//			{
//				sql1.append( " VALUES (" );
//				for( int i = 0; i < parsed.values.length; i++ )
//				{
//					if( i > 0 )
//						sql1.append( "," );
//					String value = parsed.values[ i ];
//					value = translateArgument( value, parameterMap );
//					sql1.append( value );
//				}
//				sql1.append( ')' );
//			}
//			else
//			{
//				int count = line.length;
//				if( parsed.columns != null )
//					count = parsed.columns.length;
//				if( prependLineNumber )
//					count++;
//				int par = 1;
//				sql1.append( " VALUES (?" );
//				parameterMap.add( par++ );
//				while( par <= count )
//				{
//					sql1.append( ",?" );
//					parameterMap.add( par++ );
//				}
//				sql1.append( ')' );
//			}
//			sql = sql1.toString();
//		}
	}

	public void process( Object[] values )
	{
//		PreparedStatement statement = processor.prepareStatement( sql );
//		boolean commit = false;
//		try
//		{
//			int batchSize = 0;
//			while( true )
//			{
//				if( Thread.currentThread().isInterrupted() ) // TODO Is this the right spot during an upgrade?
//					throw new ThreadInterrupted();
//
//				preprocess( line );
//
//				int pos = 1;
//				int index = 0;
//				for( int par : parameterMap )
//				{
//					try
//					{
//						if( prependLineNumber )
//						{
//							if( par == 1 )
//								statement.setInt( pos++, lineNumber );
//							else
//								statement.setString( pos++, line[ index = par - 2 ] );
//						}
//						else
//							statement.setString( pos++, line[ index = par - 1 ] );
//					}
//					catch( ArrayIndexOutOfBoundsException e )
//					{
//						throw new SourceException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + line.length + " values", reader.getLocation().lineNumber( lineNumber ) );
//					}
//					catch( SQLException e )
//					{
//						String message = buildMessage( sql, parameterMap, prependLineNumber, lineNumber, line );
//						throw new SQLExecutionException( message, reader.getLocation().lineNumber( lineNumber ), e );
//					}
//				}
//
//				if( parsed.noBatch )
//				{
//					try
//					{
//						statement.executeUpdate();
//					}
//					catch( SQLException e )
//					{
//						String message = buildMessage( sql, parameterMap, prependLineNumber, lineNumber, line );
//						// When NOBATCH is on, you can see the actual insert statement and line number in the file where the SQLException occurred.
//						throw new SQLExecutionException( message, reader.getLocation().lineNumber( lineNumber ), e );
//					}
//				}
//				else
//				{
//					statement.addBatch();
//					batchSize++;
//					if( batchSize >= 1000 )
//					{
//						statement.executeBatch();
//						batchSize = 0;
//					}
//				}
//
//				if( counter != null && counter.next() )
//					processor.getProgressListener().println( "Imported " + counter.total() + " records." );
//
//				lineNumber = reader.getLineNumber();
//				line = reader.getLine();
//				if( line == null )
//				{
//					if( batchSize > 0 )
//						statement.executeBatch();
//
//					if( counter != null && counter.needFinal() )
//						processor.getProgressListener().println( "Imported " + counter.total() + " records." );
//
//					commit = true;
//					return;
//				}
//			}
//		}
//		finally
//		{
//			processor.closeStatement( statement, commit );
//		}
	}

	static private String buildMessage( String sql, List<Integer> parameterMap, boolean prependLineNumber, int lineNumber, String[] line )
	{
		StringBuilder result = new StringBuilder( sql );
		result.append( " VALUES (" );
		boolean first = true;
		for( int par : parameterMap )
		{
			if( first )
				first = false;
			else
				result.append( ',' );
			try
			{
				if( prependLineNumber )
				{
					if( par == 1 )
						result.append( lineNumber );
					else
						result.append( line[ par - 2 ] );
				}
				else
					result.append( line[ par - 1 ] );
			}
			catch( ArrayIndexOutOfBoundsException ee )
			{
				throw new SystemException( ee );
			}
		}
		result.append( ')' );
		return result.toString();
	}


	/**
	 * Replaces arguments within the given value with ? and maintains a map.
	 *
	 * @param value Value to be translated.
	 * @param parameterMap A map of ? index to index of the CSV fields.
	 * @return The translated value.
	 */
	static protected String translateArgument( String value, List< Integer > parameterMap )
	{
		Matcher matcher = parameterPattern.matcher( value );
		StringBuffer result = new StringBuffer();
		while( matcher.find() )
		{
			int num = Integer.parseInt( matcher.group( 1 ) );
			parameterMap.add( num );
			matcher.appendReplacement( result, "?" );
		}
		matcher.appendTail( result );
		return result.toString();
	}


	public void end()
	{
		// TODO Auto-generated method stub

	}
}
