package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import main.java.NEREngine.NamedEntity;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

public class BackgroundQueryExecution extends Thread {
	public enum QueryType {
	    SELECT, CONSTRUCT
	}
	
	private QueryType queryType;
	private String queryString;
	private String endpoint;
	private NamedEntity ne;
	private List<QuerySolution> solutions;
	private Model model;
	private String errorMsg;
	private boolean hasError;
	
	public BackgroundQueryExecution(ThreadGroup group, String queryString, String endpoint, QueryType queryType) {		
		this(group, queryString, endpoint, null, queryType);
	}	

	public BackgroundQueryExecution(ThreadGroup group, String queryString, String endpoint, NamedEntity ne, QueryType queryType) {		
		super(group,("QueryExec_" + endpoint + queryString.hashCode()));
		this.queryString = queryString;
		this.endpoint = endpoint;
		this.ne = ne; //only stored if thread needs to provide the information later (cache reference)
		this.solutions = new ArrayList<QuerySolution>();
		this.model = ModelFactory.createDefaultModel();
		this.queryType = queryType;
		this.errorMsg = "";
		this.hasError = false;
	}
	
	public void run(){
		if(queryString != null && endpoint != null ){
			switch(queryType){
			case SELECT:
				solutions = execSelect();
				break;
			case CONSTRUCT:
				model = execConstruct();
				break;
			}
			
		}
	}
	
	//in case of select
	public List<QuerySolution> getSolutions(){
		return solutions;
	}
	
	//in case of construct (or describe)
	public Model getModel(){
		return model;
	}
	
	public NamedEntity getNamedEntity(){
		return ne;
	}
	
	public boolean hasError(){
		return hasError;
	}
	
	public String getErrorMsg(){
		return errorMsg;
	}
	
	public String getQueryString(){
		return queryString;
	}
	
	private List<QuerySolution> execSelect() {
		List<QuerySolution> res = new ArrayList<QuerySolution>();
		
		Query q = null;		
		try {			
//			if(endpoint == "http://linkedmdb.org/sparql")
//				q = QueryFactory.create(queryString,Syntax.syntaxSPARQL_10);
//			else
				q = QueryFactory.create(queryString);
		} catch (QueryParseException qexc) {
//			System.out.println("ERROR - " + endpoint + ": Background query generation failed! Query string:");
//			System.out.println(queryString);
//			System.out.println(qexc.getMessage());
			hasError = true;
			errorMsg = "Background query generation failed: "+ qexc.getMessage();
			return res;
		}
//		System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		qeHttp.setTimeout(60, TimeUnit.SECONDS, 60, TimeUnit.SECONDS);
		try {
			ResultSet r = qe.execSelect();
			while(r.hasNext()) {  
				res.add(r.nextSolution());
			}
			
		} catch (Exception exc) {
//			System.out.println("ERROR - " + endpoint + ": Background query failed: " + exc.getMessage());
//			System.out.println("ERROR - " + queryString);
			hasError = true;
			errorMsg = "Background query failed: " + exc.getMessage();
		} finally {
			qe.close();
		}
		return res;
	}
	
	//Actual execution of construct query to endpoint
	private Model execConstruct() {
		Model m = ModelFactory.createDefaultModel();
		
		Query q = null;		
		try {
			q = QueryFactory.create(queryString);
		} catch (QueryParseException qexc) {
//			System.out.println("ERROR - " + endpoint + ": Query generation failed: " + qexc.getMessage());
//			System.out.println("ERROR - " + queryString);
			hasError = true;
			errorMsg = "Background query generation failed: "+ qexc.getMessage();
			return m;
		}
//		System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		try {
			m = qe.execConstruct();
		} catch (Exception exc) {
//			System.out.println("ERROR - " + endpoint + ": Query failed: " + exc.getMessage());
//			System.out.println("ERROR - " + queryString);
			hasError = true;
			errorMsg = "Background query failed: " + exc.getMessage();
		} finally {
			qe.close();
		}
		return m;
	}
}
