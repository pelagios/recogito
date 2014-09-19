define(['config', 'imageannotation/annotations', 'imageannotation/utils', 'imageannotation/tooltip', 'imageannotation/events'], function(Config, Annotations, Utils, Tooltip, EventBroker) {
  
  var _map, 
      _mapLayer,
      _annotations = new Annotations(),
      _eventBroker = new EventBroker(),
      _tooltip = new Tooltip(_eventBroker),
      _currentHighlight = false; // Private fields
     
  var TWO_PI = 2 * Math.PI; // Shortcut
  
  var AnnotationLayer = function(map) {
    _map = map;
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
    var rect = jQuery.map(Annotations.getRect(annotation), function(pt) {
      return { x: scale * (pt.x - extent[0]), y: scale * (pt.y + extent[3]) }; 
    });
       
    ctx.beginPath();
    ctx.arc(rect[0].x, rect[0].y, Config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
    ctx.fill();
    ctx.closePath();
        
    ctx.globalAlpha = 0.3;
    ctx.beginPath();
    ctx.moveTo(rect[0].x, rect[0].y);
    ctx.lineTo(rect[1].x, rect[1].y);
    ctx.lineTo(rect[2].x, rect[2].y);
    ctx.lineTo(rect[3].x, rect[3].y);
    ctx.fill();
    ctx.closePath();
    ctx.globalAlpha = 1;
        
    ctx.beginPath();
    ctx.moveTo(rect[0].x, rect[0].y);
    ctx.lineTo(rect[1].x, rect[1].y);
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
    jQuery.each(_annotations.getAll(), function(idx, annotation) {
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
      _eventBroker.fireEvent('onMouseOverAnnotation', { annotation: annotation, x: x, y: y });
    } else {
      _eventBroker.fireEvent('onMouseOutOfAnnotation', { x: x, y: y });
    }
    
    _mapLayer.getSource().dispatchChangeEvent();
  }
  
  var _onMouseMove = function(e) {
    // var maxDistance = _map.getResolution() * 10;
    
    var hovered = _annotations.getAnnotationsAt(e.coordinate[0], - e.coordinate[1]);
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
        // No more annotation under mouse - clear highlights
        _highlightAnnotation(false);
      }
    }
  };
  
  var _onClick = function(e) {
    if (_currentHighlight)
      _eventBroker.fireEvent('onEditAnnotation', _currentHighlight);
  };
  
  /** Public methods **/
  
  AnnotationLayer.prototype.addAnnotations = function(a) {
    _annotations.add(a);
    _mapLayer.getSource().dispatchChangeEvent();
  };
  
  AnnotationLayer.prototype.removeAnnotation = function(id) {
    _annotations.remove(id);
    _mapLayer.getSource().dispatchChangeEvent();
  };
  
  return AnnotationLayer;
  
});
