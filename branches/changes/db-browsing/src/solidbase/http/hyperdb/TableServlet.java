package solidbase.http.hyperdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import solidbase.core.SystemException;
import solidbase.http.Parameters;
import solidbase.http.RequestContext;
import solidbase.http.Servlet;
import solidbase.util.Assert;

public class TableServlet implements Servlet
{
	public void call( RequestContext context, final Parameters params )
	{
		String table = context.getRequest().getParameter( "tablename" );

		Connection connection = DataSource.getConnection();
		try
		{
			Statement statement = connection.createStatement();
			try
			{
				final ResultSet result1 = statement.executeQuery( "SELECT COUNT(*) FROM " + table );
				Assert.isTrue( result1.next() );
				final Object object = result1.getObject( 1 );

				final ResultSet result2 = statement.executeQuery( "SELECT * FROM " + table );

				new TemplateServlet().call( context, new Parameters( params ).
						put( "title", "table " + table ).
						put( "result", result2 ).
						put( "count", object.toString() ).
						put( "body", new TableViewServlet() ) );
			}
			finally
			{
				statement.close();
			}
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}
		finally
		{
			DataSource.release( connection );
		}
	}
}
