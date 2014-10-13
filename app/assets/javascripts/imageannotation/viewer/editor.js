define(['imageannotation/config', 'imageannotation/events'], function(Config, Events) {
  
  var Editor = function(div, viewer, eventBroker) {    
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
            '  <div class="buttons">' +
            '    <button class="button ok"><span class="icon">&#xf00c;</span> OK</button>' +
            '    <button class="button cancel"><span class="icon">&#xf05e;</span> Cancel</button>' +
            '    <button class="button red delete"><span class="icon">&#xf00d;</span> Delete Annotation</button>' +
            '  </div>' +
            '</div>'),
        transcriptionInput = controls.find('#transcription'),
        commentInput = controls.find('#comment'),
        
        /** Saves the annotation to the server **/
        updateAnnotation = function() {
          var transcription = transcriptionInput.val(),
              comment = commentInput.val();

          currentAnnotation.corrected_toponym = transcription;   
          currentAnnotation.comment = comment;
          currentAnnotation.status = 'NOT_VERIFIED';  
          
          eventBroker.fireEvent(Events.ANNOTATION_UPDATED, currentAnnotation);
          hide();
        },
        
        /** Deletes the annotation **/
        deleteAnnotation = function() {
          eventBroker.fireEvent(Events.ANNOTATION_DELETED, currentAnnotation);
          hide();
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
        
        /** Opens the editor controls panel **/
        showControls = function(bounds, annotation) {
          controls.css({
            left: (bounds.left + 1) + 'px',
            top: (bounds.top + bounds.height) + 'px',
            minWidth: bounds.width + 'px'
          });
          
          var transcription = (annotation.corrected_toponym) ? annotation.corrected_toponym : annotation.toponym;
          if (transcription)
            transcriptionInput.val(transcription);
          else 
            transcriptionInput.val('');
      
          if (annotation.comment)
            commentInput.val(annotation.comment);
          else 
            commentInput.val('');   
          
          controls.show();
          transcriptionInput.focus();
        },
  
        /** Just a facade that opens the mask and the controls panel **/
        show = function(annotation) {
          currentAnnotation = annotation;
          var bounds = viewer.toViewportBounds(annotation, 50);  
          showMask(bounds);
          showControls(bounds, annotation);
        },
        
        /** Hide mask and controls panel **/
        hide = function() {
          currentAnnotation = false;
          mask.hide();
          controls.hide();
        };
    
    
    // Hide editor elements
    mask.hide();
    controls.hide();

    // Set up events
    viewer.on(['moveend', 'postrender'], function(e) {
      if (currentAnnotation)
        show(currentAnnotation);
    });
    
    eventBroker.addHandler(Events.EDIT_ANNOTATION, show);
    
    controls.keydown(function(e) {
      if (e.which === 27) // Escape
        hide();
    });
    controls.on('keypress', 'input', function(e) {
      if (e.which === 13) // Enter on the input field
        updateAnnotation();
    });
    
    controls.find('.ok').click(updateAnnotation);
    controls.find('.delete').click(deleteAnnotation);
    controls.find('.cancel').click(hide); 

    div.append(mask);
    div.append(controls);
  };
  
  return Editor;
  
});
