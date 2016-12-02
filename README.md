# Entity Annotation
Report for our 3rd semester Semantic Web Technologies project

## Installation instructions 
### Server
The server is a Maven project. To compile a runnable .jar file, execute
```
mvn package 
```
in the project root folder. To start the server, run `startServerLocally.sh`.



### Chrome extension
To install the Chrome extension execute the following steps:
- Navigate to chrome://extensions/ (in Google Chrome)
- Make sure the "Developer mode" checkbox is ticked
- Click "Load unpackaged extension...", navigate to the `/SWTChromeExtension` folder and click OK


### Using the extension
To use the Chrome extension, first make sure the server is running. Then start the nginx server (`/nginx-1.10.2/nginx.exe`), which is responsible for creating HTTPS connections. Then navigate to 
```
https://localhost:8443/RetrieveAvailableProperties
```
to make sure everything is running. Chrome may show a warning about an insecure website, which is expected because the application uses self signed certificates. 

If the above request works, the extension is set up! Now navigate to any website. Mark some text and then look for a button named `Search for entities` in the top right. Click it and wait for a popup to show with the results. The first request may take a while (between 30s and a minute) because the CoreNLP models need to be loaded. 


## Server REST API documentation
All bodies are in JSON.

### Get triples for a text
```
POST https://localhost:8443/RetrieveTriples

Request body:
{
  "options": {
    "Organization": ["309789491", "165884424", "1524249358"],
    "Person": [],
    "Location: []
  },
  "input": "This is a test to identify SAP in Walldorf with H. Plattner as founder."
}

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

