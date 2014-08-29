define([], function() {
  
  var Utils = function() { }
  
  Utils.prototype.getBounds = function(annotation) {
    var geom = annotation.shapes[0].geometry;
    
    var a = { x: geom.x, y: geom.y },
        b = {
          x: a.x + Math.cos(geom.a) * geom.l,
          y: a.y + Math.sin(geom.a) * geom.l 
        },
        c = {
          x: b.x - geom.h * Math.sin(geom.a),
          y: b.y + geom.h * Math.cos(geom.a)
        },
        d = {
          x: geom.x - geom.h * Math.sin(geom.a),
          y: geom.y + geom.h * Math.cos(geom.a)      
        };
    
    var top = Math.max(a.y, b.y, c.y, d.y),
        right = Math.max(a.x, b.x, c.x, d.x),
        bottom = Math.min(a.y, b.y, c.y, d.y),
        left = Math.min(a.x, b.x, c.x, d.x);
    
    var bounds = {
      left: left,
      top: top,
      width: right - left,
      height: top - bottom
    };
    
    console.log(bounds);
    return bounds;
  }
  
  return new Utils();
  
});
