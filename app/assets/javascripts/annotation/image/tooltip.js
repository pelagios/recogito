define([], function() {
  
  var element;
  
  var Tooltip = function() {
    var template =
      '<div class="tooltip">' +
      '  <div class="last-modification">' +
      '    <span class="username"></span><span class="ago"></span>' +
      '  </div>' +
      '</div>';
      
    element = $(template);
    element.hide();
    $('#annotation-area').append(element);
  };
  
  Tooltip.prototype.show = function(annotation, x, y) {
    element.find('.username').html(annotation.last_edit.username);
    element.find('.ago').html($.timeago(new Date(annotation.last_edit.timestamp)));
    element.css({ left: x, top: y });
    element.show();
  }
  
  Tooltip.prototype.hide = function() {
    element.hide();
  }
  
  return Tooltip;
  
});
