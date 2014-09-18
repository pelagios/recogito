define(['config', 'imageannotation/events', 'imageannotation/utils'], function(Config, EventBroker, Utils) {
  
  var _map, _mapLayer, _annotations, _currentHighlight; // Private fields
     
  var TWO_PI = 2 * Math.PI; // Shortcut
  
  var AnnotationLayer = function(map) {
    _map = map;
    _annotations = [];
    _currentHighlight = false;
    
    // Set up the map layer that holds the annotations
    _mapLayer = new ol.layer.Image({
      source: new ol.source.ImageCanvas({
        canvasFunction: _redrawAll,
        projection: 'ZOOMIFY'
      })
    });
    map.addLayer(_mapLayer);
        
    map.on('pointermove', _onMouseMove);
    map.on('singleclick', _onClick);
  };
  
  /** Draws a single annotation onto the canvas **/
  var _drawOne = function(annotation, extent, scale, ctx) {
    var geometry = annotation.shapes[0].geometry;
    var viewportX = scale * (geometry.x - extent[0]);
    var viewportY = scale * (geometry.y + extent[3]);
    var viewportLength = scale * geometry.l;
      
    var dx = Math.cos(geometry.a) * viewportLength;
    var dy = Math.sin(geometry.a) * viewportLength;
      
    ctx.beginPath();
    ctx.arc(viewportX, viewportY, Config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
    ctx.fill();
    ctx.closePath();
        
    ctx.beginPath();
    ctx.moveTo(viewportX, viewportY);
    ctx.lineTo(viewportX + dx, viewportY - dy);
    ctx.stroke();
    ctx.closePath();    
  };
  
  /** The rendering loop that draws the annotations onto the map layer **/
  var _redrawAll = function(extent, resolution, pixelRatio, size, projection) {                
    var canvas = document.createElement('canvas');
    canvas.width = size[0];
    canvas.height = size[1];

    var ctx = canvas.getContext('2d');
    ctx.fillStyle = Config.MARKER_COLOR;
    ctx.strokeStyle = Config.MARKER_COLOR;
    ctx.lineWidth = Config.MARKER_LINE_WIDTH;

    var self = this;
    jQuery.each(_annotations, function(idx, annotation) {
      // TODO optimize so that stuff outside the visible area isn't drawn
      if (annotation.id != _currentHighlight.id)
        _drawOne(annotation, extent, pixelRatio / resolution, ctx);
    });
    
    if (_currentHighlight) {
      ctx.fillStyle = Config.MARKER_HI_COLOR;
      ctx.strokeStyle = Config.MARKER_HI_COLOR;
      _drawOne(_currentHighlight, extent, pixelRatio / resolution, ctx);
    }
    
    return canvas;
  };
  
  var _highlightAnnotation = function(annotation, x, y) {
    _currentHighlight = annotation;    
    
    if (annotation) {
      EventBroker.fireEvent('onMouseOverAnnotation', annotation);
      // tooltip.show(annotation, x, y);
    } else {
      // EventBroker.fireEvent('onMouseOutOfAnnotation', annotation);
      // tooltip.hide();
    }
    
    _mapLayer.getSource().dispatchChangeEvent();
  }
  
  var _onMouseMove = function(e) {
    // TODO optimize with a quadtree
    var maxDistance = _map.getResolution() * 10;
      
    // TODO cover the whole annotation area, not just the anchor dot
    var hovered = [];
    jQuery.each(_annotations, function(idx, annotation) {
      var geometry = annotation.shapes[0].geometry;
      var dx = Math.abs(e.coordinate[0] - geometry.x);
      var dy = Math.abs(e.coordinate[1] + geometry.y);        
      if (dx < maxDistance && dy < maxDistance)
        hovered.push(annotation);
    });
      
    // TODO only redraw on highlight *change*
    if (hovered.length > 0) {
      if (_currentHighlight) {
        if (_currentHighlight.id != hovered[0].id) {
          // Change highlight from one annotation to next
          _highlightAnnotation(hovered[0], e.pixel[0], e.pixel[1]);
        }
      } else {
        // No previous highlight - highlight annotation under mouse
        _highlightAnnotation(hovered[0], e.pixel[0], e.pixel[1]);
      }
    } else {
      if (_currentHighlight) {
        // No annotation under mouse - clear highlights
        _highlightAnnotation(false);
      }
    }
  };
  
  var _onClick = function(e) {
    if (_currentHighlight)
      EventBroker.fireEvent('onEditAnnotation', _currentHighlight);
  };
  
  /** Public methods **/
  
  AnnotationLayer.prototype.addAnnotations = function(a) {
    if (jQuery.isArray(a))
      _annotations = jQuery.merge(_annotations, a);
    else
      _annotations.push(a);
    
    _mapLayer.getSource().dispatchChangeEvent();
  };
  
  AnnotationLayer.prototype.removeAnnotation = function(id) {
    _annotations = jQuery.grep(_annotations, function(a) {
      return a.id != id;
    });
    
    _mapLayer.getSource().dispatchChangeEvent();
  };
  
  return AnnotationLayer;
  
});
