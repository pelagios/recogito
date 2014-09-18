define([], function() {
  
  var _handlers = [];
  
  var EventBroker = function() {};
  
  EventBroker.prototype.addHandler = function(type, handler) {
    if (!_handlers[type])
      _handlers[type] = [];
      
    _handlers[type].push(handler);
  };
  
  EventBroker.prototype.removeHandler = function(type, handler) {
    var handlers = _handlers[type];
    if (handlers) {
      var idx = handlers.indexOf(handler);
      handlers.splice(idx, 1);  
    }
  };   
  
  EventBroker.prototype.fireEvent = function(type, opt_event) {
    var handlers = _handlers[type];
    if (handlers) {
      jQuery.each(handlers, function(idx, handler) {
        handler(opt_event);
      });
    }    
  }

  return new EventBroker();
  
});
