package solidbase.http;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import solidbase.core.SystemException;
import solidbase.util.Assert;

public class TableServlet implements Servlet
{
	public void call( Request request, OutputStream response )
	{
		String table = request.getParameter( "tablename" );
		String id = request.getParameter( "id" );

		PrintWriter writer = new PrintWriter( response );
		writer.println( "HTTP/1.1 200" );
		writer.println();
		writer.println( "<html>" );
		writer.println( "<body>" );
		writer.println( "<p>Table " + table + "</p>" );
		writer.println( "<p>Id " + id + "</p>" );

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
				writer.println( "# records " + object );

				sql = "SELECT * FROM " + table;
				System.out.println( "SQL: " + sql );

				result = connection.createStatement().executeQuery( sql );
				if( result.next() )
				{
					ResultSetMetaData meta = result.getMetaData();
					int count = meta.getColumnCount();
					writer.append( "<table>" );
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

		writer.println( "</body>" );
		writer.println( "</html>" );
		writer.flush();
	}
}
