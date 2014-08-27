define(['config', 'annotation/image/tooltip', 'annotation/image/editor', 'annotation/image/utils'], function(config, Tooltip, Editor, Utils) {
  
  var map,                        // the map
      layer,                      // the map layer
      annotations = new Object(), // the annotations
      currentHighlight = false,   // currently highlighted annotation (if any)
      tooltip,
      editor,
      TWO_PI = 2 * Math.PI; 
  
  var highlightAnnotation = function(annotation, x, y) {
    currentHighlight = annotation;    
    if (annotation) {
      tooltip.show(annotation, x, y);
    } else {
      tooltip.hide();
    }
    
    layer.getSource().dispatchChangeEvent();
  }
  
  var redraw = function(extent, resolution, pixelRatio, size, projection) {    
    var canvas = document.createElement('canvas');
    canvas.width = size[0];
    canvas.height = size[1];

    var ctx = canvas.getContext('2d');
    ctx.fillStyle = config.MARKER_COLOR;
    ctx.strokeStyle = config.MARKER_COLOR;
    ctx.lineWidth = config.MARKER_LINE_WIDTH;
    
    var draw = function(annotation) {
      var geometry = annotation.shapes[0].geometry;
      var viewportX = (geometry.x - extent[0]) / resolution;
      var viewportY = (geometry.y + extent[3]) / resolution;
      var viewportLength = geometry.l / resolution;
      
      var dx = Math.cos(geometry.a) * viewportLength;
      var dy = Math.sin(geometry.a) * viewportLength;
      
      ctx.beginPath();
      ctx.arc(viewportX, viewportY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
      ctx.fill();
      ctx.closePath();
        
      ctx.beginPath();
      ctx.moveTo(viewportX, viewportY);
      ctx.lineTo(viewportX + dx, viewportY - dy);
      ctx.stroke();
      ctx.closePath();      
    };

    for (var id in annotations) {
      if (id != currentHighlight.id)
        draw(annotations[id]);
    }
    
    if (currentHighlight) {
      ctx.fillStyle = config.MARKER_HI_COLOR;
      ctx.strokeStyle = config.MARKER_HI_COLOR;
      draw(currentHighlight);
    }
    
    return canvas;
  }
  
  var AnnotationLayer = function(olMap) {
    var self = this;
    
    map = olMap;
    tooltip = new Tooltip();
    editor = new Editor(map);
    
    layer = new ol.layer.Image({
      source: new ol.source.ImageCanvas({
        canvasFunction: redraw,
        projection: 'ZOOMIFY'
      })
    });
    map.addLayer(layer);
    
    /** THIS IS A TEMPORARY HACK ONLY **/
    map.on('pointermove', function(e) {
      var maxDistance = map.getResolution() * 10;
      
      var hovered = [];
      for (var id in annotations) {
        var geometry = annotations[id].shapes[0].geometry;
        var dx = Math.abs(e.coordinate[0] - geometry.x);
        var dy = Math.abs(e.coordinate[1] + geometry.y);        
        if (dx < maxDistance && dy < maxDistance)
          hovered.push(annotations[id]);
      }
      
      if (hovered.length > 0) {
        if (currentHighlight) {
          if (currentHighlight.id != hovered[0].id) {
            // Change highlight from one annotation to next
            highlightAnnotation(hovered[0], e.pixel[0], e.pixel[1]);
          }
        } else {
          // No previous highlight - highlight annotation under mouse
          highlightAnnotation(hovered[0], e.pixel[0], e.pixel[1]);
        }
      } else {
        // No annotation under mouse - clear highlights
        highlightAnnotation(false);
      }
    });
    /** THIS IS A TEMPORARY HACK ONLY **/
    
    map.on('singleclick', function(e) {
      if (currentHighlight) {
        self.moveTo(currentHighlight);
        editor.show(currentHighlight);
      }
    });
  }
  
  AnnotationLayer.prototype.addAnnotations = function(a) {
    if ($.isArray(a)) {
      $.each(a, function(idx, annotation) {
        annotations[annotation.id] = annotation;
      });
    } else {
      annotations[a.id] = a;
    }
    
    layer.getSource().dispatchChangeEvent();
  }
  
  AnnotationLayer.prototype.removeAnnotation = function(id) {
    delete annotations[id];
    layer.getSource().dispatchChangeEvent();
  }
  
  AnnotationLayer.prototype.moveTo = function(annotation) {
    var bounds = Utils.getBounds(annotation);
    
    var extent = [
      bounds.left ,
      - bounds.top,
      bounds.left + bounds.width,
      bounds.height - bounds.top
    ];
    
    var size = map.map.getSize();
    size = [size[0] / 2, size[1] / 2];

    map.map.getView().fitExtent(extent, size);
  }
  
  return AnnotationLayer;
  
});
