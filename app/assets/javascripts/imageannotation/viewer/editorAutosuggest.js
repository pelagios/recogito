define([], function() {
  
  var EditorAutoSuggest = function(parentEl, textInput, uriInput) {
    var ENDPOINT_URL = '../../api/search?query=',
    
        pendingQuery = false,
    
        getSuggestion = function(chars) {
          if (chars.length > 1) { // Start fetching proposals from 2 chars onwards
            jQuery.getJSON(ENDPOINT_URL + encodeURIComponent(chars).replace('%20', '+AND+') + '*', function(data) {
              var results = data.results.slice(0,10),
                  html = '<ul>';
              
              jQuery.each(results, function(idx, result) {
                var title = result.names.slice(0,5).join(', ');
                
                if (result.description)
                  title += ' (' + result.description + ')';
                  
                html += '<li title="' + title + '" data-uri="' + result.uri + '">' + result.title + '</li>';
              });
              
              html += '</ul>';
              parentEl.html(html);
            });
          }
        },
        
        show = function() {
          parentEl.show();
        },
        
        hide = function() {
          parentEl.html('');
          parentEl.hide();
        };
    
    parentEl.on('click', 'li', function(el) {
      var t = jQuery(el.target),
          name = t.html(),
          uri = t.data('uri');
          
      textInput.val(name);
      uriInput.val(uri);
    });
    
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
    
    this.show = show;
    this.hide = hide;  
  };
  
  return EditorAutoSuggest;
  
});
  
