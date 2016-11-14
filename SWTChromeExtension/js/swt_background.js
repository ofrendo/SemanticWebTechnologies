// Wrap jQuery to access $
(function($) {

	$(document).ready(function() {
		log("Loaded extension.");
		init();		
	});

	function init() {
		log("Initializing...");
		addStaticElements();
	}

	function addStaticElements() {
		var divContainer = document.createElement("div");
		divContainer.id = "swtDivSearch";

		var buttonSearch = document.createElement("button");
		buttonSearch.id = "swtButtonSearch";
		buttonSearch.innerHTML = "Search for entities";
		buttonSearch.addEventListener("click", onSearch);

		divContainer.appendChild(buttonSearch);

		document.querySelector("body").appendChild(divContainer);
	}

	function onSearch() {
		var selectedText = getSelectedText();
		if (selectedText.length > 0) {
			log("Retrieving entities...");
			Connector.retrieveTriples(selectedText, function(response) {
				console.log(response);
			});
		}
		
	}

	// http://stackoverflow.com/questions/3545018/selected-text-event-trigger-in-javascript
	function getSelectedText() {
		if (window.getSelection) {
	        return window.getSelection().toString();
	    } else if (document.selection) {
	        return document.selection.createRange().text;
	    }
	    return '';
	}

})(jQuery);










