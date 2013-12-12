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
  var self = this,
      getId = function(span) { return parseInt($(span).data('id')); };
   
  this._EDITOR_TEMPLATE = 
    '<div class="annotation-editor">' + 
    '  <div class="annotation-editor-header"></div>' +
    '  <div class="annotation-editor-selection"></div>' +
    '  <button class="annotation-editor-button button-ok">OK</button>' +
    '  <button class="annotation-editor-button button-cancel">Cancel</button>' +
    '<div>';
    
  this._editor;
    
  rangy.init();
 
  var handleSelection = function(e) {  
    var x = e.offsetX,
        y = e.offsetY,
        selection = rangy.getSelection();
        
    if (!selection.isCollapsed && selection.rangeCount == 1) {
      var selectedRange = selection.getRangeAt(0);
         
      var offsetRange = rangy.createRange();
      offsetRange.setStart(textDiv, 0);
      offsetRange.setEnd(selectedRange.startContainer, selectedRange.startOffset);
 
      // The selected text     
      var toponym = selectedRange.toString();
      
      // The character offset in the source text
      var offset = offsetRange.toString().length;
      
      // The <span>s crossed by the selection 
      var spans = selectedRange.getNodes([1], function(e) { return e.nodeName.toLowerCase() == 'span' })
      if (spans.length == 0) {
        // No span boundaries crossed
        var parent = $(selectedRange.getNodes([3])).parent().filter('span');
        if (parent.length > 0) {
          // Selection inside a <span> - size-reduction of existing annotation
          var id = getId(parent[0]);
          
          if ($(parent).text() == toponym) {
            // Selection identical with existing annotation - ask if delete?
            console.log('DELETE ANNOTATION ' + getId(parent) + '?');
          } else {
            // console.log('RESIZE OF ANNOTATION ' + id);
            self.openEditor("MODIFY ANNOTATION", x, y);
          }
        } else {
          // A new annotation
          // console.log('NEW ANNOTATION');
          self.openEditor("CREATE NEW ANNOTATION", x, y);
        }        
      } else if (spans.length == 1) {
        // One span crossed - resize/reanchoring of an existing annotation
        var id = getId(spans[0]);
        // console.log('MODIFICATION OF ANNOTATION ' + id);
        self.openEditor("MODIFY ANNOTATION", x, y);
      } else {
        // More than one span crossed - merge
        var ids = $.map(spans, function(span) { return getId(span); });
        // console.log('MERGING ' + ids);
        self.openEditor("MERGE ANNOTATIONS", x, y);
      }
      
      // TODO confirmation dialog
      
      /* TODO use CSS class rather than hard-coded color
      var highlight = document.createElement('span');
      highlight.style.backgroundColor = '#aaa';
      highlight.appendChild(document.createTextNode(toponym));
      
      selectedRange.deleteContents();
      selectedRange.insertNode(highlight);
      */
      
      console.log('Toponym: ' + toponym + ' - ' + offset); 
    }
  };
    
  $(textDiv).mouseup(function(e) {
    window.setTimeout(function() { handleSelection(e) }, 1);    
  });
}

pelagios.georesolution.FulltextAnnotationView.prototype.openEditor = function(msg, x, y) {  
  this._editor = $(this._EDITOR_TEMPLATE);
  
  var self = this,
      e = $(this._editor);
  e.find('.annotation-editor-header').html(msg);
  e.appendTo(document.body);
  e.css('top', y + 'px');
  e.css('left', x + 'px');  
  e.find('.button-ok').click(function() { self.closeEditor(); });
  e.find('.button-cancel').click(function() { self.closeEditor(); });
}

pelagios.georesolution.FulltextAnnotationView.prototype.closeEditor = function() { 
  if (this._editor) {
    $(this._editor).remove();
    delete this._editor;
  }
}



