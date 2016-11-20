package main.java.QueryEngine;

import java.util.HashMap;
import java.util.List;

import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;


public interface QueryEngine {
	public HashMap<EntityType,List<QueryProperty>> getAvailableProperties();
	public List<QueryProperty> getAvailableProperties(EntityType type);
		
	public boolean queryEntities(List<NamedEntity> entities);
//	public boolean queryEntities(List<NamedEntity> entities, QueryProperties props, List<QuerySource.Source> sources);
	public boolean queryEntities(List<NamedEntity> entities, HashMap<EntityType,List<QueryProperty>> props, List<QuerySource.Source> sources);
	
	public List<NamedEntity> getResultEntities();
	public List<String[]> getContextTriples();
}
