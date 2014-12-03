define([], function() {
  
  var EditorAutoSuggest = function(parentEl, textInput) {
    var ENDPOINT_URL = '../../api/search/place?query=',
    
        pendingQuery = false,
    
        getSuggestion = function(chars) {
          if (chars.length > 1) { // Start fetching proposals from 2 chars onwards
            jQuery.getJSON(ENDPOINT_URL + encodeURIComponent(chars + '*'), function(data) {
              console.log(data);
            });
          }
        };
    
    textInput.keyup(function(e) {
      // Introduce a 200ms wait, in order to not overload the wire
      if (pendingQuery) {
        window.clearTimeout(pendingQuery);
        pedingQuery = false;
      }
       
      var chars = textInput.val();
      pendingQuery = window.setTimeout(function() {
        getSuggestion(chars);
      }, 200);
    });    
  };
  
  return EditorAutoSuggest;
  
});
  
