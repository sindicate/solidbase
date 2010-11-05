<%@ page import="java.util.List" %>
<%@ page import="solidbase.http.*" %>
<%@ page import="solidbase.http.hyperdb.*" %>

<%
new Template().call( context, "SolidBrowser - tables", new Fragment()
{
	public void fragment( RequestContext context )
	{
		ResponseWriter writer = context.getResponse().getWriter();
		List< Table > tables = Database.getTables();
%>
		<table>
			<tr><th>Table</th><th># records</th></tr>
<%		for( Table table : tables ) { %>
			<tr><td><a href="/table:${table.name}"><%=table.name%></a></td><td>${Integer.toString( table.records )}</td></tr>
<%		} %>
		</table>
<%
	}
});
%>
