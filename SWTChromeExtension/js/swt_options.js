
var Options = (function($) {

	function init() {
		loadProperties();
		var buttonSave = document.getElementById("buttonSaveAvailableProperties");
		buttonSave.addEventListener("click", saveProperties);

		var buttonReset = document.getElementById("buttonResetAvailableProperties");
		buttonReset.addEventListener("click", resetProperties);
	}

	function loadProperties() {
		Connector.retrieveAvailableProperties(function(properties) {
			console.log("Retrieved available properties from server");
			console.log(properties);
			showProperties(properties);

			// Modify to fit those already saved
			modifyToSavedProperties();
		});
	}

	function showProperties(queryProperties) {
		var container = document.getElementById("divAvailablePropertiesContainer");
		for (var i=0;i<queryProperties.length;i++) {
			var qp = queryProperties[i];

			var qpContainer = document.createElement("div");
			var html = "<span><b>" + qp.entityType + "</b></span><br>";
			for (var j=0;j<qp.properties.length;j++) {
				var q = qp.properties[j];
				html += "<input type='checkbox' value='" + qp.entityType + "_" + q.id + "'>" + q.label + " (" + q.uri + ")" + "<br>"
			}
			qpContainer.innerHTML = html;

			container.appendChild(qpContainer);
		}
	}

	function modifyToSavedProperties() {
		chrome.storage.sync.get("options", function(obj) {
			var options = obj.options;
			log("Previously saved options: ");
			console.log(options);
			if (!options) return;

			var checkboxes = document.querySelectorAll("input[type=checkbox]");
			for (var i=0;i<checkboxes.length;i++) {
				var c = checkboxes[i];
				var val = c.value.split("_");
				var entityType = val[0];
				var id = val[1];

				if (options[entityType].indexOf(id) !== -1) {
					c.checked = true;
				}
			}
		});
	}

	function resetProperties() {
		log("Resetting available properties...");
		chrome.storage.sync.set({
			"options": {}
		}, function() {
			var checkboxes = document.querySelectorAll("input[type=checkbox]");
			for (var i=0;i<checkboxes.length;i++) {
				checkboxes[i].checked = false;
			}
			log("Reset available properties.");
		});
	}

	// Save the IDs of those properties that should be saved
	function saveProperties() {
		var checkboxes = document.querySelectorAll("input[type=checkbox]");
		var options = {};
		for (var i=0;i<checkboxes.length;i++) {
			var c = checkboxes[i];
			var val = c.value.split("_");

			var entityType = val[0];
			if (!options[entityType]) {
				options[entityType] = [];
			}
			var id = val[1];	
			if (c.checked === true) {
				options[entityType].push(id);
			}	
		}
		log("Saving available properties...");
		console.log(options);
		chrome.storage.sync.set({
			"options": options
		}, function() {
			log("Saved available properties.");
		});
	}

	var module = {};
	module.init = init;
	return module;

})(jQuery);

Options.init();



