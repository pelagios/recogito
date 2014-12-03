define([], function() {
  
  var HelpWindow = function() {
    var element = 
          $('<div class="help-popup">' +
            '  <div class="header">HELP' +
            '    <span class="exit icon" title="Close">&#xf00d;</span>' + 
            '  </div>' +
            '  <div class="body">' +
            '    <h2>Keyboard Shortcuts</h2>' +
            '    <table>' +
            '      <tr><td class="keys">SPACE</td><td>toggle between Navigation and Annotation mode</td></tr>' +
            '      <tr><td class="keys">SHIFT+Alt</td><td>rotate the map (hold keys and drag mouse)</td></tr>' +
            '      <tr><td class="keys">ESCAPE</td><td>cancel while drawing an annotation</td></tr>' +          
            '    </table>' +
            '  </div>' +
            '</div>'),
      
        show = function() {
          element.find('.exit').click(hide);   
          element.draggable({ handle: element.find('.header') });
          jQuery(document.body).append(element);
        },
      
        hide = function() {
          element.remove();
        },
      
        isVisible = function() {
          return element.closest('html').length > 0;
        };
      
    /** Privileged methods **/
    this.show = show;
    this.hide = hide;
    this.isVisible = isVisible;
  }
    
  return HelpWindow;
    
});
  
  
