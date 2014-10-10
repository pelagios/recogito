define(['imageannotation/config'], function(config) {
  
  var TWO_PI = 2 * Math.PI,
      MIN_DRAG_TIME = 500;   // Minimum duration of an annotation drag (milliseconds)
      MIN_LINE_LENGTH = 10;  // Minimum length of a baseline
      
  var canvas,            // canvas DOM element
      ctx,               // 2D drawing context
      painting = false,  // Flag indicating whether painting is in progress or not
      extrude = false,   // Flag indidcating whether we're in extrusion mode
      anchorX,           // Current anchor point - X coord
      anchorY,           // Current anchor point - Y coord
      baseEndX,          // Current baseline end - X coord
      baseEndY,          // Current baseline end - Y coord
      oppositeX,
      oppositeY,
      lastClicktime,     // Time of last mousedown event
      map,               // Reference to the OpenLayers map (for coordinate translation!)
      handlers = [];     // Registered event handlers
      
  var len = function(x1, y1, x2, y2) {
    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
  };
  
  var DrawingCanvas = function(canvasId, olMap) {
    canvas = document.getElementById(canvasId);  
    map = olMap;
    ctx = canvas.getContext('2d');
    painting = false;
    
    var reset = function() {
      canvas.width = $(canvas).width();
      canvas.height = $(canvas).height();
      ctx.strokeStyle = config.MARKER_RED;
      ctx.lineWidth = config.MARKER_LINE_WIDTH;    
    };
    
    reset();
    
    _addMouseDownHandler();
    _addMouseMoveHandler();
    _addMouseUpHandler();
    
    // Make sure the canvas adapts to window size changes
    $(window).on('resize', reset);
  }
  
  DrawingCanvas.prototype.show = function() {
    canvas.style.pointerEvents = 'auto';
  }
  
  DrawingCanvas.prototype.hide = function() {
    canvas.style.pointerEvents = 'none';
  }
  
  DrawingCanvas.prototype.on = function(event, handler) {
    handlers[event] = handler;
  }
    
  var _addMouseDownHandler = function() {  
    $(canvas).mousedown(function(e) {
      lastClicktime = new Date().getTime();
      
      if (extrude) {
        // DONE
        extrude = false;
        painting = false;
        
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
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
        anchorX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
        anchorY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;
                
        ctx.fillStyle = config.MARKER_RED;
        ctx.beginPath();
        ctx.arc(anchorX, anchorY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
        ctx.fill();
        ctx.closePath();
      }
    });
  }
  
  var _addMouseMoveHandler = function() {
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
    
    $(canvas).mousemove(function(e) {
      if (painting) {
        // TODO could be optimized - we don't necessarily need to clear the entire canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      
        if (extrude) {
          // Baseline vector (start to end)
          var delta = [ (baseEndX - anchorX), (baseEndY - anchorY) ];
          
          // Slope of the baseline normal
          var normal = normalize([-1 * delta[1], delta[0]]);
          
          // Vector baseline end to mouse
          var offsetX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
          var offsetY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;
          var toMouse = [ offsetX - baseEndX, offsetY - baseEndY ];
          
          // Projection of toMouse onto normal
          var f = [
            normal[0] * getLength(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse))),
            normal[1] * getLength(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse)))
          ];
          
          oppositeX = baseEndX + f[0];
          oppositeY = baseEndY + f[1];
          
          ctx.globalAlpha = config.MARKER_OPACITY;
          ctx.fillStyle = config.MARKER_RED;
          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(anchorX + f[0], anchorY + f[1]);
          ctx.lineTo(oppositeX, oppositeY);
          ctx.lineTo(baseEndX, baseEndY);
          ctx.fill();
          ctx.closePath();
          ctx.globalAlpha = 1;
          
          // Finished baseline
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
          ctx.fillStyle = config.MARKER_RED;
          ctx.beginPath();
          ctx.arc(anchorX, anchorY, config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();
      
          var offsetX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
          var offsetY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;
          
          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(offsetX, offsetY);
          ctx.stroke();
          ctx.closePath();
        }
      }
    });
  }
  
  var _addMouseUpHandler = function() {    
    $(canvas).mouseup(function(e) {
      if (painting) {
        if ((new Date().getTime() - lastClicktime) < MIN_DRAG_TIME) {
          // Just reset
          painting = false;
          ctx.clearRect(0, 0, canvas.width, canvas.height);
        } else {
          baseEndX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
          baseEndY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;
          
          // Reject lines that are too short
          if (len(anchorX, anchorY, baseEndX, baseEndY) > MIN_LINE_LENGTH) {
            extrude = true;
          } else {
            painting = false;
            ctx.clearRect(0, 0, self.width, self.height);
          }
        }
      }
    });
  }
  
  return DrawingCanvas;
  
});
