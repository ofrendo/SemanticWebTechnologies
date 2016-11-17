# SemanticWebTechnologies
Report for our 3rd semester SWT project

## Installation instructions for the Chrome extension

## Server
Bodies are in JSON.

Get triples for a text
```
POST https://semantic-web-technologies.herokuapp.com/RetrieveTriples

Request body:
This is a test to identify SAP in Walldorf with H. Plattner as founder.

Response body: 
{
	"entities": [
		{
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
		},
		{...}, 
		{...}
	],
	"contextTriples": [
		{
			"subject": "abc",
			"predicate": "relation",
			"object": "bca"
		},
		{...},
		{...}
	]
```

