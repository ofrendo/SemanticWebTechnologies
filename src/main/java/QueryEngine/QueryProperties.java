package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.java.NEREngine.NamedEntity.EntityType;

public class QueryProperties{
	
	private HashMap<EntityType, List<QueryProperty>> map;

	public QueryProperties() {
		//Initialize all possible lists
		map = new HashMap<EntityType, List<QueryProperty>>();
		for (EntityType et : EntityType.values()) {
			List<QueryProperty> list = new ArrayList<QueryProperty>();
			map.put(et, list);
		}
	}
	
	//Copy
	public QueryProperties(QueryProperties template) {
		this();
		for (EntityType et : EntityType.values()) {
			List<QueryProperty> list = new ArrayList<QueryProperty>();
			list.addAll(template.get(et));
			map.get(et).addAll(list);
		}
	}
	
	public List<QueryProperty> get(EntityType et){
		return map.get(et); 
	}
	
	//remove by uri
	public boolean remove(EntityType et, String uri){
		QueryProperty remove = null;
		for (QueryProperty qp : map.get(et)) {
			if(qp.getUri().equals(uri))
				remove = qp;				
		}	
		return this.remove(et, remove);		
	}
	
	//remove by object
	public boolean remove(EntityType et, QueryProperty qp){
		return map.get(et).remove(qp);		
	}
	
	public boolean removeById(EntityType et, String id){
		QueryProperty remove = null;
		for (QueryProperty qp : map.get(et)) {
			if(qp.getId().equals(id))
				remove = qp;				
			}	
		return this.remove(et, remove);		
  	}
	
	
	public void put(EntityType et, List<QueryProperty> props) {
		map.get(et).addAll(props);
	}
	
	public String toString() {
		String s = "";
		for (EntityType et : map.keySet()) {
		   s += et + ": " + map.get(et) + "\n";	
		}
		return s;
	}
	
	public String toJSONString() {
		 String result = "[\n";
		 for (EntityType et : map.keySet()) {
			 result += "  {\n";
			 result += "    \"entityType\": \"" + et.getName() + "\",\n";
			 result += "    \"properties\": [\n";
			 
			 for (QueryProperty q : map.get(et)) {
				 result += q.toJSONString();
				 result += ",\n";
			 }
			 //Remove last comma
			 result = result.substring(0, result.length() -2);
			 result += "\n    ]";
			 result += "\n  },";
		 }
		 // Remove last comma
		 result = result.substring(0, result.length() -1);
		 result += "]";
		 
		 return result;
	}
}
