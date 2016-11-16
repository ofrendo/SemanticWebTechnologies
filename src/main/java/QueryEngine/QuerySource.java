package main.java.QueryEngine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.util.FileManager;

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
	
	//init and start query for specific EntityType
	public QuerySource(Source s, EntityType et, List<NamedEntity> entities){ 
		this.source = s;
		this.endpoint = determineEndpoint(s);
		this.type = determineEntityTypeURI(s, et);
		this.model = ModelFactory.createDefaultModel();
		querySource(entities);
	}
	
	//init and query for complete context (independent of EntityType) and requested properties
	public QuerySource(Source s, List<NamedEntity> entities, List<String> properties){ 
		this.source = s;
		this.endpoint = determineEndpoint(s);
		this.type = null;
		this.model = ModelFactory.createDefaultModel();
		querySource(entities, properties);
	}
	
	private String determineEndpoint(Source s) {
		//  enpoint definition
		String ep = "";
		switch (s){
		case DBPedia:
			ep = "http://dbpedia.org/sparql";
			break;
		case LinkedMDB:
			ep = "http://linkedmdb.org/sparql";
			break;
		}
		return ep;
	}
	
	private String determineEntityTypeURI(Source s, EntityType et) {
		// rdf:type definition
		String uri = "";
		switch (s){
		case DBPedia:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://dbpedia.org/ontology/Organisation>";
				break;
			case PERSON:
				uri = "<http://dbpedia.org/ontology/Person>";
				break;
			case LOCATION:
				uri = "<http://dbpedia.org/ontology/Location>";
				break;
			}
			break;
		case LinkedMDB:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://data.linkedmdb.org/resource/movie/film_distributor>";
				break;
			case PERSON:
				uri = "<http://xmlns.com/foaf/0.1/Person>";
				break;
			case LOCATION:
				uri = "<http://data.linkedmdb.org/resource/movie/film_location>";
				break;
			}
			break;
		}
		return uri;
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
		for (NamedEntity ne : entities) {
			if(filter != ""){
				filter += " || ";
			}
			filter += "regex(?l,'" + ne.getRegexName() + "')";
		}				
		queryString += " && ( " + filter + " ) ) }"; 
	
		//System.out.println(queryString);
		
		// ---- Execute Query --------
		Query q = null;		
		try {
			q = QueryFactory.create(queryString);
		} catch (QueryParseException qexc) {
			System.out.println(source + " query generation failed for: " + entities + " - query string:");
			System.out.println(queryString);
			System.out.println(qexc.getMessage());
			return;
		}
		//System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		try {
			model = qe.execDescribe();
			System.out.println("Queried "+ source +" for: " + entities + ", size: " + model.size() + "; time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms");
		} catch (Exception exc) {
			System.out.println("Query for "+ source +" failed: " + exc.getMessage());
			System.out.println(queryString);
		} finally {
			qe.close();
		}
		
		//---------------- Query labels for subjects, predicates and objects ------------------
		List<String> uris = new ArrayList<String>();
		ResultSet results = QueryExecutionFactory.create("SELECT DISTINCT ?uri WHERE { "
				+ " "
				+ " { ?s ?p ?uri } UNION "
				+ " {?uri ?p1 ?o }"
				+ " FILTER (isURI(?uri)) "
				+ "}", model).execSelect();
		while(results.hasNext()) {  
			QuerySolution sol = results.next();
			uris.add(sol.getResource("uri").getURI());
//			System.out.println(sol.get("uri").toString());
		}
		
		results = QueryExecutionFactory.create("SELECT DISTINCT ?pred WHERE { ?s ?pred ?o }", model).execSelect();
		while(results.hasNext()) {  
			QuerySolution sol = results.next();
			uris.add(sol.get("pred").toString());
			//System.out.println(sol.get("pred").toString());
		}
		
		int from = 0;
		int to = 0;
		while(from < uris.size()-1){
			to += 100; //package size
			if (to >= uris.size())
				to = uris.size() -1;
			
			
		
			String f = " ?s = <" + String.join("> || ?s = <", uris.subList(from, to)) + ">";
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
				System.out.println("Query for labels from "+ source +" failed; count: " + uris.size() + e2.getMessage());
				System.out.println(q);
			} finally {
				qe.close() ;
			}
			
			from = to;
			
		}
		System.out.println("Queried labels from "+ source +", model size: " + model.size() + "; count: " + uris.size());
//		System.out.println(model);
//		results = QueryExecutionFactory.create("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//				+ " SELECT DISTINCT ?l WHERE { ?s rdfs:label ?l }", model).execSelect();
//		while(results.hasNext()) {
//			QuerySolution sol = results.next();
//			System.out.println(sol.get("l").toString());
//		}
	}
	
	private void querySource(List<NamedEntity> entities, List<String> properties) {
		String queryString = "";
		List<String> parts = new ArrayList<String>();
		
		Long start = System.nanoTime();
		
		// ------- 1) Search for URI candidates per EntityType with regex ------- 
		// Sort entity types into groups accoding to EntityType 
//		List<EntityType> relevant_ets = new ArrayList<EntityType>();
//		HashMap<EntityType,List<String>> et_regex = new HashMap<EntityType,List<String>>();
//		for (EntityType et : EntityType.values()) {
//			et_regex.put(et, new ArrayList<String>());
//		}		
//		for (NamedEntity ne : entities) {
//			et_regex.get(ne.getType()).add(ne.getRegexName());			
//			if(!relevant_ets.contains(ne.getType()))
//				relevant_ets.add(ne.getType());
//		}
//		
//		//Build Query
//		List<String> parts = new ArrayList<String>();
//		for (EntityType et : relevant_ets) {
//			parts.add("SELECT DISTINCT ?s WHERE {"
//					+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, et) + "."
//					+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
//					+ " FILTER (LANGMATCHES(LANG(?l), 'en') && isURI(?s) && ( "
//					+ "regex(?l,'" + String.join("') || regex(?l,'", et_regex.get(et)) + "')"
//					+ ") ) }"
//					);
//		}
//		//Union is potential for query parallelization -> is virtuoso parallelizing? -> if not: start query per et in threads??? 
//		//New concept: query per named entity and if >10 results -> apply some logic to reduce
//		String queryString = "SELECT DISTINCT ?s { { " + String.join(" } UNION { ", parts) + " } }";
//		
//		//Query URI candidates
//		List<String> uri_candidates = new ArrayList<String>();
//		for (QuerySolution s : execSelect(queryString)) {
//			uri_candidates.add(s.getResource("s").getURI());
//		}
		
		//TODO: Source specific cache based on NamedEntity name -> save regex queries
		
		// Per NamedEntity: Query for URI candidates
		ThreadGroup group = new ThreadGroup( String.valueOf(entities.toString().hashCode()));
		for (NamedEntity ne : entities) {
			queryString = "SELECT DISTINCT ?s ?label (COUNT(?p) AS ?count) WHERE {"
					+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, ne.getType()) + "."
					+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
					+ " ?s ?p ?o."
					+ " FILTER (LANGMATCHES(LANG(?l), 'en') && isURI(?s) && regex(?l,'" + ne.getRegexName() + "') )"
					+ " BIND (STR(?l) as ?label)"// BIND (COUNT(?p) as ?count)"
					+ " } GROUP BY ?s ?label"
					; 
			new BackgroundQueryExecution(group, queryString, endpoint, ne).start();
		}
		
		List<String> uri_candidates = new ArrayList<String>();
		try {
			BackgroundQueryExecution[] threads = new BackgroundQueryExecution[group.activeCount()];
			group.enumerate(threads);
			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
				List<QuerySolution> res = threads[i].getSolutions();
				NamedEntity ne = threads[i].getNamedEntity();
				if(res != null && res.size() > 0){
					if(res.size() <= 5){
						for (QuerySolution s : res) {
							uri_candidates.add(s.getResource("s").getURI());
						}
					}else{
						//extended logic for URI candidate identification, if too many -> Score: string similarity and count of relations
						//Score each URI by its label and relations
						TreeMap<Double, List<String>> score_uris = new TreeMap<Double, List<String>>();
						
						//ugly, but not performant via external SPARQL: get max count of relations
						int max_cnt = 0;
						for (QuerySolution s : res) {
							if(s.getLiteral("count").getInt() > max_cnt)
								max_cnt = s.getLiteral("count").getInt();
						}
						
						//calc score for each uri: Similarity * Relation Score having range 0-1
						//Similariy: EditDistanc normalized to range 0-1 
						//Realtion Score: Relation Count / Max(Relation Count) -> range 0-1
						for (QuerySolution s : res) {
							Double score = stringSimilarity(s.get("label").toString(),ne.getName()) * (s.getLiteral("count").getInt()/max_cnt);
							if(score_uris.containsKey(score)){
								score_uris.get(score).add(s.getResource("s").getURI());
							}else{
								List<String> new_list = new ArrayList<String>();
								new_list.add(s.getResource("s").getURI());
								score_uris.put(score, new_list);
							}
						}						
						//Select at least top 5 scored URIs - but accept only -10% from top Score 
						int cnt = 0;
						Double min_score = score_uris.descendingKeySet().first() * 0.6;
						for (Double key : score_uris.descendingKeySet()) {
							if(key < min_score)
								break;
							uri_candidates.addAll(score_uris.get(key));
							System.out.println(key + ": " + score_uris.get(key));
							cnt += score_uris.get(key).size();
							if(cnt >= 5)
								break;
						}
						
					}
						
				}
			}
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}

		Long stop = System.nanoTime();
		System.out.println(source + ": Queried URI candidates. Count: " + uri_candidates.size() + ". Time: " + TimeUnit.NANOSECONDS.toMillis(stop-start) + "ms");
		start = stop;
		
		
		
		// ------- 2) Construct Model based on candidate URIs ------- 
		
//		CONSTRUCT { ?s ?p ?o }
//		WHERE{
//		   {
//		   { 
//		   VALUES ?p { <http://xmlns.com/foaf/0.1/made> <http://dbpedia.org/ontology/foundedBy> <http://dbpedia.org/property/website> <http://dbpedia.org/property/homepage> <http://xmlns.com/foaf/0.1/homepage> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> <http://xmlns.com/foaf/0.1/depiction> }
//		   VALUES ?s { <http://dbpedia.org/resource/SAP_SE> <http://dbpedia.org/resource/Hasso_Plattner> <http://dbpedia.org/resource/Walldorf> }
//		   ?s ?p ?o
//		   } UNION {
//		   VALUES ?s { <http://dbpedia.org/resource/SAP_SE> <http://dbpedia.org/resource/Hasso_Plattner> <http://dbpedia.org/resource/Walldorf> }  
//		   VALUES ?o { <http://dbpedia.org/resource/SAP_SE> <http://dbpedia.org/resource/Hasso_Plattner> <http://dbpedia.org/resource/Walldorf> } 
//		   ?s ?p ?o
//		   } UNION {
//		   VALUES ?s { <http://dbpedia.org/resource/SAP_SE> <http://dbpedia.org/resource/Hasso_Plattner> <http://dbpedia.org/resource/Walldorf> }  
//		   VALUES ?e { <http://dbpedia.org/resource/SAP_SE> <http://dbpedia.org/resource/Hasso_Plattner> <http://dbpedia.org/resource/Walldorf> }
//		   ?s ?p ?o.
//		   ?e ?p2 ?o.
//		   FILTER (?s != ?e) 
//		   }
//		   }
//		}
		
		parts = new ArrayList<String>(); 
		
		//Available Properties
		parts.add("{"
				+ " VALUES (?p) { (<" + String.join(">) (<", properties) + ">)}"
				+ " VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				+ "}");
		
		//Direct Relations
		parts.add("{"
				+ " VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " VALUES (?o) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				+ "}");
		
		//Indirect Relations
		parts.add("{"
				+ " VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " VALUES (?e) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				+ " ?e ?p2 ?o."
				+ " FILTER (?s != ?e)"
				+ "}");
		
		
		//https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct
		queryString = "CONSTRUCT { ?s ?p ?o."
				+ " ?s <http://www.w3.org/2000/01/rdf-schema#label> ?ls."
				+ " ?p <http://www.w3.org/2000/01/rdf-schema#label> ?lp."
				+ " ?o <http://www.w3.org/2000/01/rdf-schema#label> ?lo."
				+ " } WHERE { "
				+ "{ " + String.join(" UNION ", parts) + " } "
				+ " OPTIONAL {?s <http://www.w3.org/2000/01/rdf-schema#label> ?ls.}"
				+ " OPTIONAL {?p <http://www.w3.org/2000/01/rdf-schema#label> ?lp.}"
				+ " OPTIONAL {?o <http://www.w3.org/2000/01/rdf-schema#label> ?lo.}"
				+ " FILTER ( (LANG(?ls) = '' || LANGMATCHES(LANG(?ls), 'en')) "
				+ " && (LANG(?lp) = '' || LANGMATCHES(LANG(?lp), 'en')) "
				+ " && (LANG(?lo) = '' || LANGMATCHES(LANG(?lo), 'en')))"
				+ "}"
				;
		
		// execute Query 
		model.add(execConstruct(queryString));
		stop = System.nanoTime();
		System.out.println(source + ": Queried properties, relations and labels. Model size: " + model.size() + ". Time: " + TimeUnit.NANOSECONDS.toMillis(stop-start) + "ms");
		start = stop;
		
		
		
//		// ------- 3) Query labels for subjects, predicates and objects ------- 
//		
//		// Collect all uris ...
//		List<String> uris = new ArrayList<String>();
//		ResultSet results = QueryExecutionFactory.create("SELECT DISTINCT ?uri WHERE { "
//				+ " { ?s ?p ?uri } UNION {?uri ?p1 ?o }"
//				+ " FILTER (isURI(?uri)) "
//				+ "}", model).execSelect();
//		while(results.hasNext()) {  
//			QuerySolution sol = results.next();
//			uris.add(sol.getResource("uri").getURI());
////			System.out.println(sol.get("uri").toString());
//		}
//		
//		// ... and predicates from local model
//		results = QueryExecutionFactory.create("SELECT DISTINCT ?pred WHERE { ?s ?pred ?o }", model).execSelect();
//		while(results.hasNext()) {  
//			QuerySolution sol = results.next();
//			uris.add(sol.get("pred").toString());
////			System.out.println(sol.get("pred").toString());
//		}
//		
//		// Query label per package 
//		int packagesize = 100;
//		int from, to;
//		from = to = 0;
//		while(from < uris.size()-1){
//			to += packagesize; 
//			if (to >= uris.size())
//				to = uris.size() -1;
//			
//			//construct package query
//			queryString = "CONSTRUCT { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?l } WHERE {"
//					+ " VALUES (?s) { (<" + String.join(">) (<", uris.subList(from, to)) + ">) }"
//					+ " ?s <http://www.w3.org/2000/01/rdf-schema#label> ?l."
//					+ " FILTER (LANGMATCHES(LANG(?l), 'en'))}"
//					;
//			
//			//exec Query			
//			model.add(execConstruct(queryString));
//			
//			from = to;			
//		}
//		stop = System.nanoTime();
//		System.out.println(source + ": Queried labels. Model size: " + model.size() + ". Time: " + TimeUnit.NANOSECONDS.toMillis(stop-start) + "ms");
//		start = stop;
	}
	
	//Similarity (from http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java)
	private double stringSimilarity(String s1, String s2) {
	    String longer = s1, shorter = s2;
	    if (s1.length() < s2.length()) { // longer should always have greater length
	      longer = s2; shorter = s1;
	    }
	    int longerLength = longer.length();
	    if (longerLength == 0) { return 1.0; /* both strings are zero length */ }

	    return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength; 
	  }
	

	
	private Model execConstruct(String queryString) {
		Model m = ModelFactory.createDefaultModel();
		
		Query q = null;		
		try {
			q = QueryFactory.create(queryString);
		} catch (QueryParseException qexc) {
			System.out.println(source + " query generation failed! Query string:");
			System.out.println(queryString);
			System.out.println(qexc.getMessage());
			return m;
		}
//		System.out.println(q);
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		try {
			m = qe.execConstruct();
		} catch (Exception exc) {
			System.out.println(source + ": Query failed: " + exc.getMessage());
			System.out.println(queryString);
		} finally {
			qe.close();
		}
		return m;
	}
	
//	private List<QuerySolution> execSelect(String queryString) {
//		List<QuerySolution> res = new ArrayList<QuerySolution>();
//		
//		Query q = null;		
//		try {
//			q = QueryFactory.create(queryString);
//		} catch (QueryParseException qexc) {
//			System.out.println(source + ": Query generation failed! Query string:");
//			System.out.println(queryString);
//			System.out.println(qexc.getMessage());
//			return res;
//		}
//		System.out.println(q);
//		
//		QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, q);
//		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
//		qeHttp.setModelContentType("application/rdf+xml");
//		try {
//			ResultSet r = qe.execSelect();
//			while(r.hasNext()) {  
//				res.add(r.nextSolution());
//			}
//			
//		} catch (Exception exc) {
//			System.out.println(source + ": Query failed: " + exc.getMessage());
//			System.out.println(queryString);
//		} finally {
//			qe.close();
//		}
//		return res;
//	}
	
	
	// #################################### TEST SECTION #################################################

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		//Load local Ontology from file
		InputStream in = FileManager.get().open("data/UMA-SWT-HWS16.owl");
		try {
			m.read(in,null);
		} catch (Exception e) {
			System.out.println("Error during ontology import: " + e.getMessage());
		}
		
		String queryString = " SELECT ?p WHERE { ?a <http://www.w3.org/2002/07/owl#equivalentProperty> ?p."
//				+ " ?a rdf:type rdf:Property."
				+ "}"; 
		Query query = QueryFactory.create(queryString); 
		//System.out.println(query);
		QueryExecution qe = QueryExecutionFactory.create(query, m); 
		ResultSet results = qe.execSelect(); 
		List<String> props = new ArrayList<String>();
		while(results.hasNext()) {  
			QuerySolution sol = results.next();  
			String s = sol.get("p").toString();
			System.out.println(s);
			props.add(s);
		}			
		qe.close();
		
		
		//  ---- Query Tests
		Query q = QueryFactory.create("CONSTRUCT { ?s ?p ?o } "
				+ "WHERE {"
				+ "VALUES (?p) { (<" + String.join(">) (<", props) + ">)}"
				+ "{SELECT DISTINCT ?s WHERE {"
				+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Organisation>."
				+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."   
				+ " FILTER (LANGMATCHES(LANG(?l), 'en') && regex(?l, 'SAP SE') )"				
				+ "}}"
				+ " ?s ?p ?o."// ?s <http://www.w3.org/2000/01/rdf-schema#label> ?l.  "// <http://dbpedia.org/ontology/Organisation>."
//				+ " FILTER (LANGMATCHES(LANG(?l), 'en') && regex(?l,'SAP SE') "
//				+ " FILTER (regex(?l,'SAP SE') "
//				+ " && (?p = <" + String.join("> || ?p = <", props) + "> )"
				+ "}");
		
		
		System.out.println(q);
		qe = QueryExecutionFactory.sparqlService(
				"http://dbpedia.org/sparql", 
				q);
		QueryEngineHTTP qeHttp = (QueryEngineHTTP) qe;
		qeHttp.setModelContentType("application/rdf+xml");
		
		try {
			Model mr = qe.execConstruct();			
			System.out.println(mr);
		} catch (QueryExecException e) {
			e.getCause();
		}
		
		
		
//		ResultSet results = qe.execSelect();
//		while(results.hasNext()) {
//			QuerySolution sol = results.next();
//			System.out.println(sol.get("l").toString());
//		}
	}
}
