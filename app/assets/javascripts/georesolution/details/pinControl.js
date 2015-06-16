define([], function() {
  
  var PinControl = function(map, args) {
    
    var control = L.control(args),
    
        element = jQuery('<div class="icon pin-control">&#xf08d;</div>'),
    
        /** Enabled flag (faster than DOM lookups) **/
        enabled = false;
        
        onAdd = function(map) {          
          return element[0];
        },
        
        onClick = function() {
          if (element.hasClass('enabled')) {
            enabled = false;
            element.removeClass('enabled');
          } else {
            enabled = true;
            element.addClass('enabled');
          }
        },
        
        isEnabled = function() {
          return enabled;
        };
        
    element.click(onClick);
    control.onAdd = onAdd;
    control.addTo(map);
    
    this.isEnabled = isEnabled;
  };
  
  return PinControl;
  
});
