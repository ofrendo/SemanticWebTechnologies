

<%
	
	out.print("{\"msg\": \"Hello world!\"}");

%> 



<%
   response.setContentType("application/json");
   String origin = request.getHeader("Origin");
   response.addHeader("Access-Control-Allow-Origin", origin);
%>
