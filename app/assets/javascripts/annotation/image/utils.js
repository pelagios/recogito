define([], function() {
  
  var Utils = function() { }
  
  Utils.prototype.getBounds = function(annotation) {
    var geom = annotation.shapes[0].geometry;
    
    var dx = Math.cos(geom.a) * geom.l;
    var dy = Math.sin(geom.a) * geom.l;
    
    var baseEnd = { x: geom.x + dx, y: geom.y + dy };
    var opposite = {
      x: baseEnd.x - geom.h * Math.sin(geom.a),
      y: baseEnd.y + geom.h * Math.cos(geom.a)
    };
    
    return {
      left: Math.min(Math.min(geom.x, opposite.x), baseEnd.x),
      top: Math.min(Math.min(geom.y, opposite.y), baseEnd.y),
      width: Math.max(dx, Math.abs(opposite.x - geom.x)),
      height: Math.max(dy, Math.abs(opposite.y - geom.y))
    }
  }
  
  return new Utils();
  
});
