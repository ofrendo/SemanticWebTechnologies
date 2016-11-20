package main.java.QueryEngine;

public class QueryProperty {

	private String uri;
	private String label;
	private String id;
	
	
	public QueryProperty(String uri, String label){
		this(uri, label, null);
	}
	
	public QueryProperty(String uri, String label, String id){
		this.uri = uri;
		this.label = label;
		if(id == null)
			id = String.valueOf(Math.abs(this.hashCode())); //id is used in queries 
		this.id = id;
	}

	public String getUri() {
		return uri;
	}	
	
	public String getLabel() {
		return label;
	}
	
	public String getId() {
		return id;
	}
	
	public String toString(){
		return label;
	}
	
	public boolean equals(Object o){
		  if(o != null && o.getClass() == QueryProperty.class){
			  QueryProperty qp = (QueryProperty)o;
			  if(qp.getUri().equals(uri)){
				  return true;  
			  }
			  //For search in List via String (eg remove)
		  }else if(o != null && o.getClass() == String.class){
			  String s = (String)o;
			  if(s.equals(uri)){
				  return true;  
			  }
		  }
			  
		return false;
	}
	public int hashCode() {		 
		  return uri.hashCode();
	}
}
