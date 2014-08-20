define('config', [], function() { 
  config.MARKER_COLOR = '#0000cc';
  config.MARKER_HI_COLOR = '#fff000';
  config.MARKER_CIRCLE_RADIUS = 5;
  config.MARKER_LINE_WIDTH = 3;
  return config; 
});

require(['ol-map', 'drawing-canvas', 'storage'], function(Map, DrawingCanvas, Storage) {
      
  var btnNavigate   = $('.navigate'),
      btnAnnotate   = $('.annotate'),
      map           = new Map('ol-viewer'),
      drawingCanvas = new DrawingCanvas('drawing-canvas', map),
      storage       = new Storage();
      
  btnNavigate.click(function(e) {
    drawingCanvas.hide();
    btnNavigate.addClass('selected');
    btnAnnotate.removeClass('selected');
  });
  
  btnAnnotate.click(function(e) {
    drawingCanvas.show();
    btnNavigate.removeClass('selected');
    btnAnnotate.addClass('selected');
  });
      
  drawingCanvas.on('annotationCreated', function(annotation) { 
    map.addAnnotations(annotation);
    storage.create(annotation);
  });
  
  var annotations = storage.loadAll(function(annotations) {
    map.addAnnotations(annotations);
  });
  
});
