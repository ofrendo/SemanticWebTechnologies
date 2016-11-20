package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.java.NEREngine.NamedEntity.EntityType;

public class QueryProperties{
	
	private HashMap<EntityType, List<QueryProperty>> map;

	public QueryProperties() {
		map = new HashMap<EntityType, List<QueryProperty>>();
		for (EntityType et : EntityType.values()) {
			List<QueryProperty> list = new ArrayList<QueryProperty>();
			map.put(et, list);
		}
	}
	
	//Copy
	public QueryProperties(QueryProperties template) {
		map = new HashMap<EntityType, List<QueryProperty>>();
		for (EntityType et : EntityType.values()) {
			List<QueryProperty> list = new ArrayList<QueryProperty>();
			list.addAll(template.get(et));
			map.put(et, list);
		}
	}
	
	public List<QueryProperty> get(EntityType et){
		return map.get(et); 
	}
	
	public boolean remove(EntityType et, String uri){
		QueryProperty remove = null;
		for (QueryProperty qp : map.get(EntityType.LOCATION)) {
			if(qp.getUri().equals(uri))
				remove = qp;				
		}	
		return map.get(EntityType.LOCATION).remove(remove);		
	}
	
	public String toJSONString() {
		 //TODO: Olli
		 return null;
	}
}
