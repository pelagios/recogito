define(['imageannotation/config', 
        'imageannotation/events',
        'imageannotation/viewer/popup',
        'imageannotation/viewer/editor',
        'imageannotation/viewer/annotations'], function(Config, Events, Popup, Editor, Annotations) {
  
  var layer;
  
  var AnnotationLayer = function(div, viewer, eventBroker) {
    var TWO_PI = 2 * Math.PI, // shorthand
    
        /** Annotations **/
        annotations = new Annotations(),
        currentHighlight = false,
        
        /** UI components **/
        popup = new Popup(div, eventBroker),
        editor = new Editor(div, viewer, eventBroker),
        
        /** Helper function to draws a single annotation onto the canvas **/
        drawOne = function(annotation, extent, scale, ctx, color) {    
          var rect = jQuery.map(Annotations.getRect(annotation), function(pt) {
            return { x: scale * (pt.x - extent[0]), y: scale * (pt.y + extent[3]) }; 
          });
          rect.push(rect[0]); // Close path

          // Helper function to trace the rectangle path     
          var traceRect = function() {
            ctx.moveTo(rect[0].x, rect[0].y);
            ctx.lineTo(rect[1].x, rect[1].y);
            ctx.lineTo(rect[2].x, rect[2].y);
            ctx.lineTo(rect[3].x, rect[3].y);  
            ctx.lineTo(rect[0].x, rect[0].y);    
          };
    
          // Draw rectangle
          ctx.fillStyle = color;
          ctx.strokeStyle = color;
          ctx.lineWidth = 1;
          ctx.globalAlpha = Config.MARKER_OPACITY;
          ctx.beginPath();
          traceRect();
          ctx.stroke();
          ctx.fill();
          ctx.closePath();
    
          // Draw rectangle outline
          ctx.globalAlpha = Config.MARKER_OPACITY * 1.5;
          ctx.beginPath();
          traceRect();
          ctx.stroke();
          ctx.closePath();
          ctx.globalAlpha = 1;
          
          // Draw anchor dot
          ctx.beginPath();
          ctx.arc(rect[0].x, rect[0].y, Config.MARKER_CIRCLE_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();
    
          // Draw aseline
          ctx.lineWidth = Config.MARKER_LINE_WIDTH;
          ctx.strokeStyle = color;
          ctx.beginPath();
          ctx.moveTo(rect[0].x, rect[0].y);
          ctx.lineTo(rect[1].x, rect[1].y);
          ctx.stroke();
          ctx.closePath();
        },
          
        /** Drawing loop that renders all annotations onto the viewer layer **/
        redrawAll = function(extent, resolution, pixelRatio, size, projection) {                
          var canvas = document.createElement('canvas');
          canvas.width = size[0];
          canvas.height = size[1];

          var ctx = canvas.getContext('2d');

          var self = this;
          jQuery.each(annotations.getAll(), function(idx, annotation) {
            // TODO optimize so that stuff outside the visible area isn't drawn
            if (annotation.id != currentHighlight.id) {
              var color;
              if (Annotations.getTranscription(annotation)) {
                // Colour-code according to status
                if (annotation.status === 'NOT_VERIFIED')
                  color = Config.MARKER_GREY;
                else if (annotation.status === 'VERIFIED')
                  color = Config.MARKER_GREEN;
                else
                  color = Config.MARKER_YELLOW;
              } else {
                // Needs transcription - mark as red
                color = Config.MARKER_RED;
              }
              drawOne(annotation, extent, pixelRatio / resolution, ctx, color);
            }
          });
    
          if (currentHighlight) {
            drawOne(currentHighlight, extent, pixelRatio / resolution, ctx, Config.MARKER_HI_COLOR);
          }
    
          return canvas;
        },
        
        /** Helper function to highlights the current annotation **/
        highlightAnnotation = function(annotation, x, y) {
          currentHighlight = annotation;    
    
          if (annotation) {
            document.body.style.cursor = 'pointer';
            eventBroker.fireEvent(Events.MOUSE_OVER_ANNOTATION, { annotation: annotation, x: x, y: y });
          } else {
            document.body.style.cursor = 'auto';
            eventBroker.fireEvent(Events.MOUSE_LEAVE_ANNOTATION, { x: x, y: y });
          }
    
          layer.getSource().dispatchChangeEvent();
        },
        
        /** Handler that performs collision detection and highlighting **/
        onMouseMove = function(e) {    
          var hovered = annotations.getAnnotationsAt(e.coordinate[0], - e.coordinate[1]);
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
            if (currentHighlight) {
              // No more annotation under mouse - clear highlights
              highlightAnnotation(false);
            }
          }
        },
        
        /** Click handler that fires the 'edit' event in case we have a highlight **/
        onClick = function(e) {
          if (currentHighlight)
            eventBroker.fireEvent(Events.EDIT_ANNOTATION, currentHighlight);
        },
        
        /** Batch-adds annotations to the view layer **/
        addAnnotations = function(a) {
          console.log(a);
          annotations.add(a);
          setTimeout(function() { layer.getSource().dispatchChangeEvent(); }, 50 ); 
        },
        
        /** Removes an annotation from the view layer **/
        removeAnnotation = function(a) {
          annotations.remove(a.id);
          layer.getSource().dispatchChangeEvent();
        };
        
    layer = new ol.layer.Image({
      source: new ol.source.ImageCanvas({
        canvasFunction: redrawAll,
        projection: 'ZOOMIFY'
      })
    });
        
    viewer.on('pointermove', onMouseMove);
    viewer.on('singleclick', onClick);
            
    eventBroker.addHandler(Events.STORE_ANNOTATIONS_LOADED, addAnnotations);   
    eventBroker.addHandler(Events.ANNOTATION_CREATED, addAnnotations); 
    eventBroker.addHandler(Events.ANNOTATION_UPDATED, function() { layer.getSource().dispatchChangeEvent(); });
    eventBroker.addHandler(Events.ANNOTATION_DELETED, removeAnnotation);
  };
  
  AnnotationLayer.prototype.getLayer = function() { return layer };
   
  return AnnotationLayer; 

});
