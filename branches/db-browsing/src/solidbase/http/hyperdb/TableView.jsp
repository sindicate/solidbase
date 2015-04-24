<%@ page import="java.sql.*" %>
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
<div id="popup" style="visibility: hidden; left: 0px; top: 0px;">
	<a href="detail1">detail1</a><br/>
	<a href="detail2">detail2</a>
</div>
<table>
	<tr><th colspan="${count}">Table ${table} - ${recordCount} records</th></tr>
	<tr><% for( int i = 1; i <= count; i++ ) { %><th>${meta.getColumnLabel(i)}</th><% } %></tr>
<%
		do
		{
%>
	<tr class="data"></td><%
			for( int i = 1; i <= count; i++ )
			{
				Object object = result.getObject(  i );
				if( object != null ) { %><td>${object}</td><% } else { %><td class="null"></td><% }
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
	throw new HttpException( e );
}
%>
<script>
	var popup = document.getElementById( "popup" )
	popup.onmouseover = function() { popup.style.visibility = "visible" }
	popup.onmouseout = function() { popup.style.visibility = "hidden" }
	
	function detailClick() { return false }
	function dataRowClick()
	{
		popup.style.left = ( event.clientX - 10 ) + "px"
		popup.style.top = ( event.clientY - 10 ) + "px"
		popup.style.visibility = "visible"
		return false
	}
	
	walker( document.body,
		function( node )
		{
			if( node instanceof HTMLTableRowElement )
			{
				if( node.className == "data" )
					node.onclick = dataRowClick
			}
			return true
		}
	)
</script>
