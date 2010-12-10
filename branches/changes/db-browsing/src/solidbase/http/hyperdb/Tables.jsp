<%@ page import="java.util.List" %>
<%@ page import="solidbase.http.*" %>
<%@ page import="solidbase.http.hyperdb.*" %>

<%
new TemplateServlet().call( request, new Parameters( params ).put( "title", "All tables" ).put( "body", new Servlet()
{
	public void call( RequestContext request, Parameters params )
	{
		ResponseWriter writer = request.getResponse().getWriter();
		List< Table > tables = Database.getTables();
%>
		<table id="tables">
			<tr><th>Table</th><th># records</th></tr>
<%		for( Table table : tables ) { %>
			<tr><td><a href="/tables/${table.name}">${table.name}</a></td><td></td></tr>
<%		} %>
		</table>
<script>
//	window.onload = function()
//	{
		var tables = document.getElementById( "tables" );
		var rows = tables.rows;
		for( var i = 1, len = rows.length; i < len; i++ )
		{
			var cells = rows[ i ].cells;
			var table = cells[ 0 ].innerText;
			getAsync( "/tables/" + table + "/recordcount",
				function( cell )
				{
					return function( result )
					{
						cell.innerHTML = result;
					}
				} ( cells[ 1 ] )
			)
		}
//	}
</script>
<%
	}
}));
%>
