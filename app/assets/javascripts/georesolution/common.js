define(function() {

  /**
   * A simple base class that takes care of event subcription.
   * @contructor
   */
  var HasEvents = function() { 
    this.handlers = {}
  }

  /**
   * Adds an event handler to this component. Refer to the docs of the components
   * for information about supported events.
   * @param {String} event the event name
   * @param {Function} handler the handler function
   */
  HasEvents.prototype.on = function(event, handler) {  
    this.handlers[event] = handler; 
  }

  /**
   * Fires an event.
   * @param {String} event the event name
   * @param {Object} e the event object
   * @param {Object} args the event arguments
   */
  HasEvents.prototype.fireEvent = function(event, e, args) {
    if (this.handlers[event])
      this.handlers[event](e, args);     
  }

  /**
   * A tag editor component to be used in the Details & Batch popups.
   * @param {Element} parent the DOM element to attach the editor to 
   * @param {Number} offsetTop the top offset for placing the editor
   * @param {Number} offsetLeft the left offset for placing the editor
   * @param {Function} onEnter a callback for catching the 'ENTER' keypress
   * @param {Function} onEscape a callback for catching the 'Escape' keypress
   */
  var TagEditor = function(parent, offsetTop, offsetLeft, onEnter, onEscape) {
    var self = this;
  
    this.element = $('<div class="popup-tag-editor"><input type="text" placeholder="Tags, separated by comma..."></input></div>');
    this.element.css('top', (offsetTop +  3) + 'px');
    this.element.css('left', offsetLeft + 'px');
    this.element.appendTo(parent); 
    this.element.click(function(e) { e.stopPropagation(); });
  
    var textField = this.element.find('input')[0];    
    textField.focus()
    this.element.keydown(function(e) {
      if (e.keyCode == 13) {
        // Enter
        if (onEnter)
          onEnter(textField.value.split(","));
        
        self.destroy();
      } else if (e.keyCode == 27) {
        // Escape
        if (onEscape)
          onEscape(self);
        
        self.destroy();
      }    
    }); 
  }

  TagEditor.prototype.destroy = function() {
    $(this.element).remove(); 
  }

  /**
   * Helpers and utility methods.
   */
  var Utils = {
  
    /** Normalizes Pleiades URIs by stripping the trailing '#this' (if any) **/
    normalizePleiadesURI: function(uri) {
      if (uri.indexOf('#this') < 0) {
        return uri;
      } else {
        return uri.substring(0, uri.indexOf('#this'));
      }
    },
  
    formatCategory: function(category, opt_template) {
      if (!category)
        return '';
      
      var screenName;
    
      if (category == 'SETTLEMENT')
        screenName = 'Settlement';
      else if (category == 'REGION')
        screenName = 'Region';
      else if (category == 'NATURAL_FEATURE')
        screenName = 'Natural Feature'
      else if (category == 'ETHNOS')
        screenName = 'Ethnos';
      else if (category == 'MAN_MADE_STRUCTURE')
        screenName = 'Built Structure';
      else
        screenName = category;
      
      if (opt_template)
        return opt_template.replace('{{category}}', screenName);
      else
        return screenName
    }
  
  }
  
  return {
    
    HasEvents: HasEvents,
    
    TagEditor: TagEditor,
    
    Utils: Utils
    
  };

});
