define(['imageannotation/config', 'imageannotation/events', 'imageannotation/viewer/editorAutosuggest'], function(Config, Events, EditorAutoSuggest) {
  
  var Editor = function(parent, map, eventBroker) {    
    var currentAnnotation = false,
        mask = 
          $('<div class="editor-mask">' +
            '  <div class="mask top"></div>' +
            '  <div class="middle">' +
            '    <div class="mask left"></div><div class="window"></div><div class="mask right"></div>' +
            '  </div>' +
            '  <div class="mask bottom"></div>' +
            '</div>'),
        maskTop = mask.find('.top'),
        maskMiddle = mask.find('.middle'),
        maskLeft = mask.find('.left'),
        maskRight = mask.find('.right'),
        maskBottom = mask.find('.bottom'),
        maskWindow = mask.find('.window'),
        
        controls = 
          $('<div class="editor-controls">' +
            '  <span class="label">Transcription:</span>' +
            '  <input id="transcription" type="text">' +
            '  <span class="label">Comment:</span>' +
            '  <input id="comment" type="text">' +
            '  <input id="uri" type="hidden">' +
            '  <div class="buttons">' +
            '    <button class="button ok"><span class="icon">&#xf00c;</span> OK</button>' +
            '    <button class="button cancel"><span class="icon">&#xf05e;</span> Cancel</button>' +
            '    <button class="button red delete"><span class="icon">&#xf00d;</span> Delete Annotation</button>' +
            '  </div>' +
            '</div>'),
            
        autoSuggestContainer = $('<div class="autosuggest"></div>'),
        
        transcriptionInput = controls.find('#transcription'),
        commentInput = controls.find('#comment'),
        uriInput = controls.find('#uri'),
        
        autoSuggest = new EditorAutoSuggest(autoSuggestContainer, transcriptionInput, uriInput),
        
        /** Saves the annotation to the server **/
        updateAnnotation = function() {
          var transcription = transcriptionInput.val(),
              comment = commentInput.val(),
              uri = uriInput.val();

          currentAnnotation.corrected_toponym = transcription;   
          currentAnnotation.comment = comment;
          currentAnnotation.status = 'NOT_VERIFIED';  
          if (uri)
            currentAnnotation.gazetteer_uri = uri;
          
          eventBroker.fireEvent(Events.ANNOTATION_UPDATED, currentAnnotation);
          hide();
        },
        
        /** Deletes the annotation **/
        deleteAnnotation = function() {
          eventBroker.fireEvent(Events.ANNOTATION_DELETED, currentAnnotation);
          hide();
        },
        
        toViewportBounds = function(annotation, opt_buffer) {
          var buffer = (opt_buffer) ? opt_buffer : 0,   
              resolution = map.getView().getResolution(),
              geom = annotation.shapes[0].geometry,
    
              anchor = map.getPixelFromCoordinate([ geom.x, - geom.y ]),
              angle = geom.a - map.getView().getRotation(),
    
              a = { x: anchor[0], y: anchor[1] },
              b = {
                x: a.x + Math.cos(angle) * geom.l / resolution,
                y: a.y - Math.sin(angle) * geom.l / resolution
              },
              c = {
                x: b.x - geom.h / resolution * Math.sin(angle),
                y: b.y - geom.h / resolution * Math.cos(angle)
              },
              d = {
                x: a.x - geom.h / resolution * Math.sin(angle),
                y: a.y - geom.h / resolution * Math.cos(angle)      
              },
              
              top = Math.min(a.y, b.y, c.y, d.y),
              right = Math.max(a.x, b.x, c.x, d.x),
              bottom = Math.max(a.y, b.y, c.y, d.y),
              left = Math.min(a.x, b.x, c.x, d.x),

              bounds = {
                left: Math.round(left) - buffer,
                top: Math.round(top) - buffer,
                width: Math.round(right - left) + 2 * buffer,
                height: Math.round(bottom - top) + 2 * buffer
              };
          
          return bounds;
        },
            
        /** Opens the black mask around the selection **/
        showMask = function(bounds) {
          if (bounds.top > 0) {
            maskTop.height(bounds.top);
            maskMiddle.height(bounds.height);
          } else {
            maskTop.height(0);
            maskMiddle.height(bounds.height + bounds.top);           
          }

          if (bounds.left > 0) {
            maskLeft.width(bounds.left);
            maskWindow.width(bounds.width);
          } else {
            maskLeft.width(0);
            maskWindow.width(bounds.width + bounds.left);
          }          
                        
          maskRight.css('left', (bounds.left + bounds.width) + 'px');
          maskBottom.css('top', (bounds.top + bounds.height) + 'px');
          mask.show();            
        },
        
        /** Shorthand function, sets the position of the controls **/
        setControlsPosition = function(bounds) {          
          controls.css({
            left: (bounds.left + 1) + 'px',
            top: (bounds.top + bounds.height) + 'px',
            minWidth: bounds.width + 'px'
          });             
          
          autoSuggestContainer.css({
            left: (bounds.left + controls.outerWidth()) + 'px',
            top: bounds.top
          });       
        },
        
        /** Opens the editor controls panel **/
        showControls = function(bounds, annotation) {
          var transcription = (annotation.corrected_toponym) ? annotation.corrected_toponym : annotation.toponym;
          
          if (transcription)
            transcriptionInput.val(transcription);
          else 
            transcriptionInput.val('');
      
          if (annotation.comment)
            commentInput.val(annotation.comment);
          else 
            commentInput.val('');   
          
          setControlsPosition(bounds);
          controls.show();
          transcriptionInput.focus();
        },
  
        /** Just a facade that opens the mask and the controls panel **/
        show = function(annotation) {
          var bounds = toViewportBounds(annotation, 50);  
          currentAnnotation = annotation;
          showMask(bounds);
          showControls(bounds, annotation);
          autoSuggest.show(annotation);
        },
        
        /** Updates the position of the editor (mask and controls) **/
        updatePosition = function() {
          var bounds = toViewportBounds(currentAnnotation, 50);  
          showMask(bounds);
          setControlsPosition(bounds);
        },
        
        /** Hide mask and controls panel **/
        hide = function() {
          currentAnnotation = false;
          mask.hide();
          controls.hide();
          autoSuggest.hide();
          eventBroker.fireEvent(Events.ANNOTATION_EDIT_CANCELED);
        };
    
    // Hide editor elements
    mask.hide();
    controls.hide();

    // Set up events
    map.on(['moveend', 'postrender'], function(e) {
      if (currentAnnotation)
        updatePosition();
    });
    
    eventBroker.addHandler(Events.EDIT_ANNOTATION, show);
    eventBroker.addHandler(Events.ESCAPE, hide);
     
    controls.on('keypress', 'input', function(e) {
      if (e.which === 13) // Enter on the input field
        updateAnnotation();
    });
    
    controls.find('.ok').click(updateAnnotation);
    controls.find('.delete').click(deleteAnnotation);
    controls.find('.cancel').click(hide); 

    parent.append(mask);
    parent.append(controls);
    parent.append(autoSuggestContainer);
  };
  
  return Editor;
  
});
