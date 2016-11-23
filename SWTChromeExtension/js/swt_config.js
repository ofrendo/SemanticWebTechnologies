var CONFIG = {
	DOMAIN: "https://localhost:8443",
	PROPERTY_LABEL_MAPPING: {
		"http://webprotege.stanford.edu/foundedBy": "Founded by",
		"http://webprotege.stanford.edu/homepage": "Homepage",
		"http://webprotege.stanford.edu/isPrimaryTopicOf": "Link",
		"http://webprotege.stanford.edu/abstract": "Abstract",
		"http://webprotege.stanford.edu/geoPoint": "Coordinates",
		"http://webprotege.stanford.edu/birthYear": "Birth year",
		"http://webprotege.stanford.edu/networth": "Net worth",
    "http://webprotege.stanford.edu/population": "Population"
	},
	USE_SELECTED_TEXT: null //"Michael Gove, Iain Duncan Smith and Theresa Villier are among her backers."
};




function log(input) {
	console.log("[SWT] " + input);
}

function isURL(str) {
  var pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
  '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.?)+[a-z]{2,}|'+ // domain name
  '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
  '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
  '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
  '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
  return pattern.test(str);
}

function encodeHTML(str){
 var aStr = str.split(''),
     i = aStr.length,
     aRet = [];

   while (--i) {
    var iC = aStr[i].charCodeAt();
    if (iC < 65 || iC > 127 || (iC>90 && iC<97)) {
      aRet.push('&#'+iC+';');
    } else {
      aRet.push(aStr[i]);
    }
  }
 return aRet.reverse().join('');
}