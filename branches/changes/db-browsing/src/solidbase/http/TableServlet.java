package solidbase.http;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import solidbase.core.SystemException;
import solidbase.util.Assert;

public class TableServlet extends Servlet
{
	@Override
	public void call( Request request, Response response )
	{
		new Template().call( request, response, this );
	}

	@Override
	public void fragment( Request request, Response response, String fragment )
	{
		Assert.isTrue( "body".equals( fragment ) );

		String table = request.getParameter( "tablename" );
		String id = request.getParameter( "id" );

		PrintWriter writer = response.getPrintWriter();

		Connection connection = DataSource.getConnection();
		try
		{
			try
			{
				String sql = "SELECT COUNT(*) FROM " + table;
				System.out.println( "SQL: " + sql );

				ResultSet result = connection.createStatement().executeQuery( sql );
				Assert.isTrue( result.next() );
				Object object = result.getObject( 1 );

				sql = "SELECT * FROM " + table;
				System.out.println( "SQL: " + sql );

				Statement statement = connection.createStatement();
				try
				{
					result = statement.executeQuery( sql );
					if( result.next() )
					{
						ResultSetMetaData meta = result.getMetaData();
						int count = meta.getColumnCount();
						writer.append( "<table>" );
						writer.append( "<tr><th colspan=\"" );
						writer.append( Integer.toString( count ) );
						writer.append( "\">" );
						writer.append( "Table " );
						writer.append( table );
						writer.append( ", " );
						writer.append( object.toString() );
						writer.append( " records" );
						writer.append( "</th>" );
						writer.append( "</tr>" );
						writer.append( "<tr>" );
						for( int i = 1; i <= count; i++ )
						{
							writer.append( "<th>" );
							writer.append( meta.getColumnLabel( i ) );
							writer.append( "</th>" );
						}
						writer.append( "</tr>" );
						do
						{
							writer.append( "<tr>" );
							for( int i = 1; i <= count; i++ )
							{
								writer.append( "<td>" );
								object = result.getObject(  i );
								writer.append( object == null ? "&lt;NULL&gt;" : object.toString() );
								writer.append( "</td>" );
							}
							writer.append( "</tr>" );
						}
						while( result.next() );
						writer.append( "</table>" );
					}
					else
					{
						writer.append( "<table>" );
						writer.append( "<tr><th>" );
						writer.append( "Table " );
						writer.append( table );
						writer.append( ", " );
						writer.append( object.toString() );
						writer.append( " records" );
						writer.append( "</th>" );
						writer.append( "</tr>" );
						writer.append( "<tr>" );
						writer.append( "<td>No records.</td>" );
						writer.append( "</tr>" );
						writer.append( "</table>" );
					}
				}
				finally
				{
					statement.close();
				}
			}
			finally
			{
				DataSource.release( connection );
			}
		}
		catch( SQLException e )
		{
			throw new SystemException( e );
		}

		writer.flush();
	}
}
