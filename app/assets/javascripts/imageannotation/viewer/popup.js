define(['imageannotation/events', 'imageannotation/viewer/annotations'], function(Events, Annotations) {
  
  /** A popup bubble to displays information about annotations **/
  var Popup = function(parent, eventBroker) {
    var  currentId,
         element =
          $('<div class="tooltip">' +
          '  <div class="transcription"></div>' +
          '  <div class="last-modification">' +
          '    <span class="username"></span><span class="ago"></span>' +
          '  </div>' +
          '</div>'),
          
        fillTemplate = function(annotation) {
          var transcription = (annotation.corrected_toponym) ? annotation.corrected_toponym : annotation.toponym;
          if (transcription)
            element.find('.transcription').html(transcription);
          else 
            element.find('.transcription').html('');
            
          if (annotation.last_edit) {
            element.find('.username').html(annotation.last_edit.username);
            element.find('.ago').html($.timeago(new Date(annotation.last_edit.timestamp)));
          } else {
            element.find('.username, .ago').html('');
          }
        },
        
        show = function(e) {  
          var currentId = e.annotation.id;
      
          // Fetch annotation details via API
          Annotations.fetchDetails(e.annotation, function(annotation) {
            if (currentId == annotation.id)
                fillTemplate(annotation);
          });
          
          fillTemplate(e.annotation);
                    
          element.css({ left: e.x, top: e.y });
          element.show();
        },
        
        hide = function() {
          element.hide();
        };
        
    element.hide();
    parent.append(element);

    eventBroker.addHandler(Events.MOUSE_OVER_ANNOTATION, show);
    eventBroker.addHandler(Events.MOUSE_LEAVE_ANNOTATION, hide);
  };
    
  return Popup;
  
});
