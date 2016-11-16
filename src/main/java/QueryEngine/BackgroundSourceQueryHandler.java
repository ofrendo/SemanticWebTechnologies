package main.java.QueryEngine;

import java.util.List;

import org.apache.jena.rdf.model.Model;

import main.java.NEREngine.NamedEntity;
import main.java.NEREngine.NamedEntity.EntityType;

public class BackgroundSourceQueryHandler extends Thread {
	private QuerySource.Source s;
	private EntityType et;
	private List<NamedEntity> entities;
	private Model m;
	private List<String> properties;
//	private List<String> cacheRef;
//	private String filter;


	public BackgroundSourceQueryHandler(ThreadGroup group, QuerySource.Source s, EntityType et ,List<NamedEntity> entities, List<String> properties){
		super(group,(s + "_" + entities));
		this.s = s;
		this.et = et;
		this.entities = entities;		
		this.m = null;
		this.properties = properties;
	}

	
	public void run(){
//		switch (s) {
//		case DBPedia:
//			m = new QueryDBPedia(et, entities).getModel();
//			break;
//		default:
//			break;
//		}
		if(et == null){
			m = new QuerySource(s, entities, properties).getModel();
		}else{
			m = new QuerySource(s, et, entities).getModel();
		}
		
	}
	
	public Model getResultModel(){
		return m;
	}
	
//	public List<String> getCacheRef(){
//		return cacheRef;
//	}
	
	public List<NamedEntity> getEntities(){
		return entities;
	}
	
	public QuerySource.Source getSource(){
		return s;
	}

}
