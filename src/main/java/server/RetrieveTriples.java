package main.java.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RetrieveTriples extends HttpServlet { 

	private static final long serialVersionUID = 4244463843880491238L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException  {
		
		// reading the user input
		String body = getBody(request);
		
		
		// Produce output
		String output = Server.processRequest(body);
		
		// build response
		PrintWriter out = response.getWriter();
		//out.println("{\"msg\": \"Hello world!\"}");  
		out.println(output);
		
		// Response stuff
		response.setContentType("application/json");
		String origin = request.getHeader("Origin");
		response.addHeader("Access-Control-Allow-Origin", origin);
	}  
	
	public static String getBody(HttpServletRequest request) throws IOException {
		// Read from request
	    StringBuilder buffer = new StringBuilder();
	    BufferedReader reader = request.getReader();
	    String line;
	    while ((line = reader.readLine()) != null) {
	        buffer.append(line);
	    }
	    String data = buffer.toString();
	    return data;
	}
	
}