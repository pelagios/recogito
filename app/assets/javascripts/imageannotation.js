define('config', [], function() { 
  config.MARKER_COLOR = '#0000cc';
  config.MARKER_HI_COLOR = '#fff000';
  config.MARKER_CIRCLE_RADIUS = 5;
  config.MARKER_LINE_WIDTH = 3;
  return config; 
});

require(['annotation/image/ol-map', 'annotation/image/drawing-canvas', 'annotation/image/storage'], function(Map, DrawingCanvas, Storage) {
      
  var btnNavigate   = $('.navigate'),
      btnAnnotate   = $('.annotate'),
      map           = new Map('ol-viewer'),
      drawingCanvas = new DrawingCanvas('drawing-canvas', map),
      storage       = new Storage();
      
  var switchToNavigate = function() {
        drawingCanvas.hide();
        btnNavigate.addClass('selected');
        btnAnnotate.removeClass('selected');
      },
      switchToAnnotate = function() {
        drawingCanvas.show();
        btnNavigate.removeClass('selected');
        btnAnnotate.addClass('selected');
      };
  
  btnNavigate.click(function(e) { switchToNavigate() });
  
  btnAnnotate.click(function(e) { switchToAnnotate() });
  
  $(document).keyup(function(e) {
    if (e.keyCode == 32) {
      if (btnAnnotate.hasClass('selected'))
        switchToNavigate();
      else
        switchToAnnotate();
    }
  });
      
  drawingCanvas.on('annotationCreated', function(annotation) { 
    map.addAnnotations(annotation);
    storage.create(annotation);
  });
  
  var annotations = storage.loadAll(function(annotations) {
    map.addAnnotations(annotations);
  });
  
});
