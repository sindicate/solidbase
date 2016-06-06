package solidbase.core.plugins;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import solidbase.core.CommandProcessor;
import solidbase.core.SQLExecutionException;
import solidbase.core.SourceException;
import solidbase.core.SystemException;

public class DBWriter implements DataProcessor
{
	static private final Pattern parameterPattern = Pattern.compile( ":(\\d+)" );

	private String sql;
	private String tableName;
	private String[] fieldNames;
	private String[] values;
	private boolean noBatch;
	private CommandProcessor processor;

	private Column[] columns;

	private PreparedStatement statement;
	private int[] parameterMap;
	private int batchSize;


	public DBWriter( String sql, String tableName, String[] fieldNames, String[] values, boolean noBatch, CommandProcessor processor )
	{
		this.sql = sql;
		this.tableName = tableName;
		this.fieldNames = fieldNames;
		this.values = values;
		this.noBatch = noBatch;
		this.processor = processor;
	}

	@Override
	public void init( Column[] columns )
	{
		this.columns = columns;
	}

	private PreparedStatement createStatement( Object[] line ) throws SQLException
	{
		String[] fieldNames = this.fieldNames;
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
			sql1.append( this.tableName ); // TODO Where else do we need the quotes?
			if( fieldNames != null )
			{
				sql1.append( " (" );
				for( int i = 0; i < fieldNames.length; i++ )
				{
					if( i > 0 )
						sql1.append( ',' );
					sql1.append( fieldNames[ i ] );
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
				if( fieldNames != null )
					count = fieldNames.length;
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

		// Convert the strings to date, time and timestamps
		for( int i = 0; i < values.length; i++ )
		{
			Object value = values[ i ];
			try
			{
				// TODO Time zones, is there a default way of putting times and dates in a text file? For example whats in a HTTP header?
				if( value != null && value instanceof String )
					switch( this.columns[ i ].getType() )
					{
						case Types.DATE:
							values[ i ] = java.sql.Date.valueOf( (String)value );
							break;
						case Types.TIMESTAMP:
							values[ i ] = java.sql.Timestamp.valueOf( (String)value );
							break;
						case Types.TIME:
							values[ i ] = java.sql.Time.valueOf( (String)value );
							break;
					}
			}
			catch( IllegalArgumentException e )
			{
				// TODO Add test? C:\_WORK\SAO-20150612\build.xml:32: The following error occurred while executing this line:
				// C:\_WORK\SAO-20150612\build.xml:13: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff], at line 17 of file C:/_WORK/SAO-20150612/SYSTEEM/sca.JSON.GZ
				throw new SourceException( e.getMessage(), null );
			}
		}

		int pos = 1;
		int index = 0;
		for( int par : this.parameterMap )
		{
			try
			{
				Object value = values[ index = par - 1 ];
//				if( value instanceof Reader )
//					this.statement.setClob( pos++, (Reader)value );
//				else
					this.statement.setObject( pos++, value );
			}
			catch( ArrayIndexOutOfBoundsException e )
			{
				throw new SourceException( "Value with index " + ( index + 1 ) + " does not exist, record has only " + values.length + " values", null );
			}
			catch( SQLException e )
			{
				String message = buildMessage( this.sql, this.parameterMap, values );
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
				String message = buildMessage( this.sql, this.parameterMap, values );
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

	static private String buildMessage( String sql, int[] parameterMap, Object[] values )
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
				result.append( values[ par - 1 ] );
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
