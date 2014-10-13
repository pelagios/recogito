define([], function() {
  
  var _annotations = [];
  
  /**
   * A container object that manages (surprise) annotations,
   * and provides a number of helper functions related to 
   * displaying them graphically on the screen.
   * 
   * TODO store annotations in a quadtree for improved collision detection
   */  
  var Annotations = function() { };
  
  /** Helper to compute the rectangle from an annotation geometry **/
  Annotations.getRect = function(annotation) {     
    var geom = annotation.shapes[0].geometry,
        a = { x: geom.x, 
              y: geom.y },
        b = { x: a.x + Math.cos(geom.a) * geom.l,
              y: a.y - Math.sin(geom.a) * geom.l },
        c = { x: b.x - geom.h  * Math.sin(geom.a),
              y: b.y - geom.h * Math.cos(geom.a) },
        d = { x: a.x - geom.h * Math.sin(geom.a),
              y: a.y - geom.h * Math.cos(geom.a) };
        
    return [ a, b, c, d ];
  };   
  
  Annotations.getTranscription = function(annotation) {
    return (annotation.corrected_toponym) ? annotation.corrected_toponym : annotation.toponym;
  }; 
  
  /** Helper function to compute the bounding box for a rectangle **/
  var _getBBox = function(rect) {
    return {
      top: Math.min(rect[0].y, rect[1].y, rect[2].y, rect[3].y),
      right: Math.max(rect[0].x, rect[1].x, rect[2].x, rect[3].x),
      bottom: Math.max(rect[0].y, rect[1].y, rect[2].y, rect[3].y),
      left: Math.min(rect[0].x, rect[1].x, rect[2].x, rect[3].x)
    }
  };
  
  /** Tests if the given coordinate intersects the rectangle **/
  var _intersects = function(x, y, rect) {
    var inside = false;
    var j = 3; // rect.length - 1 (but we know rect.length is always 4)
    for (var i=0; i<4; i++) {
      if ((rect[i].y > y) != (rect[j].y > y) && 
          (x < (rect[j].x - rect[i].x) * (y - rect[i].y) / (rect[j].y-rect[i].y) + rect[i].x)) {
        inside = !inside;
      }
      j = i;
    }
    return inside;
  };
  
  /** Returns all annotations **/
  Annotations.prototype.getAll = function() {
    return _annotations;
  };
  
  /** Returns the annotations at a specifix X/Y coordinate **/
  Annotations.prototype.getAnnotationsAt = function(x, y) {
    // TODO optimize with a quadtree
    var hovered = [];
    jQuery.each(_annotations, function(idx, annotation) {
      var rect = Annotations.getRect(annotation);
      if(_intersects(x, y, rect))
        hovered.push(annotation);
    });
    return hovered;
  }
  
  /** Adds a single annotation, or an array of annotations **/
  Annotations.prototype.add = function(a) {
    if (jQuery.isArray(a))
      _annotations = jQuery.merge(_annotations, a);
    else
      _annotations.push(a);    
  };
  
  /** Removes the annotation with the specified ID **/
  Annotations.prototype.remove = function(id) {
    _annotations = jQuery.grep(_annotations, function(a) {
      return a.id != id;
    });    
  };
    
  return Annotations;
  
});
