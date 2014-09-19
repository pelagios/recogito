define([], function() {
    
  var element;
  
  var Tooltip = function(eventBroker) {
    var template =
      '<div class="tooltip">' +
      '  <div class="transcription"></div>' +
      '  <div class="last-modification">' +
      '    <span class="username"></span><span class="ago"></span>' +
      '  </div>' +
      '</div>';
      
    element = $(template);
    element.hide();
    $('#annotation-area').append(element);
    
    eventBroker.addHandler('onMouseOverAnnotation', show);
    eventBroker.addHandler('onMouseOutOfAnnotation', hide);
  };
  
  var show = function(e) {    
    var transcription = (e.annotation.corrected_toponym) ? e.annotation.corrected_toponym : e.annotation.toponym;
    if (transcription)
      element.find('.transcription').html(transcription);
    else 
      element.find('.transcription').html('');
      
    element.find('.username').html(e.annotation.last_edit.username);
    element.find('.ago').html($.timeago(new Date(e.annotation.last_edit.timestamp)));
    element.css({ left: e.x, top: e.y });
    element.show();
  }
  
  var hide = function() {
    element.hide();
  }
  
  return Tooltip;
  
});
