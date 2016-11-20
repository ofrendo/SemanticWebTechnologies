package main.java.QueryEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;


public interface QueryEngine {
	public Properties getAvailableProperties();
	public List<String> getAvailableProperties(EntityType type);
		
	public boolean queryEntities(List<NamedEntity> entities);
	public boolean queryEntities(List<NamedEntity> entities, QueryProperties props, List<QuerySource.Source> sources);
	
	public List<NamedEntity> getResultEntities();
	public List<String[]> getContextTriples();
}
