package main.java.server;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import main.java.NEREngine.CoreNLPEngine;
import main.java.NEREngine.NEREngine;
import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;
import main.java.QueryEngine.JenaEngine;
import main.java.QueryEngine.QueryProperties;
import main.java.QueryEngine.QueryProperty;

public class Server {

	public static String processRequest(String body) {
		System.out.println("Body:");
		System.out.println(body);
		
		JSONObject obj = new JSONObject(body);
		JSONObject options = obj.getJSONObject("options");
		String inputText = obj.getString("input");		
		
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
		QueryProperties qp = getQueryPropertiesFromInput(options, je);
		
		je.queryEntities(list, qp, null);
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
	
	private static QueryProperties getQueryPropertiesFromInput(JSONObject options, JenaEngine je) {
		QueryProperties allProperties = je.getAvailableProperties();
		QueryProperties qp = new QueryProperties(allProperties); // deep copy TODO @sascha is this needed?
		for (EntityType et: EntityType.values()) {
			if (!options.has(et.getName())) {
				// If no options passed for this entitytype, want all
				continue;
			}
			JSONArray values = options.getJSONArray(et.getName());
			if (values.length() == 0) {
				// If no values passed in options, want all
				continue;
			}
			else {
				System.out.println("-- Reducing queryProperties for "+ et.getName() + "... --");
				// Else take only IDs contained in options
				List<String> toRemove = new ArrayList<String>();
				List<QueryProperty> entityQueryProperties = qp.get(et);
				int previousSize = entityQueryProperties.size();
				for (int i=0;i<entityQueryProperties.size();i++) {
					QueryProperty q = entityQueryProperties.get(i);
					boolean isContained = false; // check if this particular q is in options or not
					
					for (int j=0;j<values.length();j++) {
						String optionsId = values.getString(j);
						if (optionsId.equals(q.getId())) {
							isContained = true;
						}
					}
					
					if (isContained == false)
						toRemove.add(q.getId());
				}
				for (String id : toRemove) {
					qp.removeById(et, id);
				}
				System.out.println("-- Only using " + entityQueryProperties.size() + "/" + previousSize + " properties for " + et.getName() + " --");
			}
		}
		return qp;
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

		String testInput = "{"
				+ "  \"options\": {\"Organization\": [\"309789491\", \"165884424\", \"1524249358\"]}, " // depiction, homepage, founded by
				+ "  \"input\": \"This is a test to identify SAP in Walldorf with H. Plattner as founder.\""
				+ "}";
		//String testInput = "The BBC's Sanjoy Majumder, in Delhi, says rescuers have recently brought out some survivors, including two children, which brought cheers from onlookers.";
		String output = processRequest(testInput);
		System.out.println(output);
	
	}

}
