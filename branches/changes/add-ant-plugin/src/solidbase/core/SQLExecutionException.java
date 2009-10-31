package solidbase.core;

import java.sql.SQLException;
import java.util.Iterator;


public class SQLExecutionException extends SQLException
{
	protected Command command;
	protected SQLException sqlException;

	public SQLExecutionException( Command command, SQLException e )
	{
		super( e.getCause() );
		this.command = command;
		this.sqlException = e;
	}

	@Override
	public String getMessage()
	{
		return this.sqlException.getMessage() + "\nSQLState: " + this.sqlException.getSQLState() + "\nWhile executing: " + this.command.getCommand();
	}

	@Override
	public int getErrorCode()
	{
		return this.sqlException.getErrorCode();
	}

	@Override
	public SQLException getNextException()
	{
		return this.sqlException.getNextException();
	}

	@Override
	public String getSQLState()
	{
		return this.sqlException.getSQLState();
	}

	@Override
	public Iterator< Throwable > iterator()
	{
		return this.sqlException.iterator();
	}

	@Override
	public void setNextException( SQLException ex )
	{
		this.sqlException.setNextException( ex );
	}
}
