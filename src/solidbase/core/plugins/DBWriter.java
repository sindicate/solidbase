package solidbase.core.plugins;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import solidbase.core.CommandProcessor;
import solidbase.core.SQLExecutionException;
import solidbase.core.SourceException;
import solidbase.core.SystemException;

public class DBWriter
{
	static private final Pattern parameterPattern = Pattern.compile( ":(\\d+)" );

	private String sql;
	private String tableName;
	private String[] columns;
	private String[] values;
	private boolean noBatch;
	private CommandProcessor processor;

	private PreparedStatement statement;
	private int[] parameterMap;
	private int batchSize;


	public DBWriter( String sql, String tableName, String[] columns, String[] values, boolean noBatch, CommandProcessor processor )
	{
		this.sql = sql;
		this.tableName = tableName;
		this.columns = columns;
		this.values = values;
		this.noBatch = noBatch;
		this.processor = processor;
	}

	private PreparedStatement createStatement( Object[] line ) throws SQLException
	{
		String[] columns = this.columns;
		String[] values = this.values;

		String sql;
		List< Integer > parameterMap = new ArrayList< Integer >();

		if( this.sql != null )
		{
			sql = this.sql;
			sql = translateArgument( sql, parameterMap );
		}
		else
		{
			StringBuilder sql1 = new StringBuilder( "INSERT INTO " );
			sql1.append( this.tableName );
			if( columns != null )
			{
				sql1.append( " (" );
				for( int i = 0; i < columns.length; i++ )
				{
					if( i > 0 )
						sql1.append( ',' );
					sql1.append( columns[ i ] );
				}
				sql1.append( ')' );
			}
			if( values != null )
			{
				sql1.append( " VALUES (" );
				for( int i = 0; i < values.length; i++ )
				{
					if( i > 0 )
						sql1.append( "," );
					String value = values[ i ];
					value = translateArgument( value, parameterMap );
					sql1.append( value );
				}
				sql1.append( ')' );
			}
			else
			{
				int count = line.length;
				if( columns != null )
					count = columns.length;
//				if( prependLineNumber )
//					count++;
				int par = 1;
				sql1.append( " VALUES (?" );
				parameterMap.add( par++ );
				while( par <= count )
				{
					sql1.append( ",?" );
					parameterMap.add( par++ );
				}
				sql1.append( ')' );
			}
			sql = sql1.toString();
		}

		// TODO Google Guava has a Ints.toArray() ?
		this.parameterMap = ArrayUtils.toPrimitive( parameterMap.toArray( new Integer[ parameterMap.size() ] ) );
		this.sql = sql;
		return this.processor.prepareStatement( sql );
	}

	public void process( Object[] values ) throws SQLException
	{
		if( this.statement == null )
			this.statement = createStatement( values );

		int pos = 1;
		int index = 0;
		for( int par : this.parameterMap )
		{
			try
			{
				this.statement.setString( pos++, (String)values[ index = par - 1 ] );
			}
			catch( ArrayIndexOutOfBoundsException e )
			{
				throw new SourceException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + values.length + " values", null );
			}
			catch( SQLException e )
			{
				String message = buildMessage( this.sql, this.parameterMap, (String[])values );
				throw new SQLExecutionException( message, null, e );
			}
		}

		if( this.noBatch )
		{
			try
			{
				this.statement.executeUpdate();
			}
			catch( SQLException e )
			{
				String message = buildMessage( this.sql, this.parameterMap, (String[])values );
				// When NOBATCH is on, you can see the actual insert statement and line number in the file where the SQLException occurred.
				throw new SQLExecutionException( message, null, e );
			}
		}
		else
		{
			this.statement.addBatch();
			this.batchSize++;
			if( this.batchSize >= 1000 )
			{
				this.statement.executeBatch();
				this.batchSize = 0;
			}
		}
	}

	static private String buildMessage( String sql, int[] parameterMap, String[] line )
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


	public void end( boolean commit ) throws SQLException
	{
		if( this.batchSize > 0 )
		{
			this.statement.executeBatch();
			this.batchSize = 0;
		}

		if( this.statement != null )
			this.processor.closeStatement( this.statement, commit );
	}
}
