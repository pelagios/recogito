define([], function() {

  var HelpWindow = function() {
    var element =
          $('<div class="help-popup">' +
            '  <div class="header">HELP' +
            '    <span class="exit icon" title="Close">&#xf00d;</span>' +
            '  </div>' +
            '  <div class="body">' +
            '    <h2>Keyboard Shortcuts</h2>' +
            '    <table>' +
            '      <tr><td class="keys">SPACE</td><td>toggle between Navigation and Annotation mode</td></tr>' +
            '      <tr><td class="keys">ESCAPE</td><td>cancel while drawing an annotation</td></tr>' +
            '    </table>' +
            '    <h2>Map Navigation</h2>' +
            '    <table>' +
            '      <tr><td>Hold <span class="keys">SHIFT</span></td><td>to drag a box and zoom to the area</td></tr>' +
            '      <tr><td>Hold <span class="keys">SHIFT+Alt</span></td><td>to rotate the map around the center of the screen</td></tr>' +
            '    </table>' +
            '    <h2>Other</h2>' +
            '    <table>' +
            '      <tr><td class="keys">SHIFT+G</td><td>Toggle Grid (Experimental - Behaim Grid only)</td></tr>' +
            '    </table>' +
            '  </div>' +
            '</div>'),

        show = function() {
          element.find('.exit').click(hide);
          element.draggable({ handle: element.find('.header') });
          jQuery(document.body).append(element);
        },

        hide = function() {
          element.remove();
        },

        isVisible = function() {
          return element.closest('html').length > 0;
        };

    /** Privileged methods **/
    this.show = show;
    this.hide = hide;
    this.isVisible = isVisible;
  }

  return HelpWindow;

});
