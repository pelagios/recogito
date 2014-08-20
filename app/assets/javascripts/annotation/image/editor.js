define([], function() {
  
  var element;
  
  var Editor = function() {
    var template =
      '<div class="editor">' +
      '  <div class="last-modification">' +
      '    <span class="username"></span><span class="ago"></span>' +
      '  </div>' +
      '</div>';
      
    element = $(template);
    element.hide();
    $('#annotation-area').append(element);
  };
  
  Editor.prototype.show = function(annotation, x, y) {
    element.find('.username').html(annotation.last_edit.username);
    element.find('.ago').html($.timeago(new Date(annotation.last_edit.timestamp)));
    element.css({ left: x, top: y });
    element.show();
  }
  
  Editor.prototype.hide = function() {
    element.hide();
  }
  
  return Editor;
  
});
