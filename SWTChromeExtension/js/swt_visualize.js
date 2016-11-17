var SWT_Visualizer = (function() {

	function transformContextTriples(contextTriples) {
		log("transformContextTriples input/output:");
		console.log(contextTriples);
		var result = {
			nodes: [],
			links: []
		};

		var pushedUniques = [];
		for (var i=0;i<contextTriples.length;i++) {
			var triple = contextTriples[i];
			
			// Only push subject / object if it hasn't been pushed yet
			if (pushedUniques[triple.subject] === undefined) {
				pushedUniques[triple.subject] = triple.subject;
				result.nodes.push({id: triple.subject, group: 1});
			}
			if (pushedUniques[triple.object] === undefined) {
				pushedUniques[triple.object] = triple.object;
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
		var svg = d3.select("#swtSVGVisualization"),
		    width = +svg.attr("width"),
		    height = +svg.attr("height");

		var color = d3.scaleOrdinal(d3.schemeCategory20);

		var simulation = d3.forceSimulation()
		    .force("link", d3.forceLink().id(function(d) { return d.id; }).distance(150))
		    .force("charge", d3.forceManyBody().strength([-150]) )
		    .force("center", d3.forceCenter(width / 2, height / 2))
		    .force("collide", d3.forceCollide().radius(function(d) { return d.r + 0.5; }).iterations(2));

		//d3.json(, function(error, graph) {
		//  if (error) throw error;
		var graph = transformContextTriples(contextTriples);

		var link = svg.append("g")
		    .attr("class", "links")
		    .selectAll("line")
		    .data(graph.links)
		    .enter().append("line")
		    //.attr("stroke-width", function(d) { return Math.sqrt(d.value); });
		    .attr("stroke-width", function(d) { return 3 });

		var node = svg.append("g")
		    .attr("class", "nodes")
		    .selectAll("circle")
		    .data(graph.nodes)
		    .enter().append("circle")
		      .attr("r", function(d) { 
		      	// predicates should have smaller circles
		      	return (d.id.indexOf("__") >= 0) ?
		      			15 :
		      			20;
		      })
		      .attr("fill", function(d) { return color(d.group); })
		      /*.call(d3.drag()
		          .on("start", dragstarted)
		          .on("drag", dragged)
		          .on("end", dragended))*/;

		node.append("title")
		    .text(function(d) { return d.id; });

		var nodeText = svg.append("g")
		    .attr("class", "texts")
		    .selectAll("text")
		    .data(graph.nodes)
		    .enter().append("text")
		      .text(function(d) { 
		      	// For predicates remove "__1"
		      	return (d.id.indexOf("__") >= 0) ? 
		      			d.id.substring(0, d.id.indexOf("__")) :
		      			d.id; 
		      })
		      .attr("text-anchor", "middle");


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
		    	.attr("y", function(d) { return d.y+3 });
		}

		function dragstarted(d) {
		  if (!d3.event.active) simulation.alphaTarget(0.3).restart();
		  d.fx = d.x;
		  d.fy = d.y;
		}

		function dragged(d) {
		  d.fx = d3.event.x;
		  d.fy = d3.event.y;
		}

		function dragended(d) {
		  if (!d3.event.active) simulation.alphaTarget(0);
		  d.fx = null;
		  d.fy = null;
		}
	}

	var module = {};
	module.createVisualization = createVisualization;
	return module;
})();

