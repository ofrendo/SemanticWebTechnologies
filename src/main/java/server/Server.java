package main.java.server;

import java.util.List;

import main.java.NEREngine.CoreNLPEngine;
import main.java.NEREngine.NEREngine;
import main.java.NEREngine.NamedEntity;
import main.java.QueryEngine.JenaEngine;

public class Server {

	public static String processRequest(String inputText) {
		System.out.println("Input text:");
		System.out.println(inputText);
		
		// 1) NER
		NEREngine engine = CoreNLPEngine.getInstance();
		List<NamedEntity> list = engine.getEntitiesFromText(inputText);
		System.out.println("===================");
		System.out.println("Result NER:");
		for (NamedEntity entity : list) {
	        System.out.println(entity.getType() + ": " + entity.getName());
		}
				
		// 2) Retrieve LOD information
		System.out.println("===================");
		System.out.println("Result LOD:");
		JenaEngine je = new JenaEngine();
		je.queryEntities(list, null);
		List<NamedEntity> results = je.getResultEntities();
		for (NamedEntity e : results){
			System.out.println(e);	
			//System.out.println(e.toJSONString());
		}
		String entitiesJSON = getEntitiesJSON(results);
		
		// 3) Retrieve context triples
		List<String[]> contextTriples = je.getContextTriples();
		String contextTriplesJSON = getContextTriplesJSON(contextTriples);
		
		// 4) Build result
		String result = "{\"entities\": " + entitiesJSON + ",\n" +
					     "\"contextTriples\": " + contextTriplesJSON + "\n}";
		
		
		return result;
	}
	
	private static String getEntitiesJSON(List<NamedEntity> entities) {
		String result = "[";
		for (NamedEntity e : entities) {
			result += e.toJSONString() + ",";
		}
		if (entities.size() > 0) {
			result = result.substring(0, result.length()-1); //remove last ,
		}
		
		result += "]";
		return result;
	}
	
	private static String getContextTriplesJSON(List<String[]> contextTriples) {
		String result = "[\n";
		for (String[] triple : contextTriples) {
			result += "{\n";
			result += "  \"subject\": \"" + triple[0] + "\",\n";
			result += "  \"predicate\": \"" + triple[1] + "\",\n";
			result += "  \"object\": \"" + triple[2] + "\"\n";
			result += "},";
		}
		if (contextTriples.size() > 0)
			result = result.substring(0, result.length() -1); //Remove last comma
		result += "]";
		return result;
	}
	
	public static void main(String[] args) {

		//String testInput = "This is a test to identify SAP in Walldorf with H. Plattner as founder.";
		String testInput = "The BBC's Sanjoy Majumder, in Delhi, says rescuers have recently brought out some survivors, including two children, which brought cheers from onlookers.";
		String output = processRequest(testInput);
		System.out.println(output);
	
	}

}
