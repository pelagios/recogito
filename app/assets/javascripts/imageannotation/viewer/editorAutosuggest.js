define([], function() {
  
  var EditorAutoSuggest = function(parentEl, textInput, uriInput) {
    var ENDPOINT_URL = '../../api/search?prefix='+ encodeURIComponent('http://www.maphistory.info/') + '&query=',
    
        pendingQuery = false,
        
        /** List element **/
        ul = jQuery('<ul></ul>'),
    
        /** Fetches search results from the server and displays them inside the parentEl **/
        getSuggestion = function(chars) {
          if (chars.length > 1) { // Start fetching proposals from 2 chars onwards
            jQuery.getJSON(ENDPOINT_URL + encodeURIComponent(chars).replace('%20', '+AND+') + '*', function(data) {
              var results = data.results.slice(0, 10);
              
              // Remove old results from list - except selected one (if any)
              ul.find('li').not('.selected').remove();

              // Add results to list
              jQuery.each(results, function(idx, result) {
                var title = result.names.slice(0,5).join(', ');                
                if (result.description)
                  title += ' (' + result.description + ')';
                  
                ul.append('<li title="' + title + '" data-uri="' + result.uri + '">' + result.title + '</li>');
              });              
            });
          }
        },
        
        /** Handler function to select a suggestion from the list **/
        toggleSelect = function(li) {
          var selected = li.hasClass('selected');
          if (selected) {
            // User cleared selection
            li.removeClass('selected');
            uriInput.val();
          } else {
            // User set a new selection
            jQuery.each(li.siblings(), function(idx, li) {
              jQuery(li).removeClass('selected');
            });
            li.addClass('selected');
            uriInput.val(li.data('uri'));
          }
        },
        
        /** Shows the autosuggest widget **/
        show = function() {
          parentEl.show();
        },
        
        /** Hides the auto-suggest widget **/
        hide = function() {
          parentEl.html('');
          parentEl.hide();
        };
        
    parentEl.append(ul);
    
    // Double click selects the place AND transfers the title to the text field
    ul.on('dblclick', 'li', function(e) {
      var t = jQuery(e.target);
      toggleSelect(t);
      textInput.val(t.html());
    });
    
    // Single click only selects the place
    ul.on('click', 'li', function(e) {
      var t = jQuery(e.target);
      toggleSelect(t);
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
    
    /** Privileged methods **/
    this.show = show;
    this.hide = hide;  
  };
  
  return EditorAutoSuggest;
  
});
  
