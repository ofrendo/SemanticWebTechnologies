var Connector = (function() {

	function retrieveTriples(inputText, callback) {
		// https://davidwalsh.name/fetch
		var url = CONFIG.DOMAIN +  "/RetrieveTriples";
		var request = new Request(url, {
			method: "POST", 
			mode: "cors", 
			body: inputText,
			headers: new Headers({
				'Content-Type': 'text/plain'
			})
		});

		fetch(request).then(callback);
	}



	var module = {};	
	module.retrieveTriples = retrieveTriples;

	return module;
})();