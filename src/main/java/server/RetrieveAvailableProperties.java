package main.java.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.java.QueryEngine.JenaEngine;
import main.java.QueryEngine.QueryProperties;

public class RetrieveAvailableProperties extends HttpServlet {

	private static final long serialVersionUID = -4138926131264552041L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException  {
		
		// Produce output
		String output = getAvailableProperties();
		
		// build response
		PrintWriter out = response.getWriter();
		out.println(output);
		
		// Response stuff
		response.setContentType("application/json");
		String origin = request.getHeader("Origin");
		response.addHeader("Access-Control-Allow-Origin", origin);
	}  
	
	private static String getAvailableProperties() {
		JenaEngine je = new JenaEngine();
		QueryProperties qp = je.getAvailableProperties();
		return qp.toJSONString();
	}
	
	public static void main(String[] args) {
		System.out.println(getAvailableProperties());
	}
	
	
}
