package solidbase.http;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import solidbase.core.SystemException;
import solidbase.util.Assert;

public class TableServlet implements Servlet, Fragment
{
	public void call( Request request, Response response )
	{
		String table = request.getParameter( "tablename" );
		new Template().call( request, response, "SolidBrowser - table " + table, this );
	}

	public void fragment( Request request, Response response )
	{
		String table = request.getParameter( "tablename" );
//		String id = request.getParameter( "id" );

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
						writer.println( "<table>" );
						writer.print( "<tr><th colspan=\"" );
						writer.print( Integer.toString( count ) );
						writer.print( "\">" );
						writer.print( "Table " );
						Util.printEscaped( writer, table );
						writer.print( " - " );
						Util.printEscaped( writer, object.toString() );
						writer.print( " records" );
						writer.print( "</th>" );
						writer.println( "</tr>" );
						writer.print( "<tr>" );
						for( int i = 1; i <= count; i++ )
						{
							writer.print( "<th>" );
							Util.printEscaped( writer, meta.getColumnLabel( i ) );
							writer.print( "</th>" );
						}
						writer.println( "</tr>" );
						do
						{
							writer.print( "<tr>" );
							for( int i = 1; i <= count; i++ )
							{
								writer.print( "<td>" );
								object = result.getObject(  i );
								if( object != null )
									Util.printEscaped( writer, object.toString() );
								else
									writer.print( "&lt;NULL&gt;" );
								writer.print( "</td>" );
							}
							writer.println( "</tr>" );
						}
						while( result.next() );
						writer.println( "</table>" );
					}
					else
					{
						writer.println( "<table>" );
						writer.print( "<tr><th>" );
						writer.print( "Table " );
						Util.printEscaped( writer, table );
						writer.print( ", " );
						Util.printEscaped( writer, object.toString() );
						writer.print( " records" );
						writer.print( "</th>" );
						writer.println( "</tr>" );
						writer.print( "<tr>" );
						writer.print( "<td>No records.</td>" );
						writer.println( "</tr>" );
						writer.println( "</table>" );
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
