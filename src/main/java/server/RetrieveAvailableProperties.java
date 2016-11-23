package main.java.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import main.java.QueryEngine.JenaEngine;
import main.java.QueryEngine.QueryProperties;

public class RetrieveAvailableProperties extends HttpServlet {

	private static final long serialVersionUID = -4138926131264552041L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException  {
		
		// Produce output
		String output = getAvailableProperties();
		
		// Response stuff
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		String origin = request.getHeader("Origin");
		response.addHeader("Access-Control-Allow-Origin", origin);
		
		// build response
		//PrintWriter out = response.getWriter();
		//out.println(output);
		OutputStream out = response.getOutputStream();
		out.write(output.getBytes("UTF-8"));
		out.flush();
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
