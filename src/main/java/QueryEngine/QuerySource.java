package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;

public class QuerySource {
	public enum Source {
	    DBPedia, LinkedMDB
	}

	private static HashMap<String, List<String>> uriCache; 
	
	private Model model;
	private String endpoint;
	private Source source;
	

	public Model getModel(){
		return model;
	}
	
	//init and query for complete context (independent of EntityType) and requested properties
	public QuerySource(Source s, List<NamedEntity> entities, List<String> properties){ 
		this.source = s;
		this.endpoint = determineEndpoint(s);
		this.model = ModelFactory.createDefaultModel();
		if(uriCache == null)
			uriCache = new HashMap<String, List<String>>();
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

	
	private void querySource(List<NamedEntity> entities, List<String> properties) {
		String queryString = "";
		List<String> parts = new ArrayList<String>();
		
		Long start = System.nanoTime();
		
		List<String> uri_candidates = new ArrayList<String>();
		
		// Per NamedEntity: Query for URI candidates
		ThreadGroup group = new ThreadGroup( String.valueOf(entities.toString().hashCode()));
		boolean runQuery = false; 
		for (NamedEntity ne : entities) {
			if(uriCache.containsKey(ne.getCacheRef())){
				//Source specific cache based on NamedEntity name -> save regex queries
				uri_candidates.addAll(uriCache.get(ne.getCacheRef()));
				System.out.println(source + ": Found in cache: " + ne.getCacheRef());
			}else{
				if(source == Source.LinkedMDB){ //SPARQL 1.0 //TODO: BIND causes error
					queryString = "SELECT DISTINCT ?s ?label ?count WHERE {"
							+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, ne.getType()) + "."
							+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
							+ " ?s ?p ?o."
							+ " FILTER(LANGMATCHES(LANG(?l), 'en') && isURI(?s) && regex(?l,'" + ne.getRegexName() + "') )"
							+ " BIND(STR(?l) as ?label) BIND(1 as ?count)"
							+ " }"
							; 
				}else{
					queryString = "SELECT DISTINCT ?s ?label (COUNT(?p) AS ?count) WHERE {"
							+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, ne.getType()) + "."
							+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
							+ " ?s ?p ?o."
							+ " FILTER (LANGMATCHES(LANG(?l), 'en') && isURI(?s) && regex(?l,'" + ne.getRegexName() + "') )"
							+ " BIND (STR(?l) as ?label)"// BIND (COUNT(?p) as ?count)"
							+ " } GROUP BY ?s ?label"
							; 
				}
				runQuery = true;
				System.out.println(source + ": Start query for " + ne.getCacheRef());
				new BackgroundQueryExecution(group, queryString, endpoint, ne).start();
			}
		}
		
		if(runQuery){
			try {
				BackgroundQueryExecution[] threads = new BackgroundQueryExecution[group.activeCount()];
				group.enumerate(threads);
				for (int i = 0; i < threads.length; i++) {
					threads[i].join();
					List<QuerySolution> res = threads[i].getSolutions();
					NamedEntity ne = threads[i].getNamedEntity();
					if(res != null && res.size() > 0){
						List<String> uris = new ArrayList<String>();
						if(res.size() <= 5){
							for (QuerySolution s : res) {
								uris.add(s.getResource("s").getURI());
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
								uris.addAll(score_uris.get(key));
								System.out.println(key + ": " + score_uris.get(key));
								cnt += score_uris.get(key).size();
								if(cnt >= 5)
									break;
							}							
						}	
						//URI candidate determination done -> store in cache and candidate list
						uriCache.put(ne.getCacheRef(), uris);
						uri_candidates.addAll(uris);
					}					
				}
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		Long stop = System.nanoTime();
		System.out.println(source + ": Queried URI candidates. Count: " + uri_candidates.size() + ". Time: " + TimeUnit.NANOSECONDS.toMillis(stop-start) + "ms");
		start = stop;
		
		
		
		// ------- 2) Construct Model based on candidate URIs ------- 
		
//		*Example*
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
		
	
	// #################################### TEST SECTION #################################################

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	
	}
}
