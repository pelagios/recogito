define(['config'], function(config) {
  
  var TWO_PI = 2 * Math.PI;
      
  var canvasEl,          // canvas DOM element
      ctx,               // 2D drawing context
      painting = false,  // Flag indicating whether painting is in progress or not
      extrude = false,   // Flag indidcating whether we're in extrusion mode
      anchorX,           // Current anchor point - X coord
      anchorY,           // Current anchor point - Y coord
      baseEndX,          // Current baseline end - X coord
      baseEndY,          // Current baseline end - Y coord
      oppositeX,
      oppositeY,
      map,               // Reference to the OpenLayers map (for coordinate translation!)
      handlers = [];     // Registered event handlers
  
  var DrawingCanvas = function(canvasId, olMap) {
    canvasEl = document.getElementById(canvasId);  
    canvasEl.width = $(canvasEl).width();
    canvasEl.height = $(canvasEl).height();
    
    map = olMap;
  
    ctx = canvasEl.getContext('2d');
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
      if (extrude) {
        // DONE
        extrude = false;
        painting = false;
        
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, self.width, self.height);
        
        if (handlers['annotationCreated']) {
          var imageAnchorCoords = map.getCoordinateFromPixel([anchorX, anchorY]);
          var imageEndCoords = map.getCoordinateFromPixel([baseEndX, baseEndY]);
          var imageOppositeCoords = map.getCoordinateFromPixel([oppositeX, oppositeY]);
          
          var dx = imageEndCoords[0] - imageAnchorCoords[0];
          var dy = imageEndCoords[1] - imageAnchorCoords[1];
          var dh = [
            imageOppositeCoords[0] - imageEndCoords[0],
            imageOppositeCoords[1] - imageEndCoords[1]
          ];
          
          
          var corr = 0;
          if (dx < 0 && dy >= 0)
            corr = Math.PI
          else if (dx < 0 && dy < 0)
            corr = - Math.PI
           
          var baselineAngle = Math.atan(dy / dx) + corr;
          var baselineLength = Math.sqrt(dx * dx + dy * dy);
          var height = Math.sqrt(dh[0]*dh[0] + dh[1]*dh[1]);
          
          handlers['annotationCreated']({
            shapes: [{ 
              type: 'toponym',
              geometry: {
                x: imageAnchorCoords[0],
                y: - imageAnchorCoords[1],
                a: baselineAngle,
                l: baselineLength,
                h: height
              }
            }]
          });
        }
      } else {
        painting = true;
        anchorX = e.offsetX;
        anchorY = e.offsetY;
    
        ctx.fillStyle = config.MARKER_COLOR;
        ctx.beginPath();
        ctx.arc(e.offsetX, e.offsetY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
        ctx.fill();
        ctx.closePath();
      }
    });
  }
  
  var _addMouseMoveHandler = function(self) {
    var getLength = function(coord) {
      return Math.sqrt(coord[0] * coord[0] + coord[1] * coord[1]);
    };
    
    var normalize = function(vector) {
      var len = getLength(vector);
      return [ vector[0] / len, vector[1] / len ];
    };
    
    var getAngleBetween = function(a, b) {
      var dotProduct = a[0] * b[0] + a[1] * b[1];
      return Math.acos(dotProduct);
    };
    
    $(canvasEl).mousemove(function(e) {
      if (painting) {
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, self.width, self.height);
      
        if (extrude) {
          // Baseline vector (start to end)
          var delta = [ (baseEndX - anchorX), (baseEndY - anchorY) ];
          
          // Slope of the baseline normal
          var normal = normalize([-1 * delta[1], delta[0]]);
          
          // Vector baseline end to mouse
          var toMouse = [ e.offsetX - baseEndX, e.offsetY - baseEndY ];
          
          // Projection of toMouse onto normal
          var f = [
            normal[0] * getLength(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse))),
            normal[1] * getLength(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse)))
          ];
          
          oppositeX = baseEndX + f[0];
          oppositeY = baseEndY + f[1];
          
          ctx.fillStyle = config.MARKER_FILL;
          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(anchorX + f[0], anchorY + f[1]);
          ctx.lineTo(oppositeX, oppositeY);
          ctx.lineTo(baseEndX, baseEndY);
          ctx.fill();
          ctx.closePath();
          
          // Finished baseline
          ctx.fillStyle = config.MARKER_COLOR;
          ctx.beginPath();
          ctx.arc(anchorX, anchorY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();
      
          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(baseEndX, baseEndY);
          ctx.stroke();
          ctx.closePath();
        } else { 
          // Baseline
          ctx.fillStyle = config.MARKER_COLOR;
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
      }
    });
  }
  
  var _addMouseUpHandler = function(self) {
    $(canvasEl).mouseup(function(e) {
      if (painting) {
        baseEndX = e.offsetX;
        baseEndY = e.offsetY;
        extrude = true;
      }
    });
  }
  
  return DrawingCanvas;
  
});
