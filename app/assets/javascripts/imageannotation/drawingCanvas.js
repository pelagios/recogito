define(['imageannotation/config', 'imageannotation/events'], function(Config, Events) {
  
  var DrawingCanvas = function(canvasId, viewer, eventBroker) {
    
    var TWO_PI = 2 * Math.PI,  // Shorthand
        MIN_DRAG_TIME = 300,   // Minimum duration of an annotation drag (milliseconds)
        MIN_LINE_LENGTH = 10,  // Minimum length of a baseline 
        
        /** Drawing components **/
        canvas = document.getElementById(canvasId),
        ctx = canvas.getContext('2d'),
        
        /** Painting state flags **/
        painting = false,
        extrude = false,
        
        /** Parameters of the current drawing **/
        anchorX,       // Anchor point
        anchorY,       
        baseEndX,      // Baseline end
        baseEndY,      
        oppositeX,     // Coordinate diagonally opposite the anchor point
        oppositeY,
        lastClicktime, // Time of last mousedown event
        
        /** Helper function to compute the length of a vector **/
        len = function(x1, y1, x2, y2) {
          if (y1)
            // Treat input as a tuple of coordinates
            return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
          else
            // Treat input as a vector
            return Math.sqrt(x1[0] * x1[0] + x1[1] * x1[1]);
        },
        
        /** Helper function to normalize a vector **/
        normalize = function(vector) {
          var l = len(vector);
          return [ vector[0] / l, vector[1] / l ];
        },
    
        /** Helper function to compute the angle between two vectors **/
        getAngleBetween = function(a, b) {
          var dotProduct = a[0] * b[0] + a[1] * b[1];
          return Math.acos(dotProduct);
        },
        
        /** Helper function to reset the canvas, e.g. after a window resize **/
        reset = function() {
          canvas.width = $(canvas).width();
          canvas.height = $(canvas).height();
          ctx.strokeStyle = Config.MARKER_RED;
          ctx.lineWidth = Config.MARKER_LINE_WIDTH;    
        },
        
        /** Show the canvas **/
        show = function() { canvas.style.pointerEvents = 'auto'; },
        
        /** Hide the canvas **/
        hide = function() { canvas.style.pointerEvents = 'none'; },
        
        /** Cancels drawing **/
        cancel = function() {
          painting = false;
          extrude = false;
          ctx.clearRect(0, 0, canvas.width, canvas.height);
        },
        
        /** Mouse down handler **/
        onMouseDown = function(e) {  
          lastClicktime = new Date().getTime();
      
          if (extrude) {
            // Were done.
            extrude = false;
            painting = false;
        
            // TODO could be optimized - we don't necessarily need to clear the entire canvas
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        
            var imageAnchorCoords = viewer.getCoordinateFromPixel([anchorX, anchorY]);
            var imageEndCoords = viewer.getCoordinateFromPixel([baseEndX, baseEndY]);
            var imageOppositeCoords = viewer.getCoordinateFromPixel([oppositeX, oppositeY]);
          
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
          
            var annotation = {
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
            };

            eventBroker.fireEvent(Events.ANNOTATION_CREATED, annotation);
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
        },
  
        /** Mouse move handler **/
        onMouseMove = function(e) {
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
                normal[0] * len(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse))),
                normal[1] * len(toMouse) * Math.cos(getAngleBetween(normal, normalize(toMouse)))
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
        },
        
        /** Mouse up handler **/
        onMouseUp = function(e) {    
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
      };
        
    $(canvas).mousedown(onMouseDown); 
    $(canvas).mousemove(onMouseMove);
    $(canvas).mouseup(onMouseUp);
    
    eventBroker.addHandler(Events.SWITCH_TO_ANNOTATE, show);
    eventBroker.addHandler(Events.SWITCH_TO_NAVIGATE, hide);
    eventBroker.addHandler(Events.ESCAPE, cancel);
    
    reset();    
    $(window).on('resize', reset);
  };

  return DrawingCanvas;
  
});
