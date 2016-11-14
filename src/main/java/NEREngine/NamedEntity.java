package main.java.NEREngine;

import java.util.HashMap;

public class NamedEntity {

	  private String name;
	  private EntityType type;
	  private HashMap<String,HashMap<String, Integer>> properties;
	  private String uri;

	  public String getName() {
	    return name;
	  }

	  public EntityType getType() {
	    return type;
	  }

	  public NamedEntity(String name, EntityType type) {
	    super();
	    this.name = name;
	    this.type = type;
	    this.properties = new HashMap<String,HashMap<String, Integer>>();
	    this.uri = "";
	  }
	  
	  public NamedEntity(NamedEntity template){
		  super();
		  this.name = template.getName();
		  this.type = template.getType();
		  this.properties = new HashMap<String,HashMap<String, Integer>>();
		  this.addProperties(template.getProperties());
		  this.uri = template.getURI();
	  }  
	  @Override
	  public boolean equals(Object o){
		  if(o != null && o.getClass() == NamedEntity.class){
			  NamedEntity ne = (NamedEntity)o;
			  if(ne.getName().equals(name) && ne.getType() == type){
				  return true;  
			  }
		  }
			  
		return false;
		  
	  }
	  
	  @Override
	  public String toString(){
		return type + " '" + name + "' URI: " + uri + "\n Properties: " + properties;
		  
	  }
	  
	  // Sample result should be:
	  /*{
			"entityName": "SAP",
			"entityType": "ORGANIZATION"
			"URI": "http://dbpedia.org/resource/SAP_SE",
			"properties": [
				{"name": "isPrimaryTopicOf", "value": ["http://en.wikipedia.org/wiki/SAP_SE"]},
				{"name": "foundedBy", "value": ["http://dbpedia.org/resource/Claus_Wellenreuther", 
										   "http://dbpedia.org/resource/Hasso_Plattner", 
										   "http://dbpedia.org/resource/Klaus_Tschira", 
										   "http://dbpedia.org/resource/Dietmar_Hopp"]},
				{"name": "depiction", "value": ["http://commons.wikimedia.org/wiki/Special:FilePath/SAP_2011_logo.svg"]},
				{"name": "homepage", "value": ["http://sap.com"]}
			]
		}*/
	  public String toJSONString() {
		  String result = "{\n" +
				 "  \"entityName\": \"" + name + "\",\n" +
				 "  \"entityType\": \"" + type + "\",\n" +
				 "  \"URI\": \"" + uri + "\",\n" + 
				 "  \"properties\": [";
		  
		  for (String key : properties.keySet()) {
			  HashMap<String, Integer> property = properties.get(key);
			  result += "\n    {\"name\": \"" + key + "\", \"value\": [";
			  // Keys in this hashmap are actually the values we want, not the ints
			  for (String value : property.keySet()) {
				  result += "\"" + value + "\",";
			  }
			  // remove last comma from properties
			  result = result.substring(0, result.length()-1);
			  result += "]},"; // close value and property
		  }
		  if (properties.keySet().size() > 0) {
			  // remove last comma
			  result = result.substring(0, result.length()-1);
		  }
		  result += "\n  ]"; // close properties
		  result += "\n}"; // close entity
		  
		  return result;
	  }
	  
	  
	  public String getRegexName(){
		  //return ("(^.{0,5}\\\\s+|^)" + name.replace(".", ".*") + "((\\\\s+.{0,5}$)|$)");
		  return ("(^.{0,10}\\\\s+|^)" + name.replace(".", ".*") + "((\\\\s+.{0,5}(\\\\(.*\\\\))?$)|$)");
	  }
	  
	  //add property values via copy
	  public void addProperties(HashMap<String,HashMap<String, Integer>> p){
		  for (String p_key : p.keySet()) {
			  for (String v_key : p.get(p_key).keySet()) {
				  addPropertyValue(p_key, v_key, p.get(p_key).get(v_key).intValue());
			  }			
		  }
	  }
	  
	  public void addPropertyValue(String p_key, String v_key, Integer count){
		  if(!properties.containsKey(p_key)){
			  //Add new property with initial value list
			  properties.put(p_key, new HashMap<String, Integer>());
		  }
		  if(properties.get(p_key).containsKey(v_key)){
			  //Sum old and new count of property value
			  properties.get(p_key).replace(v_key,properties.get(p_key).get(v_key).intValue() + count);
		  }else{
			  //Add new value to property
			  properties.get(p_key).put(v_key, count);
		  }
	  }
	  
	  public void setURI(String uri){
		  this.uri = uri;
	  }
	  
	  public String getURI(){
		 return this.uri;
	  }
	  
	  public HashMap<String,HashMap<String, Integer>> getProperties(){		  
		  //TODO copy necessary?
		  return properties;
	  }
	  
	  
	  public enum EntityType {
		    PERSON, 
		    ORGANIZATION, 
		    LOCATION
		    //, DATE
		}
}