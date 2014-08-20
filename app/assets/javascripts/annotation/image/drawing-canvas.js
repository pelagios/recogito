define(['config'], function(config) {
  
  var TWO_PI = 2 * Math.PI;
      
  var canvasEl,          // canvas DOM element
      ctx,               // 2D drawing context
      painting = false,  // Flag indicating whether painting is in progress or not
      anchorX,           // Current anchor point - X coord
      anchorY,           // Current anchor point - Y coord
      map,               // Reference to the OpenLayers map (for coordinate translation!)
      handlers = [];     // Registered event handlers
  
  var DrawingCanvas = function(canvasId, olMap) {
    canvasEl = document.getElementById(canvasId);  
    canvasEl.width = $(canvasEl).width();
    canvasEl.height = $(canvasEl).height();
    
    map = olMap;
  
    ctx = canvasEl.getContext('2d');
    ctx.fillStyle = config.MARKER_COLOR;
    ctx.strokeStyle = config.MARKER_COLOR;
    ctx.lineWidth = config.MARKER_LINE_WIDTH;    

    this.width = canvasEl.width;
    this.height = canvasEl.height;
    
    painting = false;
    
    _addMouseDownHandler(this);
    _addMouseMoveHandler(this);
    _addMouseUpHandler(this);
  }
  
  DrawingCanvas.prototype.show = function() {
    canvasEl.style.pointerEvents = 'auto';
  }
  
  DrawingCanvas.prototype.hide = function() {
    canvasEl.style.pointerEvents = 'none';
  }
  
  DrawingCanvas.prototype.on = function(event, handler) {
    handlers[event] = handler;
  }
    
  var _addMouseDownHandler = function(self) {  
    $(canvasEl).mousedown(function(e) {
      painting = true;
      anchorX = e.offsetX;
      anchorY = e.offsetY;
    
      ctx.beginPath();
      ctx.arc(e.offsetX, e.offsetY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
      ctx.fill();
      ctx.closePath();
    });
  }
  
  var _addMouseMoveHandler = function(self) {
    $(canvasEl).mousemove(function(e) {
      if (painting) {
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, self.width, self.height);
      
        ctx.beginPath();
        ctx.arc(anchorX, anchorY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
        ctx.fill();
        ctx.closePath();
      
        ctx.beginPath();
        ctx.moveTo(anchorX, anchorY);
        ctx.lineTo(e.offsetX, e.offsetY);
        ctx.stroke();
        ctx.closePath();
      }
    });
  }
  
  var _addMouseUpHandler = function(self) {
    $(canvasEl).mouseup(function(e) {
      if (painting) {
        painting = false;
        
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, self.width, self.height);
        
        if (handlers['annotationCreated']) {
          var imageAnchorCoords = map.getCoordinateFromPixel([anchorX, anchorY]);
          var imageEndCoords = map.getCoordinateFromPixel([e.offsetX, e.offsetY]);
          
          var dx = imageEndCoords[0] - imageAnchorCoords[0];
          var dy = imageEndCoords[1] - imageAnchorCoords[1];
          
          var baselineAngle = Math.atan(dy / dx); 
          var baselineLength = Math.sqrt(dx * dx + dy * dy);
          
          handlers['annotationCreated']({ 
            x: imageAnchorCoords[0],
            y: - imageAnchorCoords[1],
            a: baselineAngle,
            l: baselineLength 
          });
        }        
      }
    });
  }
  
  return DrawingCanvas;
  
});
