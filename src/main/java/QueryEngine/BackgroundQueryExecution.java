package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.List;

import main.java.NEREngine.NamedEntity;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

public class BackgroundQueryExecution extends Thread {
	String queryString;
	String endpoint;
	NamedEntity ne;
	List<QuerySolution> solutions;
	

	public BackgroundQueryExecution(ThreadGroup group, String queryString, String endpoint, NamedEntity ne) {		
		super(group,("QueryExec_" + endpoint + queryString.hashCode()));
		this.queryString = queryString;
		this.endpoint = endpoint;
		this.ne = ne;
		this.solutions = new ArrayList<QuerySolution>();
	}
	
	public void run(){
		if(queryString != null && endpoint != null ){
			solutions = execSelect();
		}
	}
	
	public List<QuerySolution> getSolutions(){
		return solutions;
	}
	
	public NamedEntity getNamedEntity(){
		return ne;
	}
	
	private List<QuerySolution> execSelect() {
		List<QuerySolution> res = new ArrayList<QuerySolution>();
		
		Query q = null;		
		try {
			q = QueryFactory.create(queryString);
		} catch (QueryParseException qexc) {
			System.out.println(endpoint + ": Background query generation failed! Query string:");
			System.out.println(queryString);
			System.out.println(qexc.getMessage());
			return res;
		}
//		System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		try {
			ResultSet r = qe.execSelect();
			while(r.hasNext()) {  
				res.add(r.nextSolution());
			}
			
		} catch (Exception exc) {
			System.out.println(endpoint + ": Background query failed: " + exc.getMessage());
			System.out.println(queryString);
		} finally {
			qe.close();
		}
		return res;
	}
}
