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

package solidbase.test.mocks;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

// This mock PreparedStatement (and mock Connection, DriverManager) simulate PostgreSQL
// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it
public class PostgreSQLPreparedStatement implements PreparedStatement
{
	private PostgreSQLConnection connection;
	private PreparedStatement statement;

	public PostgreSQLPreparedStatement( PostgreSQLConnection connection, PreparedStatement statement )
	{
		this.connection = connection;
		this.statement = statement;
	}

	public void close() throws SQLException
	{
		this.statement.close();
	}

	public ResultSet executeQuery() throws SQLException
	{
		if( this.connection.transactionAborted )
			throw new SQLException( "Current transaction is aborted", "25P02" );
		try
		{
			return this.statement.executeQuery();
		}
		catch( SQLException e )
		{
			if( !this.connection.getAutoCommit() )
				this.connection.transactionAborted = true;
			throw e;
		}
	}

	public int executeUpdate() throws SQLException
	{
		if( this.connection.transactionAborted )
			throw new SQLException( "Current transaction is aborted", "25P02" );
		try
		{
			return this.statement.executeUpdate();
		}
		catch( SQLException e )
		{
			if( !this.connection.getAutoCommit() )
				this.connection.transactionAborted = true;
			throw e;
		}
	}

	public void setString( int parameterIndex, String x ) throws SQLException
	{
		this.statement.setString( parameterIndex, x );
	}

	public void setObject( int parameterIndex, Object x ) throws SQLException
	{
		this.statement.setObject( parameterIndex, x );
	}

	public ResultSet executeQuery( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int executeUpdate( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getMaxFieldSize() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setMaxFieldSize( int max ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getMaxRows() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setMaxRows( int max ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setEscapeProcessing( boolean enable ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getQueryTimeout() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setQueryTimeout( int seconds ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void cancel() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public SQLWarning getWarnings() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void clearWarnings() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setCursorName( String name ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean execute( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public ResultSet getResultSet() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getUpdateCount() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean getMoreResults() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setFetchDirection( int direction ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getFetchDirection() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setFetchSize( int rows ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getFetchSize() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getResultSetConcurrency() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getResultSetType() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void addBatch( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void clearBatch() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int[] executeBatch() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Connection getConnection() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean getMoreResults( int current ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public ResultSet getGeneratedKeys() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int executeUpdate( String sql, int autoGeneratedKeys ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int executeUpdate( String sql, int[] columnIndexes ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int executeUpdate( String sql, String[] columnNames ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean execute( String sql, int autoGeneratedKeys ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean execute( String sql, int[] columnIndexes ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean execute( String sql, String[] columnNames ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getResultSetHoldability() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isClosed() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setPoolable( boolean poolable ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isPoolable() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public < T > T unwrap( Class< T > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isWrapperFor( Class< ? > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNull( int parameterIndex, int sqlType ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBoolean( int parameterIndex, boolean x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setByte( int parameterIndex, byte x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setShort( int parameterIndex, short x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setInt( int parameterIndex, int x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setLong( int parameterIndex, long x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setFloat( int parameterIndex, float x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setDouble( int parameterIndex, double x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBigDecimal( int parameterIndex, BigDecimal x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBytes( int parameterIndex, byte[] x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setDate( int parameterIndex, Date x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTime( int parameterIndex, Time x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTimestamp( int parameterIndex, Timestamp x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setAsciiStream( int parameterIndex, InputStream x, int length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setUnicodeStream( int parameterIndex, InputStream x, int length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBinaryStream( int parameterIndex, InputStream x, int length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void clearParameters() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setObject( int parameterIndex, Object x, int targetSqlType ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean execute() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void addBatch() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setCharacterStream( int parameterIndex, Reader reader, int length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setRef( int parameterIndex, Ref x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBlob( int parameterIndex, Blob x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setClob( int parameterIndex, Clob x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setArray( int parameterIndex, Array x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public ResultSetMetaData getMetaData() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setDate( int parameterIndex, Date x, Calendar cal ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTime( int parameterIndex, Time x, Calendar cal ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTimestamp( int parameterIndex, Timestamp x, Calendar cal ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNull( int parameterIndex, int sqlType, String typeName ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setURL( int parameterIndex, URL x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public ParameterMetaData getParameterMetaData() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setRowId( int parameterIndex, RowId x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNString( int parameterIndex, String value ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNCharacterStream( int parameterIndex, Reader value, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNClob( int parameterIndex, NClob value ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setClob( int parameterIndex, Reader reader, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBlob( int parameterIndex, InputStream inputStream, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNClob( int parameterIndex, Reader reader, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setSQLXML( int parameterIndex, SQLXML xmlObject ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setObject( int parameterIndex, Object x, int targetSqlType, int scaleOrLength ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setAsciiStream( int parameterIndex, InputStream x, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBinaryStream( int parameterIndex, InputStream x, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setCharacterStream( int parameterIndex, Reader reader, long length ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setAsciiStream( int parameterIndex, InputStream x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBinaryStream( int parameterIndex, InputStream x ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setCharacterStream( int parameterIndex, Reader reader ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNCharacterStream( int parameterIndex, Reader value ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setClob( int parameterIndex, Reader reader ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setBlob( int parameterIndex, InputStream inputStream ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setNClob( int parameterIndex, Reader reader ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void closeOnCompletion() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isCloseOnCompletion() throws SQLException
	{
		throw new UnsupportedOperationException();
	}
}
