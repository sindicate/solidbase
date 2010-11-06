<%@ page import="java.sql.*" %>
<%@ page import="solidbase.core.SystemException" %>
<%@ page import="solidbase.util.Assert" %>
<%@ page import="solidbase.http.*" %>

<%
String table = request.getRequest().getParameter( "tablename" );

ResponseWriter writer = request.getResponse().getWriter();

Object recordCount = params.get( "count" );
ResultSet result = (ResultSet)params.get( "result" );
try
{
	if( result.next() )
	{
		ResultSetMetaData meta = result.getMetaData();
		int count = meta.getColumnCount();
%>
<table>
	<tr><th colspan="${Integer.toString(count)}">Table ${table} - ${recordCount} records</th></tr>
	<tr><% for( int i = 1; i <= count; i++ ) { %><th>${meta.getColumnLabel(i)}</th><% } %></tr>
<%
		do
		{
%>
	<tr><%
			for( int i = 1; i <= count; i++ )
			{
				Object object = result.getObject(  i );
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
	<tr><th>Table ${table}, ${recordCount} records</th></tr>
	<tr><td class="null">No records.</td></tr>
</table>
<%
	}
}
catch( SQLException e )
{
	throw new SystemException( e );
}
%>
