define(['config'], function(config) {
  
  var layer,                // the map layer
      annotations = [],     // the annotations
      TWO_PI = 2 * Math.PI; 
  
  var redraw = function(extent, resolution, pixelRatio, size, projection) {    
    var canvas = document.createElement('canvas');
    canvas.width = size[0];
    canvas.height = size[1];

    var ctx = canvas.getContext('2d');
    ctx.fillStyle = config.MARKER_COLOR;
    ctx.strokeStyle = config.MARKER_COLOR;
    ctx.lineWidth = config.MARKER_LINE_WIDTH;

    $.each(annotations, function(idx, annotation) {  
      var viewportX = (annotation.x - extent[0]) / resolution;
      var viewportY = (annotation.y + extent[3]) / resolution;
      var viewportLength = annotation.l / resolution;
      
      var dx = Math.cos(annotation.a) * viewportLength;
      var dy = Math.sin(annotation.a) * viewportLength;
      
      ctx.beginPath();
      ctx.arc(viewportX, viewportY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
      ctx.fill();
      ctx.closePath();
        
      ctx.beginPath();
      ctx.moveTo(viewportX, viewportY);
      ctx.lineTo(viewportX + dx, viewportY - dy);
      ctx.stroke();
      ctx.closePath();
    });
    
    return canvas;
  }
  
  var AnnotationLayer = function(map) {
    layer = new ol.layer.Image({
      source: new ol.source.ImageCanvas({
        canvasFunction: redraw,
        projection: 'ZOOMIFY'
      })
    });
    map.addLayer(layer);
  }
  
  AnnotationLayer.prototype.addAnnotation = function(annotation) {
    annotations.push(annotation);
    layer.getSource().dispatchChangeEvent();
  }
  
  return AnnotationLayer;
  
});
