// Wrap jQuery to access $
(function($) {

	$(document).ready(function() {
		log("Loaded extension.");
		init();		
	});

	var options = {};

	function init() {
		log("Initializing...");
		addFixedElements();
		addPopup();

		chrome.storage.sync.get("options", function(obj) {
			options = obj.options;
		});
	}

	function addFixedElements() {
		var divContainer = document.createElement("div");
		divContainer.id = "swtDivSearch";

		var buttonSearch = document.createElement("button");
		buttonSearch.id = "swtButtonSearch";
		buttonSearch.innerHTML = "Search for entities";
		buttonSearch.addEventListener("click", onSearch);

		divContainer.appendChild(buttonSearch);

		document.querySelector("body").appendChild(divContainer);
	}

	function addPopup() {
		var divOverlay = document.createElement("div");
		divOverlay.id = "swtDivOverlay";
		divOverlay.classList.add("swtHidden");

		var divContainer = document.createElement("div");
		divContainer.id = "swtDivPopup";
		divContainer.classList.add("swtHidden");

		var header = document.createElement("h3");
		header.innerHTML = "SelectedText";
		header.id = "swtDivPopupHeader";
		divContainer.appendChild(header);

		var divEntityContainer = document.createElement("div");
		divEntityContainer.id = "swtDivEntityContainer";
		divContainer.appendChild(divEntityContainer);

		//var svgVisualization = document.createElement("svg");
		var svgVisualization = document.createElementNS("http://www.w3.org/2000/svg", "svg");
		svgVisualization.setAttribute("width", divContainer.offsetWidth-5);
		//svgVisualization.setAttribute("width", 700);
		svgVisualization.setAttribute("height", 560);
		svgVisualization.id = "swtSVGVisualization";
		divContainer.appendChild(svgVisualization);

		var buttonClose = document.createElement("button");
		buttonClose.id = "swtButtonClosePopup";
		buttonClose.innerHTML = "X";
		buttonClose.addEventListener("click", onPopupClose);
		divContainer.appendChild(buttonClose);

		document.querySelector("body").appendChild(divOverlay);
		document.querySelector("body").appendChild(divContainer);
	}
	function onPopupShow() {
		var divContainer = document.getElementById("swtDivPopup");
		divContainer.classList.remove("swtHidden");
		var divOverlay = document.getElementById("swtDivOverlay");
		//divOverlay.classList.remove("swtHidden");
	}
	function onPopupClose() {
		var divContainer = document.getElementById("swtDivPopup");
		divContainer.classList.add("swtHidden");
		var divOverlay = document.getElementById("swtDivOverlay");
		//divOverlay.classList.add("swtHidden");
	}

	function setPopupContents(selectedText, data) {
		var divEntityContainer = document.getElementById("swtDivEntityContainer");
		divEntityContainer.innerHTML = "";
		var svg = document.getElementById("swtSVGVisualization");
		svg.innerHTML = "";

		document.getElementById("swtDivPopupHeader").innerHTML = "Entity search: " + selectedText;
		for (var i=0;i<data.entities.length;i++) {
			divEntityContainer.appendChild(createEntity(data.entities[i]));
		}
		createVisualization(data.contextTriples);
	}

	function createEntity(entity) {
		var divContainer = document.createElement("div");
		var a = document.createElement("a");
		a.href = entity.URI;
		a.innerHTML = entity.entityName + ": " + entity.entityType;
		divContainer.appendChild(a);

		var divContent = document.createElement("div");
		divContent.classList.add("swtDivEntityContent");
		var depictionSrc;
		var ul = document.createElement("ul");
		for (var i=0; i<entity.properties.length;i++) {

			var propertyName = entity.properties[i].name; 
			if (CONFIG.PROPERTY_LABEL_MAPPING[propertyName]) {
				propertyName = CONFIG.PROPERTY_LABEL_MAPPING[propertyName];
			}

			for (var j=0;j<entity.properties[i].value.length;j++) {

				var val = entity.properties[i].value[j];

				// "depiction" is special case
				if (propertyName === "depiction" || propertyName === "http://webprotege.stanford.edu/depiction") {
					depictionSrc = val;
				}
				else {

					// Compare to previous strings already added
					var highestSim = -1;
					for (var k=0;k<j;k++) {
						var alreadyAddedVal = entity.properties[i].value[k];
						var sim = CONFIG.SIM_FUNCTION(alreadyAddedVal, val);
						if (highestSim < sim)
							highestSim = sim;
					}


					var li = (highestSim < CONFIG.MIN_SIM) ? 
								buildOriginalAttributeLi(propertyName, val) : 
								buildSimilarAttributeLi(propertyName, val, highestSim);
					li.classList.add("swtLi");
					ul.appendChild(li);
				}
			}
		}
		if (entity.properties.length === 0) {
			var li = document.createElement("li");
			li.innerHTML = "No results found.";
			ul.appendChild(li);
		}

		divContent.appendChild(ul);

		if (depictionSrc) {
			//divContainer.appendChild(document.createElement("br"));

			var img = document.createElement("img");
			img.classList.add("swtDepiction");
			img.src = depictionSrc;
			divContent.appendChild(img);
		}

		divContainer.appendChild(divContent);

		return divContainer;
 	}

 	function buildOriginalAttributeLi(propertyName, val) {
 		var li = document.createElement("li");
		li.innerHTML = buildLiHTML(propertyName, val);
		return li;
 	}
 	function buildSimilarAttributeLi(propertyName, val, highestSim) {
 		var li = document.createElement("li");
 		li.innerHTML = propertyName + ": Similar value found (sim=" + formatDouble(highestSim) + "). Click to show";

 		li.classList.add("liSimilarValue");
 		li.dataset.show_value = "hide";
 		var showing = false;

 		li.addEventListener("click", function(e) {
 			li = e.target;
 			if (showing === true) {
 				showing = false;
 				li.dataset.show_value = "hide";
 				li.innerHTML = propertyName + ": Similar value found (sim=" + formatDouble(highestSim) + "). Click to show";
 			}
 			else {
 				showing = true;
 				li.dataset.show_value = "show";
 				li.innerHTML = buildLiHTML(propertyName, val);
 			}
 		});
 		return li;
 	}
 	function buildLiHTML(propertyName, val) {
 		if (isURL(val)) {
			return propertyName + ": <a href='" + val + "'>" + val + "</a>";
		}
		else {
			return propertyName + ": " + val;
		}
 	}

 	function createVisualization(contextTriples) {
 		SWT_Visualizer.createVisualization(contextTriples);
 	}

	function onSearch() {
		var selectedText = (CONFIG.USE_SELECTED_TEXT === null) ?
							getSelectedText() :
							CONFIG.USE_SELECTED_TEXT;
		selectedText = sanitizeInput(selectedText);

		if (selectedText.length > 0) {
			Connector.retrieveTriples(options, selectedText, function(data) {
				console.log(data);
				setPopupContents(selectedText, data);
				onPopupShow();
			});
		}
		
	}

	function sanitizeInput(inputText) {
		inputText = inputText.replace("’", "'");

		return inputText;
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










