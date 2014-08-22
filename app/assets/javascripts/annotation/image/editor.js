define(['annotation/image/utils'], function(Utils) {
  
  var map, element,
      top, middle, left, right, bottom,
      window;
  
  var Editor = function(olMap) {    
    map = olMap;
    
    var template = 
      '<div class="editor-mask">' +
      '  <div class="mask top"></div>' +
      '  <div class="middle">' +
      '    <div class="mask left"></div><div class="window"></div><div class="mask right"></div>' +
      '  </div>' +
      '  <div class="mask bottom"></div>' +
      '</div>';
      
    element = $(template);
    element.hide();
    
    top = element.find('.top');
    middle = element.find('.middle');
    left = element.find('.left');
    right = element.find('.right');
    bottom = element.find('.bottom');
        
    window = element.find('.window');
        
    $('#annotation-area').append(element);
  };
  
  Editor.prototype.show = function(annotation) {
    var bounds = map.toViewportCoordinates(Utils.getBounds(annotation), 50);   
  
    top.height(bounds.top);
    middle.height(bounds.height);
    left.width(bounds.left);
    
    window.width(bounds.width);
    
    right.css('left', (bounds.left + bounds.width) + 'px');
    bottom.css('top', (bounds.top + bounds.height) + 'px');

    element.show();   
  }
  
  Editor.prototype.hide = function() {
    element.hide();
  }
  
  return Editor;
  
});
