# SemanticWebTechnologies
Report for our 3rd semester SWT project

## API call
Bodies are in JSON.

Get triples for a text
```
POST https://semantic-web-technologies.herokuapp.com/RetrieveTriples

Request body:
{
	text: "This is a test to identify SAP in Walldorf with H. Plattner as founder."
}

Response body: 
[
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
]
```

