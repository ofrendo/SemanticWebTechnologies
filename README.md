# SemanticWebTechnologies
Report for our 3rd semester SWT project

## Installation instructions for the Chrome extension

## Server
Bodies are in JSON.

### Get triples for a text
```
POST https://localhost:8443/RetrieveTriples

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

### Get available query properties
```
GET https://localhost:8443/RetrieveAvailableProperties

Response body:
[
  {
    "entityType": "Organization",
    "properties": [
      {"uri": "http://webprotege.stanford.edu/typeOfOrganisation",
       "label": "type of organisation",
       "id": "1583284149"},
      {"uri": "http://webprotege.stanford.edu/locatedIn",
       "label": "located in",
       "id": "116857677"},
      {"uri": "http://webprotege.stanford.edu/distributerOf",
       "label": "distributer of",
       "id": "1517547678"},
      {"uri": "http://webprotege.stanford.edu/foundedBy",
       "label": "founded by",
       "id": "1524249358"},
      {"uri": "http://webprotege.stanford.edu/homepage",
       "label": "homepage",
       "id": "165884424"},
      {"uri": "http://webprotege.stanford.edu/isPrimaryTopicOf",
       "label": "is primary topic of",
       "id": "607078104"},
      {"uri": "http://webprotege.stanford.edu/depiction",
       "label": "depiction",
       "id": "309789491"}
    ]
  },  
  {...}, 
  {...}
]

