define(['common/hasEvents'], function(HasEvents) {
          
  var element;
  
  /** An overlay component for the details map to filter search results **/
  var SearchresultsFilter = function(el) {
    var self = this;
    
    HasEvents.apply(this, arguments);   
    
    el.on('click', ':checkbox', function(e) {
      var checkbox = $(this);
      if (checkbox.is(':checked'))
        self.fireEvent('showGazetteer', checkbox.val());
      else
        self.fireEvent('hideGazetteer', checkbox.val());
    });
    
    el.dblclick(function(e) { e.stopPropagation(); })
    el.hide();
    element = el;
  };
  SearchresultsFilter.prototype = Object.create(HasEvents.prototype);
  
  SearchresultsFilter.prototype.show = function(total, query, resultsGrouped) {
    var html = total + ' Results';
    
    html += '<ul>';
    for (gazetteer in resultsGrouped) {
      html += '<li><input type="checkbox" value="' + gazetteer + '" checked="true">' + resultsGrouped[gazetteer].length + ' ' + gazetteer + '</li>';
    }
    html += '</ul>';
    
    element.html(html);
    element.show();
  };
  
  SearchresultsFilter.prototype.clear = function() {
    element.html('');
    element.hide();
  };
  
  return SearchresultsFilter;
  
});
