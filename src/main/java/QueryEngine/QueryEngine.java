package main.java.QueryEngine;

import java.util.List;

import main.java.NEREngine.NamedEntity;


public interface QueryEngine {
	public QueryProperties getAvailableProperties();
//	public List<QueryProperty> getAvailableProperties(EntityType type); -> use getAvailableProperties().get(EntityType)
		
	public boolean queryEntities(List<NamedEntity> entities);
	public boolean queryEntities(List<NamedEntity> entities, QueryProperties props, List<QuerySource.Source> sources);
	
	public List<NamedEntity> getResultEntities();
	public List<String[]> getContextTriples();
}
