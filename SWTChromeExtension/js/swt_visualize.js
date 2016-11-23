var SWT_Visualizer = (function() {

	function transformContextTriples(contextTriples) {
		log("transformContextTriples input/output:");
		console.log(contextTriples);
		var result = {
			nodes: [],
			links: []
		};

		var pushedUniques = {};
		for (var i=0;i<contextTriples.length;i++) {
			var triple = contextTriples[i];
			console.log("triple= " + JSON.stringify(triple));
			// Only push subject / object if it hasn't been pushed yet
			if (pushedUniques[triple.subject] === undefined) {
				console.log("Pushing '" + triple.subject + "' with grp=1");
				pushedUniques[triple.subject] = 1;
				result.nodes.push({id: triple.subject, group: 1});
			}
			// Change group of node if it's a subject and has been pushed as object
			else if (pushedUniques[triple.subject] !== 1) {
				for (var j=0;j<result.nodes.length;j++) {
					if (result.nodes[j].id === triple.subject) {
						console.log("Changing '" + triple.subject + "' to grp=1 for triple=" + JSON.stringify(triple));
						result.nodes[j].group = 1;
						pushedUniques[triple.subject] = 1;
					}
				}
			}
			// Only push an object if it has not been pushed yet
			if (pushedUniques[triple.object] === undefined) {
				console.log("Pushing '" + triple.object + "' with grp=3 for triple=" + JSON.stringify(triple));
				pushedUniques[triple.object] = 3;
				result.nodes.push({id: triple.object, group: 3});
			}

			// Always pushed predicate (since it's not unique)	
			result.nodes.push({id: triple.predicate + "__" + i, group: 2});
			
			result.links.push({source: triple.subject, target: triple.predicate + "__" + i});
			result.links.push({source: triple.predicate + "__" + i, target: triple.object});
		}
		console.log(result);
		return result;
	}

	function createVisualization(contextTriples) {
		// see http://bl.ocks.org/mbostock/4062045
		var svg = d3.select("#swtSVGVisualization"),
		    width = +svg.attr("width"),
		    height = +svg.attr("height");
	    var container = svg.append("g");

		var color = d3.scaleOrdinal(d3.schemeCategory20);

		var simulation = d3.forceSimulation()
		    .force("link", d3.forceLink().id(function(d) { return d.id; })) //.distance(150)
		    .force("charge", d3.forceManyBody().strength([-150]).distanceMin([1000]) )  //
		    .force("center", d3.forceCenter(width / 2, height / 2))
		    .force("collide", d3.forceCollide().radius(function(d) { 
		    	//console.log(d.r); return d.r + 0.5; 
		    	return 30;
		    }).iterations(2));

		//d3.json(, function(error, graph) {
		//  if (error) throw error;
		var graph = transformContextTriples(contextTriples);

		var link = container.append("g")
		    .attr("class", "links")
		    .selectAll("line")
		    .data(graph.links)
		    .enter().append("line")
		    //.attr("stroke-width", function(d) { return Math.sqrt(d.value); });
		    .attr("stroke-width", function(d) { return 3 });

		var node = container.append("g")
		    .attr("class", "nodes")
		    .selectAll("circle")
		    .data(graph.nodes)
		    .enter().append("circle")
		      .attr("r", function(d) { 
		      	// predicates should have smaller circles
		      	return (d.id.indexOf("__") >= 0) ?
		      			20 :
		      			27;
		      })
		      .attr("fill", function(d) { return color(d.group); })
		      /*.call(d3.drag()
		          .on("start", dragstarted)
		          .on("drag", dragged)
		          .on("end", dragended))*/;

		node.append("title")
		    .text(function(d) { 
		    	return (d.id.indexOf("__") >= 0) ? 
		      			d.id.substring(0, d.id.indexOf("__")) :
		      			d.id; 
		    });

		var nodeText = container.append("g")
		    .attr("class", "texts")
		    .selectAll("text")
		    .data(graph.nodes)
		    .enter().append("text")
		      .text(function(d) { 
		      	// For predicates remove "__1"

		      	//return "test\nbreak very long text with lots of breaks";
		      	return (d.id.indexOf("__") >= 0) ? 
		      			d.id.substring(0, d.id.indexOf("__")) :
		      			d.id; 
		      })
		      .attr("text-anchor", "middle")
		      .call(wrap, 10);


		simulation
		    .nodes(graph.nodes)
		    .on("tick", ticked);

		simulation.force("link")
		    .links(graph.links);
	
		function ticked() {
		    link
		        .attr("x1", function(d) { return d.source.x; })
		        .attr("y1", function(d) { return d.source.y; })
		        .attr("x2", function(d) { return d.target.x; })
		        .attr("y2", function(d) { return d.target.y; });

		    node
		        .attr("cx", function(d) { return d.x; })
		        .attr("cy", function(d) { return d.y; });

		    nodeText
		    	.attr("x", function(d) { return d.x })
		    	.attr("y", function(d) { return d.y+3 })
		    	.selectAll(function(d) { return this.childNodes; })
		    		.attr("x", function(d) { return d.x });
		}

		// https://bl.ocks.org/mbostock/7555321
		function wrap(text, width) {
			setTimeout(function() {
				text.each(function() {
				    var text = d3.select(this);
				    var words = text.text().split(/\s+/).reverse();
				    var word;
				    var wordNumber = words.length;
				    var line = [];
				    var lineNumber = 0;
				    var lineHeight = 12; //1.1; // ems
				    //var x = text.attr("x");
				    var y = text.attr("y");
				        //dy = parseFloat(text.attr("dy") || 0),
				    var tspan = text.text(null).append("tspan");//.attr("x", x); //.attr("dy", dy + "em");
				    var inFirstLine = true;

				    while (words.length > 0) {
				    	word = words.pop();
				    	line.push(word);
				    	tspan.text(line.join(" "));
				      	
				      	//console.log(tspan.node().getComputedTextLength());
				      	if (tspan.node().getComputedTextLength() > width && wordNumber > 1) {
				      		if (inFirstLine === true) {
				      			inFirstLine = false;
				      			continue;
				      		}
				      		// if over width remove the last word
				        	line.pop();
				        	tspan.text(line.join(" ")); 
				        	line = [word];
				        	lineNumber++;
				        	tspan = text.append("tspan").attr("dy", lineHeight).text(word);  // ++lineNumber *  //.attr("x", x)
				      	}
				    }
				    // When done adding tspans, recenter text by changing dy
				   	// 1 line --> 0, 2 lines --> 0.5*lineHeight, 3 lines --> 1*lineHeight
				    text.select(function(){return this.childNodes;})
				    	.each(function(d, i) {
				    		var currentDy = d3.select(this[0]).attr("dy");
				    		d3.select(this[0]).attr("dy", currentDy - ((lineNumber)/2)*lineHeight);
				    		//console.log(this);
				    		//console.log(d);
				    	});
				   	
			  	});
			}, 100);
		}

		// https://bl.ocks.org/mbostock/db6b4335bf1662b413e7968910104f0f
		// Enable zoom and pan
		var zoom = d3.zoom()
		    .scaleExtent([0.5, 10])
		    .on("zoom", zoomed);
		svg.call(zoom);

		function zoomed() {
			//console.log("Zoomed");
			d3.event.sourceEvent.preventDefault();
			d3.event.sourceEvent.stopPropagation();
			container.attr("transform", d3.event.transform);
		}

	}

	var module = {};
	module.createVisualization = createVisualization;
	return module;
})();

