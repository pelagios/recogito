require(['imageannotation/ol-map', 'imageannotation/drawing-canvas', 'imageannotation/storage'], function(Map, DrawingCanvas, Storage) {
      
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
    if (e.target.tagName !== 'INPUT' && e.keyCode == 32) {
      if (btnAnnotate.hasClass('selected'))
        switchToNavigate();
      else
        switchToAnnotate();
    }
  });
      
  drawingCanvas.on('annotationCreated', function(annotation) { 
    storage.create(annotation, function(annotation) {
      map.addAnnotations(annotation);
    });    
  });
  
  var annotations = storage.loadAll(function(annotations) {
    map.addAnnotations(annotations);
  });
  
});
