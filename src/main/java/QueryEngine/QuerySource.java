package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;
import main.java.QueryEngine.BackgroundQueryExecution.QueryType;

public class QuerySource extends Thread{
	
	public enum Source {
	    DBPedia 
	    ,DBPediaLive 
	    ,FactForge
	    ,EEA  //European Environment Agency
	    ,LinkedMDB
	    ,Education_UK
	    ,DataGovUk
	    ,IServe
	    ,WorldBank
	    ,YAGO2
	    ,LOB //Linked Open Beer
	}

	private static HashMap<String, List<String>> uriCache; 
	
	private Model model;
	private String endpoint;
	private Source source;
	private List<NamedEntity> entities;
	private List<String> properties;
	private List<String> uri_candidates;
//	private boolean isSPARQL10; 
	
	// ############ Interface ###############
	
	public Model getModel(){
		return model;
	}

	public List<NamedEntity> getEntities(){
		return entities;
	}
	
	public QuerySource.Source getSource(){
		return source;
	}
	
	//init and query for complete context (independent of EntityType) and requested properties
	public QuerySource(ThreadGroup group,Source s, List<NamedEntity> entities, List<String> properties){
		super(group,(s + "_" + entities.hashCode()));
		this.source = s;
		this.entities = entities;
		this.properties = properties;
		this.uri_candidates = new ArrayList<String>();
		
		determineSourceProperties(s);
		this.model = ModelFactory.createDefaultModel(); //Fallback: Empty Model
		if(uriCache == null)
			uriCache = new HashMap<String, List<String>>();
		
	}
	
	public void run(){
		// 1) Identify potential URIs 
		this.uri_candidates.addAll(getURICandidates(entities));
		
		// 2) Query context of them
		if(uri_candidates.isEmpty()){
			System.out.println(source + ": ERROR - No URIs found.");
			return;
		}
		getContextModel(this.uri_candidates);
	}
	
	//######## Methods for source specific definitions ##############
	
	private void determineSourceProperties(Source s) {
		//  enpoint definition
		switch (s){
		case DBPedia:
			this.endpoint = "http://dbpedia.org/sparql"; 
//			this.isSPARQL10 = false;
			break;
		case DBPediaLive:
			this.endpoint = "http://dbpedia-live.openlinksw.com/sparql/"; 
			break;
		case FactForge:
			this.endpoint = "http://factforge.net/sparql";
			break;
		case EEA:
			this.endpoint = "http://semantic.eea.europa.eu/sparql"; 
			break;
		case LinkedMDB:
			this.endpoint = "http://linkedmdb.org/sparql";
//			this.isSPARQL10 = true;
			break;
		case Education_UK:
			this.endpoint = "http://services.data.gov.uk/education/sparql";
			break;
		case DataGovUk:
			this.endpoint = "http://services.data.gov.uk/reference/sparql";
			break;
		case IServe:
			this.endpoint = "http://iserve.kmi.open.ac.uk/iserve/sparql";
			break;
		case WorldBank:
			this.endpoint = "http://worldbank.270a.info/sparql";
			break;
		case YAGO2:
			this.endpoint = "https://linkeddata1.calcul.u-psud.fr/sparql";
			break;
		case LOB: 
			this.endpoint = "http://ec2-52-209-172-100.eu-west-1.compute.amazonaws.com:8890/sparql";
			break;
		}
		
	}
	
	private String determineEntityTypeURI(Source s, EntityType et) { 
		// rdf:type definition
		String uri = "";
		switch (s){	
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
		case Education_UK:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://education.data.gov.uk/def/school/School>";
				break;
			case PERSON:
				uri = ""; // don't have persons 
				break;
			case LOCATION:
				uri = "<http://data.ordnancesurvey.co.uk/ontology/admingeo/CivilAdministrativeArea>";
				break;
		}
		case DataGovUk:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://www.w3.org/ns/org#Organization>";
				break;
			case PERSON:
				uri = "<http://xmlns.com/foaf/0.1/Person>"; 
				break;
			case LOCATION:
				uri = ""; //have no location
				break;
		}
		case IServe:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://schema.org/Organization>";
				break;
			case PERSON:
				uri = ""; //have no persons
				break;
			case LOCATION:
				uri = ""; //have no location
				break;
			}
		case WorldBank:
			switch (et) {
			case ORGANIZATION:
				uri = ""; //no organisations
				break;
			case PERSON:
				uri = ""; //no persons
				break;
			case LOCATION:
				uri = "<http://dbpedia.org/ontology/Country>";
				break;
		}
		case YAGO2:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://yago-knowledge.org/resource/wordnet_organization_108008335>";
				break;
			case PERSON:
				uri = "<http://yago-knowledge.org/resource/wordnet_person_100007846>";
				break;
			case LOCATION:
				uri = "<http://yago-knowledge.org/resource/wordnet_location_100027167>";
				break;
		}
		case LOB:
			switch (et) {
			case ORGANIZATION:
				uri = "<http://dws.informatik.uni-mannheim.de/swt/linked-open-beer/ontology/Brewery>";
				break;
			case PERSON:
				uri = ""; //none
				break;
			case LOCATION:
				uri = ""; //none
				break;
		}			
		break;
		default:  //Default is DBPedia, DBPediaLive, FactForge, 
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
		}
		return uri;
	}

	//######### Queries ##################	
	
	// ------- 1) Identify URIs based on regex search and scoring of results ------- 
	private List<String> getURICandidates(List<NamedEntity> entities){
		Long start = System.nanoTime();
		String queryString = "";		
		List<String> uri_candidates = new ArrayList<String>();
		
		// --- A) Per NamedEntity: Query for URI candidates ---
		ThreadGroup group = new ThreadGroup( source + "_" +  String.valueOf(entities.toString().hashCode()));
		boolean runQuery = false; 
		for (NamedEntity ne : entities) {
			if(uriCache.containsKey(getCacheRef(ne))){
				//Source specific cache based on NamedEntity name -> save regex queries
				uri_candidates.addAll(uriCache.get(getCacheRef(ne)));
				System.out.println(source + ": " + ne.getCacheRef() + " found in cache. Count: " + uriCache.get(getCacheRef(ne)).size());
			}else if(determineEntityTypeURI(source, ne.getType()) != ""){
//				if(isSPARQL10){ //SPARQL 1.0 // BIND/Alias causes error
//					queryString = "SELECT DISTINCT ?s ( STR(?l) as ?label ) ( 1 as ?count ) WHERE {"
//							+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, ne.getType()) + "."
//							+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
//							+ " ?s ?p ?o."
//							+ " FILTER(LANGMATCHES(LANG(?l), 'en') && isURI(?s) && regex(?l,'" + ne.getRegexName() + "') )"
////							+ " BIND(STR(?l) as ?label) BIND(1 as ?count)"
//							+ " }"
//							; 
//				}else{
					queryString = "SELECT DISTINCT ?s (STR(?l) as ?label) (COUNT(?p) AS ?count) WHERE {"
							+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + determineEntityTypeURI(source, ne.getType()) + "."
							+ " ?s <http://www.w3.org/2000/01/rdf-schema#label>  ?l."
							+ " ?s ?p ?o."
							+ " FILTER ((LANG(?l) = '' || LANGMATCHES(LANG(?l), 'en')) && isURI(?s) && regex(?l,'" + ne.getRegexName() + "') )"
//							+ " BIND (STR(?l) as ?label)"// BIND (COUNT(?p) as ?count)"
							+ " } GROUP BY ?s ?l"
							; 
//				}
				runQuery = true;
				System.out.println(source + ": Start query for " + ne.getCacheRef());
				new BackgroundQueryExecution(group, queryString, endpoint, ne, QueryType.SELECT).start();
			}
		}
		
		// --- B) Evaluate results of queries ---
		if(runQuery){
			try {
				BackgroundQueryExecution[] threads = new BackgroundQueryExecution[group.activeCount()];
				group.enumerate(threads);
				for (int i = 0; i < threads.length; i++) {
					threads[i].join();
					List<QuerySolution> res = threads[i].getSolutions();
					NamedEntity ne = threads[i].getNamedEntity();
					boolean hasError = threads[i].hasError();
					if(res != null && !res.isEmpty() && !hasError){
						//Results found -> if more than 5 -> extended logic						
						List<String> uris = new ArrayList<String>();
						if(res.size() <= 5){
							for (QuerySolution s : res) {
								if(s.contains("s")){
									uris.add(s.getResource("s").getURI());
								}else{
									System.out.println(source + ": WARNING - " + ne.getCacheRef() + ": Result contains wrong variable(s). Query: " + threads[i].getQueryString());
								}
							}
						}else{
							uris.addAll(findMostRelevantURIs(res, ne));						
						}	
						//URI candidate determination done -> store in cache and candidate list						
						uriCache.put(getCacheRef(ne), uris);
						uri_candidates.addAll(uris);
						System.out.println(source + ": Retrieved " + uris.size() + " URI candidate(s) for " + ne.getCacheRef() + ".");
					}else{
						//No match or error?
						if(hasError){
							System.out.println(source + ": ERROR - " + ne.getCacheRef() + ": " + threads[i].getErrorMsg());
							System.out.println(source + ": ERROR - " + ne.getCacheRef() + ": Query string: " + threads[i].getQueryString());
						}else{
							System.out.println(source + ": WARNING - " + ne.getCacheRef() + ": Result is empty. Query: " + threads[i].getQueryString());
						}							
					}
				}
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}
		System.out.println(source + ": Retrieved " + uri_candidates.size() + " URI candidate(s) in total. Time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms");
		return uri_candidates;
	}
	
	
	// ------- 2) Construct Model based on candidate URIs ------- 
	private void getContextModel(List<String> uri_candidates){
		Long start = System.nanoTime();	
		
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
		
		//Add owl:sameAs manually to ensure that it is retrieved (not used in output) 
		properties.add("http://www.w3.org/2002/07/owl#sameAs");
		
		String queryString = "";
		List<String> parts = new ArrayList<String>();
		
		//Available Properties
		parts.add(" VALUES (?p) { (<" + String.join(">) (<", properties) + ">)}"
				+ " VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				);
		
		//Direct Relations
		parts.add(" VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " VALUES (?o) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				);
		
		//Indirect Relations
		parts.add(" VALUES (?s) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " VALUES (?e) { (<" + String.join(">) (<", uri_candidates) + ">)}"
				+ " ?s ?p ?o."
				+ " ?e ?p2 ?o."
				+ " FILTER (?s != ?e)"
			    );
		
		
		//https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct
		
		ThreadGroup group = new ThreadGroup( String.valueOf(parts.toString().hashCode()));
		
		for (String part : parts) {
			queryString = "CONSTRUCT { ?s ?p ?o."
				+ " ?s <http://www.w3.org/2000/01/rdf-schema#label> ?ls."
				+ " ?p <http://www.w3.org/2000/01/rdf-schema#label> ?lp."
				+ " ?o <http://www.w3.org/2000/01/rdf-schema#label> ?lo."
				+ " } WHERE { "
				+ part
				+ " OPTIONAL {?s <http://www.w3.org/2000/01/rdf-schema#label> ?ls. FILTER (LANG(?ls) = '' || LANGMATCHES(LANG(?ls), 'en'))}"
				+ " OPTIONAL {?p <http://www.w3.org/2000/01/rdf-schema#label> ?lp. FILTER (LANG(?lp) = '' || LANGMATCHES(LANG(?lp), 'en'))}"
				+ " OPTIONAL {?o <http://www.w3.org/2000/01/rdf-schema#label> ?lo. FILTER (LANG(?lo) = '' || LANGMATCHES(LANG(?lo), 'en'))}"
//				+ " FILTER ( (LANG(?ls) = '' || LANGMATCHES(LANG(?ls), 'en')) "
//				+ " && (LANG(?lp) = '' || LANGMATCHES(LANG(?lp), 'en')) "
//				+ " && (LANG(?lo) = '' || LANGMATCHES(LANG(?lo), 'en')))"
				+ "}"
				;
			
			new BackgroundQueryExecution(group, queryString, endpoint, QueryType.CONSTRUCT).start();  
//			System.out.println(queryString);
		}		
		//--- Wait for results

		try {
			BackgroundQueryExecution[] threads = new BackgroundQueryExecution[group.activeCount()];
			group.enumerate(threads);
			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
				if(threads[i].getModel().size() > 0){
					this.model.add(threads[i].getModel());
				}else{
					System.out.println(source + ": WARNING - Construct query returned no result: " + threads[i].getQueryString());
				}
			}
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	
		System.out.println(source + ": Queried properties, relations and labels. Model size: " + model.size() + ". Time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms");
	}
	
	
	//######### Helper Methods ##################
	
	//consistent source specific key for cache
	private String getCacheRef(NamedEntity ne){
		return source + ne.getCacheRef();
	}
	
	private List<String> findMostRelevantURIs(List<QuerySolution> res, NamedEntity ne){
		//extended logic for URI candidate identification, if too many -> Score: string similarity and count of relations
		//Score each URI by its label and relations
		TreeMap<Double, List<String>> score_uris = new TreeMap<Double, List<String>>();
		List<String> uris = new ArrayList<String>();
		
		//ugly, but not efficient via external SPARQL: get max count of relations
		int max_cnt = 0;
		for (QuerySolution s : res) {
			if(s.getLiteral("count").getInt() > max_cnt)
				max_cnt = s.getLiteral("count").getInt();
		}
		
		//calc score for each uri: Similarity * Relation Score having range 0-1
		//Similariy: EditDistanc normalized to range 0-1 
		//Realtion Score: Relation Count / Max(Relation Count) -> range 0-1
		for (QuerySolution s : res) {
			String label = s.get("label").toString();
			//LOCATIONs have often a region information after a comma 
			if(ne.getType() == EntityType.LOCATION){
			    Matcher matcher = Pattern.compile("([^,]*)(,.*)*").matcher(label);
				if(matcher.find()){
					label = matcher.group(1);
//					System.out.println("Regex Gr. 1: " + label);
				}
			}
			Double score = stringSimilarity(label ,ne.getName()) * (s.getLiteral("count").getDouble()/max_cnt);
			if(score_uris.containsKey(score)){
				score_uris.get(score).add(s.getResource("s").getURI());
			}else{
				List<String> new_list = new ArrayList<String>();
				new_list.add(s.getResource("s").getURI());
				score_uris.put(score, new_list);
			}
		}						
		//Select at least top 5 scored URIs - but accept only -25% from top score 
		int cnt = 0;
		Double min_score = score_uris.descendingKeySet().first() * 0.75;
		for (Double key : score_uris.descendingKeySet()) {
			if(key < min_score)
				break;
			uris.addAll(score_uris.get(key));
			System.out.println(source + ": Score for " + ne.getCacheRef() + " of " + score_uris.get(key)+ ": " + key);
			cnt += score_uris.get(key).size();
			if(cnt >= 5)
				break;
		}	
		return (uris);
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
}
