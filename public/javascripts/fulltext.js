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
    '  <div class="annotation-editor-body">' +
    '    <span class="annotation-editor-selection"></span>' +
    '    <span class="annotation-editor-message"></span>' +
    '  </div>' +
    '  <div class="annotation-editor-buttons">' +
    '    <button class="annotation-editor-button button-ok">OK</button>' +
    '    <button class="annotation-editor-button button-cancel">Cancel</button>' +
    '  </div>' +
    '<div>';
    
  this._editor;
    
  rangy.init();
  
  var wrapToponym = function(selectedRange) { 
    var span = document.createElement('span');
    span.className = 'annotation corrected';
    span.appendChild(document.createTextNode(selectedRange.toString()));
    selectedRange.deleteContents();
    selectedRange.insertNode(span);
  };
  
  var unwrapToponym = function(span) {
    var text = span.text();
    span.replaceWith(text);
  };
  
  var rewrapToponym = function(selectedRange) {
    // We'll need to replace all nodes that are fully or partially in the range
    var nodes = selectedRange.getNodes();
    
    // If the selected text ends in an annotation, well' remove that as well
    if (nodes.length > 1 && nodes[nodes.length-1].parentNode.nodeName == 'SPAN')
      nodes.push(nodes[nodes.length-1].parentNode);

    // The plaintext within all nodes in the range   
    var text = ''; 
    $.each(nodes, function(idx, node) {
      if (node.nodeType == 3) // Text node
        text += $(node).text();
    });

    // Head - all text up to the selected toponym
    var head = text.substr(0, selectedRange.startOffset);
    
    // Toponym
    var toponym = selectedRange.toString();
    
    // Tail - everything after the toponym
    var tail = text.substr(selectedRange.startOffset + toponym.length);

    // Get the correct anchor node to replace
    var nodeToReplace;
    if (nodes[0].parentNode.nodeName == 'SPAN') {
      nodeToReplace = nodes[0].parentNode;
      $.each(nodes, function(idx, node) { $(node).remove(); });
    } else { 
      nodeToReplace = nodes[0];
      $.each(nodes, function(idx, node) { 
        if (idx > 0)
          $(node).remove(); 
      });
    }
    
    $(nodeToReplace).replaceWith(head + '<span class="annotation corrected">' + toponym + '</span>' + tail);
  }
  
  // API call - delete annotation
  var deleteAnnotation = function(id) {
    
  };
  
  // API call - create new annotation
  var createAnnotation = function(toponym, offset) {
    
  };
  
  // API call - update annotation
  var updateAnnotation = function(id, toponym, offset) {
  };
 
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
          var id = getId(parent[0]);
          
          if ($(parent).text() == toponym) {
            // Selection identical with existing annotation - ask if delete?
            self.openEditor("MODIFY ANNOTATION", toponym, "Already marked as a toponym. Do you want to delete annotation instead?", x, y,
              function() {
                deleteAnnotation(getId(parent));
                unwrapToponym(parent);
              }
            );
          } else {
            self.openEditor("MODIFY ANNOTATION", toponym, "Update toponym?", x, y, function() {
              updateAnnotation(getId(parent), toponym, offset);
              rewrapToponym(selectedRange);
              selection.removeAllRanges();
            });
          }
        } else {
          // A new annotation
          self.openEditor("NEW ANNOTATION", toponym, "Mark as toponym?", x, y, function() {
            createAnnotation(toponym, offset);
            wrapToponym(selectedRange);
          });
        }        
      } else if (spans.length == 1) {
        // One span crossed - resize/reanchoring of an existing annotation
        var id = getId(spans[0]);
        self.openEditor("MODIFY ANNOTATION", toponym, "Update toponym?", x, y, function() {
          updateAnnotation(id, toponym, offset);
          rewrapToponym(selectedRange);
          selection.removeAllRanges();
        });
      } else {
        // More than one span crossed - merge
        var ids = $.map(spans, function(span) { return getId(span); });
        self.openEditor("MERGE ANNOTATIONS", toponym, "Merge to one toponym?", x, y, function() {
          // TODO
          rewrapToponym(selectedRange);
          selection.removeAllRanges();
        });
      }
    }
  };
    
  $(textDiv).mouseup(function(e) {
    window.setTimeout(function() { handleSelection(e) }, 1);    
  });
}

pelagios.georesolution.FulltextAnnotationView.prototype.openEditor = function(title, selection, msg, x, y, ok_callback) {  
  this._editor = $(this._EDITOR_TEMPLATE);
  
  var self = this,
      e = $(this._editor);
  e.find('.annotation-editor-header').html(title);
  e.find('.annotation-editor-selection').html(selection);
  e.find('.annotation-editor-message').html(msg);
  e.appendTo(document.body);
  e.css('top', y + 'px');
  e.css('left', x + 'px');  
  e.find('.button-ok').focus().click(function() { ok_callback(); self.closeEditor(); });
  e.find('.button-cancel').click(function() { self.closeEditor(); });
}

pelagios.georesolution.FulltextAnnotationView.prototype.closeEditor = function() { 
  if (this._editor) {
    $(this._editor).remove();
    delete this._editor;
  }
}



