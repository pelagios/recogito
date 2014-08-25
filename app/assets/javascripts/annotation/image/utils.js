define([], function() {
  
  var Utils = function() { }
  
  Utils.prototype.getBounds = function(annotation) {
    var geom = annotation.shapes[0].geometry;
    
    var dx = Math.cos(geom.a) * geom.l;
    var dy = Math.sin(geom.a) * geom.l;
    
    var opposite = { x: geom.x + dx, y: geom.y + dy };
    
    return {
      left: Math.min(geom.x, opposite.x),
      top: Math.min(geom.y, opposite.y),
      width: dx,
      height: dy
    }
  }
  
  return new Utils();
  
});
