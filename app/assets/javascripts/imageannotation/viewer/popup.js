define(['imageannotation/events'], function(Events) {
  
  /** A popup bubble to displays information about annotations **/
  var Popup = function(parent, eventBroker) {
    var element =
          $('<div class="tooltip">' +
          '  <div class="transcription"></div>' +
          '  <div class="last-modification">' +
          '    <span class="username"></span><span class="ago"></span>' +
          '  </div>' +
          '</div>'),
        
        show = function(e) {    
          var transcription = (e.annotation.corrected_toponym) ? e.annotation.corrected_toponym : e.annotation.toponym;
          if (transcription)
            element.find('.transcription').html(transcription);
          else 
            element.find('.transcription').html('');
      
          element.find('.username').html(e.annotation.last_edit.username);
          element.find('.ago').html($.timeago(new Date(e.annotation.last_edit.timestamp)));
          element.css({ left: e.x, top: e.y });
          element.show();
        };
        
    element.hide();
    parent.append(element);

    eventBroker.addHandler(Events.MOUSE_OVER_ANNOTATION, show);
    eventBroker.addHandler(Events.MOUSE_LEAVE_ANNOTATION, function() { element.hide(); });
  };
    
  return Popup;
  
});
