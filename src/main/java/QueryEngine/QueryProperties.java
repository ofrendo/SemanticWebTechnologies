package main.java.QueryEngine;

import java.util.List;
import java.util.Properties;

import main.java.NEREngine.NamedEntity.EntityType;

public class QueryProperties extends Properties {

	private static final long serialVersionUID = -3773584819674554288L;

	public QueryProperties() {
		super();	
	}

	public QueryProperties(Properties defaults) {
		super(defaults);
	}
	
	public synchronized void put(EntityType type, List<String> properties){
		super.put(type, properties);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized List<String> get(EntityType type) {
		try {
			return (List<String>) super.get(type);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	

}
