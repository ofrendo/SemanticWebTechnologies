package main.java.QueryEngine;

import java.util.List;

import org.apache.jena.rdf.model.Model;

import main.java.NEREngine.NamedEntity;

public class BackgroundSourceQueryHandler extends Thread {
	private QuerySource.Source s;
	private List<NamedEntity> entities;
	private Model m;
	private List<String> properties;


	public BackgroundSourceQueryHandler(ThreadGroup group, QuerySource.Source s ,List<NamedEntity> entities, List<String> properties){
		super(group,(s + "_" + entities));
		this.s = s;
		this.entities = entities;		
		this.m = null;
		this.properties = properties;
	}

	
	public void run(){
		m = new QuerySource(s, entities, properties).getModel();	
	}
	
	public Model getResultModel(){
		return m;
	}

	public List<NamedEntity> getEntities(){
		return entities;
	}
	
	public QuerySource.Source getSource(){
		return s;
	}

}
