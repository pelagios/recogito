define([], function() {
  
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
    var geom = annotation.shapes[0].geometry;

    var viewportAnchorCoords = map.getPixelFromCoordinate([geom.x, - geom.y]);    
    var viewportBaselineLength = geom.l / map.getView().getResolution();
    var dx = Math.cos(geom.a) * viewportBaselineLength;
    var dy = Math.sin(geom.a) * viewportBaselineLength;
  
    // TODO make this work for all quadrants
    var x = viewportAnchorCoords[0] - 25;
    var y = viewportAnchorCoords[1] - dy - 25;
    var width = dx + 50;
    var height = dy + 50;
    
    top.css('height', y + 'px');
    middle.css('height', height + 'px');
    left.css('width', x + 'px');
    right.css('left', (x + width) + 'px');
    window.css('width', width + 'px');
    bottom.css('top', (y + height) + 'px');
    
    /*
    window.css({
      width: dx + 30,
      height: dy + 30
    });
    
    editor.css({
      left: viewportAnchorCoords[0] - 15,
      top: viewportAnchorCoords[1] - dy - 15
    });
    * 
    * 
    *         top.height = y + 'px';
        middle.height = height + 'px';
        left.width = x + 'px';
        right.left = (x +  width) + 'px';
        region.width = width + 'px';        
        bottom.top = (y + height) + 'px';
    */

    element.show();   
  }
  
  Editor.prototype.hide = function() {
    element.hide();
  }
  
  return Editor;
  
});
