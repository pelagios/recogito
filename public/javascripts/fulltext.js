/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * Fulltext annotation view.
 * 
 * Emits the following events:
 * 
 * TODO
 * 
 * @param {Element} mapDiv the DIV holding the annotated fulltext
 * @constructor
 */
pelagios.georesolution.FulltextAnnotationView = function(textDiv) {
  rangy.init();
    
  $(textDiv).mouseup(function() {
    var selection = rangy.getSelection();
    if (!selection.isCollapsed && selection.rangeCount == 1) {
      var selectedRange = selection.getRangeAt(0);
         
      var offsetRange = rangy.createRange();
      offsetRange.setStart(textDiv, 0);
      offsetRange.setEnd(selectedRange.startContainer, selectedRange.startOffset);
      
      var toponym = selectedRange.toString();
      var offset = offsetRange.toString().length;   
      
      console.log('Toponym: ' + toponym + ' - ' + offset); 
    }
  });
}
