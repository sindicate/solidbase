package solidbase.http.hyperdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import solidbase.core.SystemException;
import solidbase.http.Fragment;
import solidbase.http.RequestContext;
import solidbase.http.ResponseWriter;
import solidbase.http.Servlet;
import solidbase.http.Util;
import solidbase.util.Assert;

public class TableServlet implements Servlet, Fragment
{
	public void call( RequestContext context )
	{
		String table = context.getRequest().getParameter( "tablename" );
		new Template().call( context, "SolidBrowser - table " + table, this );
	}

	public void fragment( RequestContext context )
	{
		String table = context.getRequest().getParameter( "tablename" );
//		String id = request.getParameter( "id" );

		ResponseWriter writer = context.getResponse().getWriter();

		Connection connection = DataSource.getConnection();
		try
		{
			try
			{
				String sql = "SELECT COUNT(*) FROM " + table;
//				System.out.println( "SQL: " + sql );

				ResultSet result = connection.createStatement().executeQuery( sql );
				Assert.isTrue( result.next() );
				Object object = result.getObject( 1 );

				sql = "SELECT * FROM " + table;
//				System.out.println( "SQL: " + sql );

				Statement statement = connection.createStatement();
				try
				{
					result = statement.executeQuery( sql );
					if( result.next() )
					{
						ResultSetMetaData meta = result.getMetaData();
						int count = meta.getColumnCount();
						writer.write( "<table>\n" );
						writer.write( "<tr><th colspan=\"" );
						writer.write( Integer.toString( count ) );
						writer.write( "\">" );
						writer.write( "Table " );
						Util.printEscaped( writer, table );
						writer.write( " - " );
						Util.printEscaped( writer, object.toString() );
						writer.write( " records" );
						writer.write( "</th>" );
						writer.write( "</tr>\n" );
						writer.write( "<tr>" );
						for( int i = 1; i <= count; i++ )
						{
							writer.write( "<th>" );
							Util.printEscaped( writer, meta.getColumnLabel( i ) );
							writer.write( "</th>" );
						}
						writer.write( "</tr>\n" );
						do
						{
							writer.write( "<tr>" );
							for( int i = 1; i <= count; i++ )
							{
								object = result.getObject(  i );
								if( object != null )
								{
									writer.write( "<td>" );
									Util.printEscaped( writer, object.toString() );
								}
								else
									writer.write( "<td class=\"null\">" );
								writer.write( "</td>" );
							}
							writer.write( "</tr>\n" );
						}
						while( result.next() );
						writer.write( "</table>\n" );
					}
					else
					{
						writer.write( "<table>\n" );
						writer.write( "<tr><th>" );
						writer.write( "Table " );
						Util.printEscaped( writer, table );
						writer.write( ", " );
						Util.printEscaped( writer, object.toString() );
						writer.write( " records" );
						writer.write( "</th>" );
						writer.write( "</tr>\n" );
						writer.write( "<tr>" );
						writer.write( "<td class=\"null\">No records.</td>" );
						writer.write( "</tr>\n" );
						writer.write( "</table>\n" );
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
	}
}
