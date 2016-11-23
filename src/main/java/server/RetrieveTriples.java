package main.java.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
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
		
		// Response stuff
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		String origin = request.getHeader("Origin");
		response.addHeader("Access-Control-Allow-Origin", origin);
		
		// Produce output
		String output = Server.processRequest(body);
		
		// build response
		//PrintWriter out = response.getWriter();
		//out.println(output);
		OutputStream out = response.getOutputStream();
		out.write(output.getBytes("UTF-8"));
		out.flush();
		
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