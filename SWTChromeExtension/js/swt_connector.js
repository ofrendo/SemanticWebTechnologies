var Connector = (function() {
	var loading = false;

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

		if (loading === false) {
			log("Retrieving entities for \"" + inputText + "\"...");
			loading = true;
			var buttonSearch = document.getElementById("swtButtonSearch");
			buttonSearch.innerHTML = "Search for entities (LOADING...)";

			fetch(request).then(function(response) {
				loading = false;
				buttonSearch.innerHTML = "Search for entities";
				return response.json();
			}).then(function(data) {
				callback(data);
			});
		}
	}

	function retrieveAvailableProperties(callback) {
		var url = CONFIG.DOMAIN + "/RetrieveAvailableProperties";
		var request = new Request(url, {
			method: "GET", 
			mode: "cors", 
			headers: new Headers({
				'Content-Type': 'text/plain'
			})
		});

		log("Retrieving available properties...");
		fetch(request).then(function(response) {
			return response.json();
		}).then(function(data) {
			callback(data);
		});

	}



	var module = {};	
	module.retrieveTriples = retrieveTriples;
	module.retrieveAvailableProperties = retrieveAvailableProperties;

	return module;
})();