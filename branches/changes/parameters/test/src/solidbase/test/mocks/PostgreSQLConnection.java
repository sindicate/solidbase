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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

// This mock Connection (and mock DriverManager, PreparedStatement) simulate PostgreSQL
// PostgreSQL aborts a transaction when an SQLException is raised, so you can't continue with it
public class PostgreSQLConnection implements Connection
{
	private Connection connection;
	boolean transactionAborted;

	public PostgreSQLConnection( Connection connection )
	{
		this.connection = connection;
	}

	public Statement createStatement() throws SQLException
	{
		return this.connection.createStatement();
	}

	public PreparedStatement prepareStatement( String sql ) throws SQLException
	{
		if( this.transactionAborted )
			throw new SQLException( "Current transaction is aborted", "25P02" );
		try
		{
			PreparedStatement result = this.connection.prepareStatement( sql );
			return new PostgreSQLPreparedStatement( this, result );
		}
		catch( SQLException e )
		{
			if( !getAutoCommit() )
				this.transactionAborted = true;
			throw e;
		}
	}

	public void setAutoCommit( boolean autoCommit ) throws SQLException
	{
		this.connection.setAutoCommit( autoCommit );
	}

	public boolean getAutoCommit() throws SQLException
	{
		return this.connection.getAutoCommit();
	}

	public void commit() throws SQLException
	{
		this.connection.commit();
		this.transactionAborted = false;
	}

	public void rollback() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void close() throws SQLException
	{
		this.connection.close();
	}

	public < T > T unwrap( Class< T > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isWrapperFor( Class< ? > iface ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public CallableStatement prepareCall( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public String nativeSQL( String sql ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isClosed() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public DatabaseMetaData getMetaData() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setReadOnly( boolean readOnly ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isReadOnly() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setCatalog( String catalog ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public String getCatalog() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTransactionIsolation( int level ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getTransactionIsolation() throws SQLException
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

	public Statement createStatement( int resultSetType, int resultSetConcurrency ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency )
			throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Map< String, Class< ? >> getTypeMap() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setTypeMap( Map< String, Class< ? >> map ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setHoldability( int holdability ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public int getHoldability() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Savepoint setSavepoint() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Savepoint setSavepoint( String name ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void rollback( Savepoint savepoint ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void releaseSavepoint( Savepoint savepoint ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Statement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability )
			throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int[] columnIndexes ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, String[] columnNames ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Clob createClob() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Blob createBlob() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public NClob createNClob() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public SQLXML createSQLXML() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isValid( int timeout ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public void setClientInfo( String name, String value ) throws SQLClientInfoException
	{
		throw new UnsupportedOperationException();
	}

	public void setClientInfo( Properties properties ) throws SQLClientInfoException
	{
		throw new UnsupportedOperationException();
	}

	public String getClientInfo( String name ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Properties getClientInfo() throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Array createArrayOf( String typeName, Object[] elements ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	public Struct createStruct( String typeName, Object[] attributes ) throws SQLException
	{
		throw new UnsupportedOperationException();
	}
}
