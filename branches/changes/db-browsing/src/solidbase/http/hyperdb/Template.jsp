<%@ page import="java.util.List" %>
<%@ page import="solidbase.http.*" %>
<%@ page import="solidbase.http.hyperdb.*" %>

<%
String title = (String)params.get( "title" );
Servlet body = (Servlet)params.get( "body" );

request.getResponse().setContentType( "text/html", "UTF-8" );

ResponseWriter writer = request.getResponse().getWriter();
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
	<title>HyperDB${ title == null ? "" : " - " + title }</title>
	<link rel="stylesheet" type="text/css" href="/styles.css" />
	<script type="text/javascript" src="/util.js"></script>
</head>
<body>
<% body.call( request, params ); %>
</body>
</html>
