/**
 * 
 */
package main.java.QueryEngine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;

import main.java.NEREngine.CoreNLPEngine;
import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;


/** 
 * @author Sascha Ulbrich
 *
 */
public class JenaEngine implements QueryEngine {	
	private static OntModel ontoModel;	
	private static QueryProperties availableProperties;
	private static final String PREFIX = ":";
	
	private Model model; //Raw input from source
	private InfModel infModel; //Infered model -> combination of raw model and ontology
	private List<NamedEntity> entities;
	private QueryProperties properties;
	private List<QuerySource.Source> sources; 
	
	
	
	//######################### Public methods: Interface ##########################################
	
	public JenaEngine() {		
		if(ontoModel == null){
			ontoModel = loadLocalOntology();
		}
		if(availableProperties == null){
			availableProperties = readAvailableProperties();
		}		
	}	
	

	/* (non-Javadoc)
	 * @see QueryEngine.QueryEngine#getAvailableProperties()
	 */
	@Override
	public QueryProperties getAvailableProperties() {
		//If properties manipulated later -> by reference is bad -> deep copy needed	
		return new QueryProperties(availableProperties);
	}


	/* (non-Javadoc)
	 * @see QueryEngine.QueryEngine#queryEntityProperties(java.util.List)
	 * Query properties with full set of available properties
	 */
	@Override
	public boolean queryEntities(List<NamedEntity> entities) {
		return queryEntities(entities, null, null);
	}

	/* (non-Javadoc)
	 * @see QueryEngine.QueryEngine#queryEntityProperties(java.util.List, java.util.Properties)
	 * Query properties with custom set of properties
	 */
	@Override 
	public boolean queryEntities(List<NamedEntity> entities, QueryProperties props, List<QuerySource.Source> sources) {
		if(entities == null || entities.isEmpty()){
			System.out.println("ERROR - No named entities provided!");
			return false;
		}
		if(props == null){
			props = availableProperties;
		}
		if(sources == null){
			//Default source selection
			//http://sparqles.ai.wu.ac.at/
			//Stability -> host own SPARQL endpoint.... 
			sources = new ArrayList<QuerySource.Source>();
			sources.add(QuerySource.Source.DBPedia);
			sources.add(QuerySource.Source.DBPediaLive);
//			sources.add(QuerySource.Source.FactForge); //timeouts
//			sources.add(QuerySource.Source.EEA); //SPARQL 1.0?
//			sources.add(QuerySource.Source.LinkedMDB); //SPARQL 1.0! 
//			sources.add(QuerySource.Source.Education_UK); //Slow
//			sources.add(QuerySource.Source.DataGovUk); //only internal references... nothing we can use
			sources.add(QuerySource.Source.IServe);
//			sources.add(QuerySource.Source.WorldBank); //No rdfs:label! Instead http://www.w3.org/2004/02/skos/core#prefLabel
//			sources.add(QuerySource.Source.YAGO2); //by far slower as DBPedia
//			sources.add(QuerySource.Source.LOB); //SPARQL 1.0 (virtuoso 6.1.3)!  
		}
		
		//add copies of entities to ensure that list cannot be change from outside
		this.entities = copyList(entities);
		
		this.sources = sources;
		this.properties = props;
		this.model = ModelFactory.createDefaultModel();
		
		//Query Sources to build model
		handleParallelSourceQueries();
		if(model.size() < 1){
			System.out.println("ERROR - No matching URIs found across all selected sources!");
			return false;
		}
		
		//Query local model if Entities could be identified
		handleLocalQueries();
		
		return true;		
	}
	
	@Override
	public List<NamedEntity> getResultEntities(){
		return copyList(this.entities);
	}
	
	@Override
	public List<String[]> getContextTriples(){
		if(model != null && model.size() > 0){
			return queryContextTriples(model); //independent of own ontology
		}else{
			return new ArrayList<String[]>();
		}
	}	
	


	//######################### Private methods doing actual work ##########################################
	private List<NamedEntity> copyList(List<NamedEntity> entities){
		List<NamedEntity> copy = new ArrayList<NamedEntity>();
		if(entities != null){
			for (NamedEntity ne : entities) {
				copy.add(new NamedEntity(ne));
			}
		}
		return copy;		
	}
	

	private OntModel loadLocalOntology() {
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		//Load local Ontology from file
		InputStream in = FileManager.get().open("data/UMA-SWT-HWS16.owl");
		try {
			m.read(in,null);
		} catch (Exception e) {
			System.out.println("Error during ontology import: " + e.getMessage());
		}		
		System.out.println("Loaded local Ontology of size: " + m.size());
		return m;
	}

	private void handleParallelSourceQueries() {
		
		System.out.println("Start load from sources...");
		Long start = System.nanoTime();
		
		//Query sources in parallel 
		ThreadGroup group = new ThreadGroup( entities.toString());
		for (QuerySource.Source s : sources) {
			new QuerySource(group, s, entities, getQueryProperties()).start();
		}
		
		//Wait till all are finished and derive model
		try {
//			QuerySource[] threads = new QuerySource[group.activeCount()];
			QuerySource[] threads = new QuerySource[sources.size()];
			group.enumerate(threads, false);
			for (int i = 0; i < threads.length; i++) {
				if(threads[i] != null){
					threads[i].join();
					model.add(threads[i].getModel()); //Fallback for query errors is empty model
				}
			}
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Load of Sources finished. Model size: " + model.size()+ "; Time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms");
	}
	
	//Derive all possibly needed properties based on own ontology definition
	private List<String> getQueryProperties(){
		
		List<String> props = new ArrayList<String>();
		
		Query query = QueryFactory.create(" SELECT ?p WHERE { ?a <http://www.w3.org/2002/07/owl#equivalentProperty> ?p. }"); 
		QueryExecution qe = QueryExecutionFactory.create(query, ontoModel); 
		ResultSet results = qe.execSelect(); 
		
		while(results.hasNext()) {  
			QuerySolution sol = results.next();  
			props.add(sol.get("p").toString());
		}			
		qe.close();
		
		return props;
	}

	private void handleLocalQueries() {

		//Construct inference model (Ontology + loaded triples) 
		//-> count (indirect) relations between entities and choose most relevant entities
		//System.out.println("Relevant URIs in Context: " + relevantURIs);
		infModel = ModelFactory.createInfModel( ReasonerRegistry.getOWLMicroReasoner(), ModelFactory.createUnion(model, ontoModel));
		
		//Set URI for each entity
		deriveRelevantURIs(this.infModel);
					
		//query each entity separately on local model				
		for (NamedEntity e : entities) {
			if (e.getURI() != null && e.getURI().length() > 0){
				//construct dictionary for entity type specific properties 
//				prepareProperties(properties.get(e.getType()));
				
				//Construct local query
				String lq = constructLocalQuery(properties.get(e.getType()), e.getURI());		
			
				//Execute Query
				//result.add(executeLocalQuery(lq,propDic,cmodel, e));
				executeLocalQuery(lq,properties.get(e.getType()),infModel, e);	
			}else{
				System.out.println(model);
			}
			
		}
	
	}
	
	// ------- Prepare properties -> assign indices for variable to technical property names
//	private Hashtable<String, String> prepareProperties(List<String> props) {
//		Hashtable<String, String> properties = new Hashtable<String, String>();
//		int i = 1;
//		for (String prop : props) {
//			properties.put(prop, Integer.toString(i));
//			i++;
//		}
//		return properties;
//	}
		
	// ------- Construct local query: incl. dynamic list of properties
	private String constructLocalQuery(List<QueryProperty> props, String uri) {
		String queryString = "";
		String res = "<" + uri + ">";
		
		// ---- Construct Query ----------
		// 1) Prefix (only basics and the own prefix for local queries)
		
		queryString += "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
				+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
				+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ " PREFIX " + PREFIX + " <http://webprotege.stanford.edu/>"
				;
		
		// 2) Select Clause
		queryString += " SELECT ?l";
		for (QueryProperty prop: props) {
			queryString += " ?" + prop.getId();
		}	
		// 3) Where Clause	
		// 3a) static part
		queryString += " WHERE {"
				+ res + " rdfs:label ?l."
			;
		// 3b) dynamic part
		for (QueryProperty prop: props) {
//			queryString += " OPTIONAL { " + res + " " + PREFIX + entry.getKey() + " ?" + entry.getValue() + ". }";
			queryString += " OPTIONAL { " + res + " <" + prop.getUri() + "> ?" + prop.getId() + ". }";
		}

		// 3c) Filter
		queryString += " FILTER(LANG(?l) = '' || LANGMATCHES(LANG(?l), 'en'))";
		
		// 3d) String conversion
		queryString += " BIND( str(?l) as ?label )"
				+ "}"; 
		
		return queryString;
	}
	
	private String deriveEntityClasses(EntityType et) {
		String type = PREFIX;
		switch (et) {
		case ORGANIZATION:
			type += "Organisation";
			break;
		case PERSON:
			type += "Person";
			break;
		case LOCATION:
			type += "Location";
			break;
		}
		return type;
	}


	// ------- Handle local query execution
	private void executeLocalQuery(String query, List<QueryProperty> propDic, Model m, NamedEntity ne){
		
		//label is always present and has special logic -> enhance property dictionary just for reading them 
		List<QueryProperty> enhDic = new ArrayList<QueryProperty>();
		enhDic.addAll(propDic);
		enhDic.add(new QueryProperty("label", "label"));
		
		//Parse Query
//		System.out.println(query);
		Query q = QueryFactory.create(query); 
//		System.out.println(q);
		
		//Execute Query
		Long start = System.nanoTime();
		QueryExecution qe = QueryExecutionFactory.create(q, m);
		ResultSet RS = qe.execSelect();
		
		
		//Parse Result of Query
		while (RS.hasNext()) {
			QuerySolution tuple = RS.next();
			handleQueryTuple(tuple, enhDic, ne);
		}
		System.out.println("Queried local model in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start) + "ms, size: " + RS.getRowNumber());
		qe.close();
	}
	
	// ------- Per Entity: Determine most relevant URI across sources
	private void deriveRelevantURIs(Model m) {
		
		for (NamedEntity e : entities) {	
			
			// ---- Derive values ----
			// rdf:type 
			String type = deriveEntityClasses(e.getType());
			
			// rdf:label Regex
			String name = e.getRegexName();
			
			//construct filter part (same for every part of the union)
			String filter = "(LANG(?l1) = '' || LANGMATCHES(LANG(?l1), 'en'))"
					+ " && (LANG(?l2) = '' || LANGMATCHES(LANG(?l2), 'en'))"
					+ " && regex(?l1,'" + name + "')"
					;
			
			//Add context info to filter, if context available
			if(entities.size() > 1){
				//derive the other entities in the context 
				String others = "";
				for (NamedEntity e2 : entities) {
					if(e2.getName() != e.getName()){
						if(others != ""){
							others += " || ";
						}
						others += "regex(?l2,'" + name + "')";
					}
				}
				//add to filter
				filter += " && ( " + others + " ) ";
			}
					
			
			// Union part 1: direct relations
			String part1 = "SELECT ?e1 ?p1  WHERE {"
					+ " ?e1 ?p1 ?e2."
					+ " ?e1 rdfs:label ?l1."
					+ " ?e2 rdfs:label ?l2."
					+ " ?e1 rdf:type " + type + "."
					+ " FILTER ( " + filter + " ) }";
			
			// Union part 2: indirect relations
			String part2 = "SELECT ?e1 ?p1  WHERE {"
					+ " ?e1 ?p1 ?o."
					+ " ?e2 ?p2 ?o."
					+ " ?e1 rdfs:label ?l1."
					+ " ?e2 rdfs:label ?l2."
					+ " ?e1 rdf:type " + type + "."
					+ " FILTER (" + filter + ")}";
			
			// Complete Query
			String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ " PREFIX " + PREFIX + " <http://webprotege.stanford.edu/>"
					+ " SELECT ?e1 (count(?p1) as ?pCount) { { "
					+ part1
					+ " } UNION { " 
					+ part2
					+ " } } GROUP BY ?e1"
					; 
			//System.out.println(queryString);
			Query query = QueryFactory.create(queryString); 
			//System.out.println(query);
			QueryExecution qe = QueryExecutionFactory.create(query, m); 
			ResultSet results = qe.execSelect(); 
			int max = 0;
			String value = "";
			while(results.hasNext()) {  
				QuerySolution sol = results.next(); 
				//System.out.println(sol);
				if( sol.contains("pCount") && sol.get("pCount").asLiteral().getInt() > max){
					max = sol.get("pCount").asLiteral().getInt();
					value = sol.get("e1").toString();
				}
			}
			e.setURI(value);
			qe.close();
		}
	}


	// ------- Parse Tuple of local query result: based on Entity  
	private void handleQueryTuple(QuerySolution tuple, List<QueryProperty> propDic, NamedEntity ne) {
		String v = "";
		
		//handle dynamic properties
		for (QueryProperty prop: propDic) {
			v = new String();
			if(tuple.contains(prop.getId())){
				RDFNode node = tuple.get(prop.getId());
				if(node.isLiteral()){
					if (node.asLiteral().getLanguage() != null && !node.asLiteral().getLanguage().equals("en") && !node.asLiteral().getLanguage().equals("")) {
//						System.out.println("Skipped language literal: " + node.asLiteral().getLanguage());
						continue;
					}
					
					v = node.asLiteral().getString();
					if(isNumeric(v) && v.contains(".")){ //double
						v = String.format("%,.2f", Double.parseDouble(v)) + " (" + node.asLiteral().getDatatypeURI() + ")";
					}
				}else{
					v = node.toString();
				}
				ne.addPropertyValue(prop, v, 1);				
			}
		}
		
	}
	
	private boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}


	// ------- read available Properties via local Ontology
	private QueryProperties readAvailableProperties(){
		QueryProperties queryprops = new QueryProperties();
		
		Model m = ModelFactory.createRDFSModel(ontoModel);
		List<QueryProperty> props;
		for (EntityType et : EntityType.values()) {
			props = new ArrayList<QueryProperty>();
			
			String type = deriveEntityClasses(et);
			
			//Query available properties from local Ontology
			String queryString = " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ " PREFIX " + PREFIX + " <http://webprotege.stanford.edu/>"
					+ " SELECT ?p ?label"
					+ " WHERE {"
					+ " ?p rdfs:domain "+ type +"."
					+ " ?p rdf:type rdf:Property."
					+ " ?p rdfs:label ?l."
					+ " FILTER(LANG(?l) = '' || LANGMATCHES(LANG(?l), 'en'))"
					+ " bind( str(?l) as ?label )"
							+ "}"
					; 
			Query query = QueryFactory.create(queryString); 
			//System.out.println(query);
			QueryExecution qe = QueryExecutionFactory.create(query, m); 
			ResultSet results = qe.execSelect(); 
			while(results.hasNext()) {  
				QuerySolution sol = results.next();  
				String label = sol.get("label").toString();
				String uri = sol.getResource("p").getURI();
				
				props.add(new QueryProperty(uri, label));
			}			
			qe.close();
			queryprops.put(et, props);
		}
		return queryprops;
	}
	
	private List<String[]> queryContextTriples(Model m) {
		List<String[]> result = new ArrayList<String[]>();
		
		//Add filter for URI 
		String filter_e1 = "";
		String filter_e2 = "";
		String filter_o = ""; // "o"ther entity for indirect relation
		for (NamedEntity e : entities) {			
			if(filter_e1 != ""){
				filter_e1 += " || "; 
				filter_e2 += " || "; 
				filter_o += " || ";
			}
			filter_e1 += " ( ?e1 = <" + e.getURI() + "> )";
			filter_e2 += " ( ?e2 = <" + e.getURI() + "> )";
			filter_o += " ( ?o = <" + e.getURI() + "> )";
		}
		
		String filterLang = " && LANGMATCHES(LANG(?le1), 'en') && LANGMATCHES(LANG(?le2), 'en') && LANGMATCHES(LANG(?lp), 'en')"; 
		
		String filter1 = " ( " + filter_e1 + " ) && ( " + filter_e2 + " ) "	+ filterLang;		
		String filter2 = " ( " + filter_e1 + " ) && ( " + filter_o + " ) && ?e1 != ?o " + filterLang;
		
		String bind = " BIND (STR(?le1) as ?l_e1) BIND (STR(?le2) as ?l_e2) BIND (STR(?lp) as ?l_p)";
		
		
		// Union part 1: direct relations
		String part1 = "SELECT DISTINCT ?l_e1 ?l_p ?l_e2  WHERE {"
				+ " ?e1 ?p ?e2."
				+ " ?e1 rdfs:label ?le1."
				+ " ?e2 rdfs:label ?le2."
				+ " OPTIONAL {?p rdfs:label ?lp.}"
				+ " FILTER ( " + filter1 + " ) "
				+ bind
				+ "}";
		
		// Union part 2: indirect relations
		String part2 = "SELECT DISTINCT ?l_e1 ?l_p ?l_e2  WHERE {"
				+ " ?e1 ?p ?e2."
				+ " OPTIONAL { ?o ?p2 ?e2 }"
				+ " OPTIONAL { ?e2 ?p3 ?o }"
				+ " ?e1 rdfs:label ?le1."
				+ " ?e2 rdfs:label ?le2."
				+ " OPTIONAL { ?p rdfs:label ?lp.}"
				+ " FILTER ( " + filter2 + " ) "
				+ bind
				+ "}";
		
		// Complete Query
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ " PREFIX " + PREFIX + " <http://webprotege.stanford.edu/>"
				+ " SELECT DISTINCT ?l_e1 ?l_p ?l_e2 { { "
				+ part1
				+ " } UNION { " 
				+ part2
				+ " } } "
				; 
		//System.out.println(queryString);
		Query query = QueryFactory.create(queryString); 
		//System.out.println(query);
		QueryExecution qe = QueryExecutionFactory.create(query, m); 
		ResultSet results = qe.execSelect(); 

		String[] triple;
		while(results.hasNext()) {  
			QuerySolution sol = results.next(); 
			//System.out.println(sol);
			if( sol.contains("l_e1") && sol.contains("l_e2") && sol.contains("l_p")){
				triple = new String[3];
				triple[0] = sol.get("l_e1").toString();				
				triple[1] = sol.get("l_p").toString();
				triple[2] = sol.get("l_e2").toString();				
				result.add(triple);
			}
		}
		qe.close();
		
		return result;
	}
	
	
	// #################################### TEST SECTION #################################################
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//  ---- End-to-End Test		
		
		String text = "";
		
		//test for education UK
//		List<QuerySource.Source> sources = new ArrayList<QuerySource.Source>();
//		sources.add(QuerySource.Source.DBPedia);
//		sources.add(QuerySource.Source.DBPediaLive);
//		sources.add(QuerySource.Source.Education_UK);
//		text = "Is the Headley Park Primary School somehow related to London or Bristol?";
//		runtest(text,null,sources);
		
		//text = "The BBC's Sanjoy Majumder, in Delhi, says rescuers have recently brought out some survivors, including two children, which brought cheers from onlookers.";
//		text = "She told the Times of India that most of the people travelling with her had been found but that her father was still missing."; // No Organization!
//		runtest(text);
		
//		text = "She told the german Spiegel that most of the people travelling with her had been found but that her father was still missing.";
//		runtest(text);
		
//		text = "Jonas joins the Brown University?";
//		text = "Michael Sherwood quits Goldman Sachs role";
//		runtest(text);
		
//		//1st simple test with all entity types
//		text = "This is a test to identify SAP in Walldorf with H. Plattner as founder.";
//		runtest(text);
		
//		text = "Michael Sherwood quits Goldman Sachs role";
//		runtest(text);
		
//		text = "Mr Trump has said Japan needs to pay more to maintain US troops on its soil.";
//		runtest(text);
		
//		text = "Mr Abe is stopping in New York on his way to an Asia-Pacific trade summit in Peru.";
//		runtest(text);
		
		// This text takes too long - need limit of stuff somewhere
		//text = "The Kremlin revealed Mr Trump and Mr Putin had discussed Syria and agreed that current Russian-US relations were \"extremely unsatisfactory\"";
		//runtest(text);
		
//		text = "Mr Putin and Mr Trump agreed to stay in touch by phone in Russia, and arrange to meet in person at a later date, the Kremlin added.";
//		runtest(text);
		
//		text = "Germany is a country.";
//		runtest(text);
//		text = "Russia is a country as well.";
//		runtest(text);
		
//		text = "Russia is a country having a lot relations to Germany or even Syria.";
//		runtest(text);
//		
//		text = "Michael Gove, Iain Duncan Smith and Theresa Villier are among her backers.";
//		runtest(text);
		
		text = "Is the Rossland Beer Company  producing beer?";
		runtest(text);
		
//		//2nd TEST (just hit the cache)
//		text = "Just testing how caching works for H. Plattner from Walldorf.";
//		runtest(text);
//		
//		// 3rd TEST (Cache and remove property)
//		JenaEngine je = new JenaEngine();
//		text = "This is a test to identify if Walldorf is in cache but Heidelberg has to be queried";
//		QueryProperties props = je.getAvailableProperties();
//		
//		String uri = "";
//		for (QueryProperty qp : props.get(EntityType.LOCATION)) {
//			if(qp.getLabel().equals("depiction"))
//				uri = qp.getUri();				
//		}	
//		boolean b = false;
//		if(uri != "")
//			b = props.remove(EntityType.LOCATION,uri);
//		System.out.println("Removal: " + b);
//		System.out.println("Available Props: " + je.getAvailableProperties());
//		
//		
//		runtest(text,props,null);
		
		
//		// 4th TEST: Heikos example
//		//some trouble with special characters
//		text = "Zu den verdaechtigen gehört Walter K., ein ehemaliger Fußballprofi aus Stuttgart. "
//				+ "K. spielte zweitweise sogar in der deutschen Nationalmannschaft, nach seiner Karrier "
//				+ "betrieb er für die Allianz ein Versicherungsbüro.";
//		runtest(text);
//		
//		// 4th TEST: Heikos example in english
//		//some trouble with special characters rertieved through stuttgart
//		text = "The suspect Walter K. is a former soccer player from Stuttgart. "
//				+ "After his carrer he had a insurance office for Allianz.";
//		runtest(text);
		

		
		// ----- Simple test without NER
//		List<NamedEntity> list = new ArrayList<NamedEntity>();
//		list.add(new NamedEntity("SAP", EntityType.ORGANIZATION));
//		list.add(new NamedEntity("Walldorf", EntityType.LOCATION));
//		
//		
//		JenaEngine je = new JenaEngine();
//		List<QueryProperty> qp = je.getAvailableProperties();				
////		qp.get(EntityType.LOCATION).remove("depiction");
//		//qp.get(EntityType.ORGANIZATION).remove("distributerOf");
//		
//		System.out.println(je.getAvailableProperties());
//		System.out.println(qp);
//		
//		je.queryEntities(list,qp);
//		System.out.println(je.getResultEntities());
//		for (String[] a : je.getContextTriples()) {
//			System.out.println(a[0] + " - " + a[1] + " - " + a[2]);
//		}
//				
		
		/*
		// ----- Test property derivation
		JenaEngine je = new JenaEngine();
		System.out.println(je.getAvailableProperties(EntityType.LOCATION));
		*/
	}
	private static void runtest(String text) {
		runtest(text, null, null);
	}

	private static void runtest(String text, QueryProperties qp, List<QuerySource.Source> sources) {
		// 1) NER
		List<NamedEntity> list = CoreNLPEngine.getInstance().getEntitiesFromText(text);
		System.out.println("Result NER:");
		for (NamedEntity entity : list) {
	        System.out.println(entity.getType() + ": " + entity.getName());
		}
				
		// 2) Retrieve LOD information
		System.out.println("===================");
		System.out.println("Result LOD:");
		JenaEngine je = new JenaEngine();
		je.queryEntities(list, qp, sources);
		for (NamedEntity e : je.getResultEntities()){
			System.out.println(e);	
			System.out.println(e.toJSONString());
		}
		
		System.out.println("===================");
		System.out.println("Triples of context:");
		for (String[] a : je.getContextTriples()) {
			System.out.println(a[0] + " - " + a[1] + " - " + a[2]);
		}
	}
}
