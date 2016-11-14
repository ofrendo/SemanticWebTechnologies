package main.java.QueryEngine;

import java.util.List;
import java.util.Properties;

import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;


public interface QueryEngine {
	public Properties getAvailableProperties();
	public List<String> getAvailableProperties(EntityType type);
		
	public void queryEntities(List<NamedEntity> entities);
	public void queryEntities(List<NamedEntity> entities, QueryProperties props);
	
	public List<NamedEntity> getResultEntities();
	public List<String[]> getContextTriples();
}
