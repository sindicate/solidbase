<%@ page import="java.sql.*" %>
<%@ page import="solidbase.core.SystemException" %>
<%@ page import="solidbase.util.Assert" %>
<%@ page import="solidbase.http.*" %>

<%
String table = request.getRequest().getParameter( "tablename" );

ResponseWriter writer = request.getResponse().getWriter();

Connection connection = DataSource.getConnection();
try
{
	try
	{
		String sql = "SELECT COUNT(*) FROM " + table;

		ResultSet result = connection.createStatement().executeQuery( sql );
		Assert.isTrue( result.next() );
		Object object = result.getObject( 1 );

		sql = "SELECT * FROM " + table;

		Statement statement = connection.createStatement();
		try
		{
			result = statement.executeQuery( sql );
			if( result.next() )
			{
				ResultSetMetaData meta = result.getMetaData();
				int count = meta.getColumnCount();
%>
<table>
	<tr><th colspan="${Integer.toString(count)}">Table ${table} - ${object.toString()} records</th></tr>
	<tr><% for( int i = 1; i <= count; i++ ) { %><th>${meta.getColumnLabel(i)}</th><% } %></tr>
<%
				do
				{
%>
	<tr><%
					for( int i = 1; i <= count; i++ )
					{
						object = result.getObject(  i );
						if( object != null ) { %><td>${object.toString()}</td><% } else { %><td class="null"></td><% }
					}
%></tr>
<%
				}
				while( result.next() );
%>
</table>
<%
			}
			else
			{
%>
<table>
	<tr><th>Table ${table}, ${object.toString()} records</th></tr>
	<tr><td class="null">No records.</td></tr>
</table>
<%
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
%>
