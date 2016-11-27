/*Copyright (c) 2011 Cam Pedersen <cam@campedersen.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the 'Software'), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.*/



var jaccard = (function() {
  /*
   * Return mutual elements in the input sets
   */
  var intersection = function (a, b) {
    var aCopy = a.slice(0);
    var shared = [];
    for (var i=0;i<b.length;i++) {
      var element = b[i];
      var index = aCopy.indexOf(element);
      // Check whether element exists in a: if it does, add it to shared and remove from a so it does not get reused
      if (index !== -1) {
        shared.push(aCopy[index]);
        aCopy.splice(index, 1);
      }
    }
    return shared;
  }

  /*
   * Return distinct elements from both input sets
   */
  var union = function (a, b) {
    var aCopy = a.slice(0);
    var shared = aCopy.slice(0);;
    for (var i=0;i<b.length;i++) {
      var element = b[i];
      var index = aCopy.indexOf(element);
      // Check whether element exists in a: if it does, remove it from a so it does not get reused. if it does not, add to shared
      if (index !== -1) {
        aCopy.splice(index, 1);
      }
      else {
        shared.push(element);
      }
    }
    return shared;
  }

  /*
   * Similarity
   */
  var index = function (a, b) {
    return intersection(a, b).length / union(a, b).length;
  }

  /*
   * Dissimilarity
   */
  var distance = function (a, b) {
    return 1 - index(a, b);
  }


  function sim(s1, s2) {
    var s1p = s1.split(" ");
    var s2p = s2.split(" ");
    return index(s1p, s2p);
  }

  var module = {};
  module.intersection = intersection;
  module.union = union;
  module.index = index;
  module.sim = sim;
  return module;

})();
