package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;

public class QuerySource {
	public enum Source {
	    DBPedia, LinkedMDB
	}

	private Model model;
	private String type;
	private String endpoint;
	private Source source;

	public Model getModel(){
		return model;
	}

	public QuerySource(Source s, EntityType et, List<NamedEntity> entities){ 
		this.source = s;
		determineSourceParameters(s,et);
		querySource(entities);
	}
	
	private void determineSourceParameters(Source s, EntityType et) {
		// rdf:type and enpoint definition
		switch (s){
		case DBPedia:
			endpoint = "http://dbpedia.org/sparql";
			switch (et) {
			case ORGANIZATION:
				type = "<http://dbpedia.org/ontology/Organisation>";
				break;
			case PERSON:
				type = "<http://dbpedia.org/ontology/Person>";
				break;
			case LOCATION:
				type = "<http://dbpedia.org/ontology/Location>";
				break;
			}
			break;
		case LinkedMDB:
			endpoint = "http://linkedmdb.org/sparql";
			switch (et) {
			case ORGANIZATION:
				type = "<http://data.linkedmdb.org/resource/movie/film_distributor>";
				break;
			case PERSON:
				type = "<http://xmlns.com/foaf/0.1/Person>";
				break;
			case LOCATION:
				type = "<http://data.linkedmdb.org/resource/movie/film_location>";
				break;
			}
			break;
		}
	}

	private void querySource(List<NamedEntity> entities) {
		Long start = System.nanoTime();
		
		// ---- Definitions ---
		String queryString = "";
			

		// 2) DESCRIBE Clause
		queryString += "DESCRIBE ?e";
		// 3) Where Clause	
		queryString += " WHERE { "
				+ "?e <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + type + ". "
				+ "?e <http://www.w3.org/2000/01/rdf-schema#label> ?l."
			//	+ "?e <http://dbpedia.org/ontology/abstract> ?a."
			//	+ "?e <http://www.w3.org/2000/01/rdf-schema#comment> ?c."
				;
		// 3c) Filter		
		queryString += " FILTER( LANGMATCHES(LANG(?l), 'en')"
			//	+ " && LANGMATCHES(LANG(?a), 'en')"
			//	+ " && LANGMATCHES(LANG(?c), 'en')"
				;
		// 3d) dynamic filter part
		String filter = "";
		for (NamedEntity e : entities) {
			if(filter != ""){
				filter += " || ";
			}
			filter += "regex(?l,'" + e.getRegexName() + "')";
		}				
		queryString += " && ( " + filter + " ) ) }"; 
	
		//System.out.println(queryString);
		
		// ---- Execute Query --------
		Query q = null;		
		try {
			q = QueryFactory.create(queryString);
		} catch (QueryParseException e) {
			System.out.println(source + " query generation failed for: " + entities + " - query string:");
			System.out.println(queryString);
			System.out.println(e.getMessage());
			return;
		}
		//System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		try {
			model = qe.execDescribe();
			System.out.println("Queried "+ source +" for: " + entities + ", size: " + model.size() + "; time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms");
		} catch (Exception e2) {
			System.out.println("Query for "+ source +" failed: " + e2.getMessage());
			System.out.println(q);
		} finally {
			qe.close() ;
		}
		
		//---------------- Query labels for subjects, predicates and objects ------------------
		List<String> subjects = new ArrayList<String>();
		ResultSet results = QueryExecutionFactory.create("SELECT DISTINCT ?uri WHERE { "
				+ " "
				+ " { ?s ?p ?uri } UNION "
				+ " {?uri ?p1 ?o }"
				+ " FILTER (isURI(?uri)) "
				+ "}", model).execSelect();
		while(results.hasNext()) {  
			QuerySolution sol = results.next();
			subjects.add(sol.getResource("uri").getURI());
//			System.out.println(sol.get("uri").toString());
		}
		
		results = QueryExecutionFactory.create("SELECT DISTINCT ?pred WHERE { ?s ?pred ?o }", model).execSelect();
		while(results.hasNext()) {  
			QuerySolution sol = results.next();
			subjects.add(sol.get("pred").toString());
			//System.out.println(sol.get("pred").toString());
		}
		
		int from = 0;
		int to = 0;
		while(from < subjects.size()-1){
			to += 100; //package size
			if (to >= subjects.size())
				to = subjects.size() -1;
			
			
		
			String f = " ?s = <" + String.join("> || ?s = <", subjects.subList(from, to)) + ">";
			//System.out.println(f);
			
			q = QueryFactory.create("SELECT ?s ?p ?l WHERE { "
					+ " ?s ?p ?o "
					+ " FILTER ( ( " + f + " ) && ?p = <http://www.w3.org/2000/01/rdf-schema#label> && LANGMATCHES(LANG(?o), 'en')"
					+ " ) BIND (STR(?o) as ?l)}");
			
			qe = QueryExecutionFactory.sparqlService(endpoint, q);
			try {
				results = qe.execSelect();
				while(results.hasNext()) {
					QuerySolution sol = results.next();
					//System.out.println(sol.get("s").toString() + " - " + sol.get("p").toString() + " - " + sol.get("o").toString());
					Literal l = ResourceFactory.createLangLiteral(sol.get("l").toString(), "en");
					Resource r = ResourceFactory.createResource(sol.get("s").toString());
					Property p = ResourceFactory.createProperty(sol.get("p").toString());
					model.addLiteral(r, p, l);					
				}			
//				model.add(qe.execDescribe());				
			} catch (Exception e2) {
				System.out.println("Query for labels from "+ source +" failed; count: " + subjects.size() + e2.getMessage());
				System.out.println(q);
			} finally {
				qe.close() ;
			}
			
			from = to;
			
		}
		System.out.println("Queried labels from "+ source +", model size: " + model.size() + "; count: " + subjects.size());
//		System.out.println(model);
//		results = QueryExecutionFactory.create("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//				+ " SELECT DISTINCT ?l WHERE { ?s rdfs:label ?l }", model).execSelect();
//		while(results.hasNext()) {
//			QuerySolution sol = results.next();
//			System.out.println(sol.get("l").toString());
//		}
	}
}
