define(function() {
  
  var LoadIndicator = function() {
    var element = document.createElement('div');
    element.className = 'load-indicator';
    element.style.visibility = 'hidden';
    
    document.body.appendChild(element);
    
    /** Privileged fields **/
    this.element = element;
    this.timer;
  };
  
  /**
   * Shows the load indicator.
   */
  LoadIndicator.prototype.show = function() {
    var self = this;
    this.timer = setTimeout(function() {
      self.element.style.visibility = 'visible';
      delete self.timer;
    }, 200); 
  }

  /**
   * Hides the load indicator.
   */
  LoadIndicator.prototype.hide = function() {
    if (this.timer)
      clearTimeout(this.timer);
    this.element.style.visibility = 'hidden';
  }
  
  return LoadIndicator;
  
});
