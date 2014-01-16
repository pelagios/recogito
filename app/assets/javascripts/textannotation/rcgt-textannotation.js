/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

/**
 * Fulltext annotation view.
 * @param {Element} mapDiv the DIV holding the annotated fulltext
 */
recogito.TextAnnotationUI = function(textDiv, gdocId, gdocPartId) { 
  var self = this,
      getId = function(node) { return parseInt($(node).data('id')); };
   
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
  
  this._gdocId = gdocId;
  this._gdocPartId = gdocPartId;  
  this._editor; // To keep track of the current open editor & prevent opening of multipe editors
  
  rangy.init();
 
  // Main event handler
  var handleSelection = function(e) {  
    var x = (e.offsetX) ? e.offsetX : e.pageX,
        y = (e.offsetY) ? e.offsetY : e.pageY,
        selection = rangy.getSelection();
        
    if (!selection.isCollapsed && selection.rangeCount == 1) {
      var normalizedSelection = self.normalizeSelection(textDiv, selection.getRangeAt(0));

      var toponym = normalizedSelection.toponym; // Selected text          
      var offset = normalizedSelection.offset; // Offset, measured from text start
      var selectedRange = normalizedSelection.selectedRange; // Normalized selected range
      
      // Determine if the selection crosses existing annotations 
      var nodes = selectedRange.getNodes([1], function(e) { return e.nodeName.toLowerCase() == 'a' })
      if (nodes.length == 0) {
        // No span boundaries crossed
        var parent = $(selectedRange.getNodes([3])).parent().filter('a');
        if (parent.length > 0) {
          var id = getId(parent[0]);
          
          if ($(parent).text() == toponym) {
            // Selection identical with existing annotation - ask if delete?
            self.deleteAnnotation(
              'DELETE ANNOTATION', 
              'Already marked as a toponym. Do you want to delete annotation instead?',
              toponym, x, y,
              getId(parent),
              parent);
          } else {            
            self.updateAnnotation(
              'MODIFY ANNOTATION',
              'Update toponym?',
              toponym, x, y,
              offset,
              selectedRange, selection,
              getId(parent));
          }
        } else {
          // A new annotation          
          self.createAnnotation(
            'NEW ANNOTATION', 
            'Mark as toponym?',
            toponym, x, y,
            offset,
            selectedRange);
        }        
      } else if (nodes.length == 1) {
        // One span crossed - resize/reanchoring of an existing annotation
        var id = getId(nodes[0]);
        self.updateAnnotation(
          'MODIFY ANNOTATION',
          'Update toponym?',
          toponym, x, y,
          offset,
          selectedRange, selection,
          id);
      } else {
        // More than one span crossed - merge
        var ids = $.map(nodes, function(node) { return getId(node); });        
        self.updateAnnotation(
          'MERGE ANNOTATIONS',
          'Merge to one toponym?',
          toponym, x, y,
          offset,
          selectedRange, selection,
          ids[0]);
      }
    }
  };
    
  $(textDiv).mouseup(function(e) {
    window.setTimeout(function() { handleSelection(e) }, 1);    
  });
}

/**
 * Normalizes a selection range and computes absolute offset.
 * @param {Element} textDiv the text container DIV
 * @param {Object} selectedRange the selection range
 */
recogito.TextAnnotationUI.prototype.normalizeSelection = function(textDiv, selectedRange) {
  // The toponym (= annotation text)
  var toponym = selectedRange.toString();
  
  // A range from the start of the text to the start of the selection (= offset!)
  var offsetRange = rangy.createRange();
  offsetRange.setStart(textDiv, 0);
  offsetRange.setEnd(selectedRange.startContainer, selectedRange.startOffset);
      
  // Normalize selected and offset range by removing leading & trailing spaces
  if (toponym.indexOf(" ") == 0) {
    selectedRange.setStart(selectedRange.startContainer, selectedRange.startOffset + 1);
    offsetRange.setEnd(selectedRange.startContainer, selectedRange.startOffset);
    toponym = selectedRange.toString();
  }
      
  if (toponym.indexOf(" ", toponym.length - 1) !== -1) {
    selectedRange.setEnd(selectedRange.endContainer, selectedRange.endOffset - 1);
    toponym = selectedRange.toString();
  }
  
  // Compute the character offset in the original plaintext source
  var newLines = offsetRange.getNodes([1], function(node) { return node.nodeName == 'BR'; });
  var offset = offsetRange.toString().length + newLines.length - 1;
  
  return { toponym: toponym, offset: offset, selectedRange: selectedRange };
}

/**
 * Creates a new annotation.
 */
recogito.TextAnnotationUI.prototype.createAnnotation = function(msgTitle, msgDetails, toponym, x, y, offset, selectedRange) {
  var self = this;
  
  // If silentMode == true, the popup dialog won't open (used for batch annotation)
  var create = function(silentMode) {
    // Store on server
    recogito.TextAnnotationUI.REST.createAnnotation(toponym, offset, self._gdocId, self._gdocPartId);
    
    // Create markup
    var anchor = document.createElement('a');
    anchor.className = 'annotation corrected';
    anchor.appendChild(document.createTextNode(selectedRange.toString()));
    selectedRange.deleteContents();
    selectedRange.insertNode(anchor);
    
    // Check for other occurrences of the same toponym in the text and batch-annotate
    if (!silentMode)
      self.batchAnnotate(toponym); 
  };
  
  if (msgTitle && msgDetails) {
    this.openEditor(msgTitle, toponym, msgDetails, x, y, create);
  } else {
    create(true);
  }
}

/**
 * Updates an existing annotation.
 */
recogito.TextAnnotationUI.prototype.updateAnnotation = function(msgTitle, msgDetails, toponym, x, y, offset, selectedRange, selection, annotationId) {
  // TODO the hoisted 'toponym' variable shadows the argument - clean up!
  var self = this,
      t = toponym,
      id = annotationId;
  
  this.openEditor(msgTitle, toponym, msgDetails, x, y, function() {
    // Store on server
    recogito.TextAnnotationUI.REST.updateAnnotation(id, t, offset, self._gdocId, self._gdocPartId);
    
    // We'll need to replace all nodes in the markup that are fully or partially in the range
    var nodes = selectedRange.getNodes();
    
    // If the selected text ends in an annotation, well' remove that as well
    if (nodes.length > 1 && nodes[nodes.length-1].parentNode.nodeName == 'A')
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
    if (nodes[0].parentNode.nodeName == 'A') {
      nodeToReplace = nodes[0].parentNode;
      $.each(nodes, function(idx, node) { $(node).remove(); });
    } else { 
      nodeToReplace = nodes[0];
      $.each(nodes, function(idx, node) { 
        if (idx > 0)
          $(node).remove(); 
      });
    }
    
    $(nodeToReplace).replaceWith(head + '<a class="annotation corrected">' + toponym + '</a>' + tail);
    selection.removeAllRanges();
  });  
}

/**
 * Deletes an existing annotation.
 */
recogito.TextAnnotationUI.prototype.deleteAnnotation = function(msgTitle, msgDetails, toponym, x, y, annotationId, domNode) {
  var self = this;
  this.openEditor(msgTitle, toponym, msgDetails, x, y, function() {
    // Store on server
    recogito.TextAnnotationUI.REST.deleteAnnotation(annotationId);
      
    // Remove markup
    var text = domNode.text();
    domNode.replaceWith(text);
  });  
}

/**
 * Productivity feature: queries for untagged occurences of the toponym and automatically annotates.
 * @param {String} toponym the toponym
 * @param {Number} offset
 */
recogito.TextAnnotationUI.prototype.batchAnnotate = function(toponym) {
  var self = this,
      textDiv = document.getElementById('text');
  
  // Loop through all nodes in the #text DIV...
  var untagged = $.grep($(textDiv).contents(), function(node) {
    // ...and count direct text children that contain the toponym
    if (node.nodeType == 3)
      if ($(node).text().indexOf(' ' + toponym + ' ') > -1)
        return true
  });
  
  if (untagged.length > 0) {
    var doBatch = confirm(
      'There are at least ' + untagged.length + ' ' +
      'more un-tagged occurrences of "' + toponym + '". ' +
      'Do you want me to tag them too?');
      
    if (doBatch) {
      $.each(untagged, function(idx, textNode) {
        var text = $(textNode).text();
        var selectedRange = rangy.createRange();
        selectedRange.setStart(textNode, text.indexOf(toponym));
        selectedRange.setEnd(textNode, text.indexOf(toponym) + toponym.length);

        var ranges = self.normalizeSelection(textDiv, selectedRange)        
        self.createAnnotation(false, false, toponym, 0, 0, ranges.offset, ranges.selectedRange); 
      });
    }
  }
}

/**
 * Opens the editor, unless it is already open.
 */
recogito.TextAnnotationUI.prototype.openEditor = function(title, selection, msg, x, y, ok_callback) {  
  if (!this._editor) {
    this._editor = $(this._EDITOR_TEMPLATE);
  
    var self = this,
        e = $(this._editor),
        header = e.find('.annotation-editor-header');
      
    header.html(title);
    e.find('.annotation-editor-selection').html(selection);
    e.find('.annotation-editor-message').html(msg);
    e.appendTo(document.body);
    e.css({ position: 'absolute', top: y + 'px', left: x + 'px' });
    e.find('.button-ok').focus().click(function() { ok_callback(); self.closeEditor(); });
    e.find('.button-cancel').click(function() { self.closeEditor(); });
    e.draggable({ handle: header });
  }
}

/**
 * Closes the editor.
 */
recogito.TextAnnotationUI.prototype.closeEditor = function() { 
  if (this._editor) {
    $(this._editor).remove();
    delete this._editor;
  }
}

/**
 * Helper function for accessing query string params.
 */
recogito.TextAnnotationUI.getQueryParam = function(key) {
  var re = new RegExp('(?:\\?|&)'+key+'=(.*?)(?=&|$)','gi');
  var r = [], m;
  while ((m = re.exec(document.location.search)) != null) r.push(m[1]);
  return r;
}

/** REST calls **/
recogito.TextAnnotationUI.REST = { }

recogito.TextAnnotationUI.REST.createAnnotation = function(toponym, offset, gdocId, gdocPartId) {
  var data = (gdocPartId) ? 
    '{ "gdocPartId": ' + gdocPartId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }' :
    '{ "gdocId": ' + gdocId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }';
    
  console.log(data);
  
  $.ajax({
    url: '../api/annotations',
    type: 'POST',
    data: data,
    contentType : 'application/json',
    error: function(result) {
      alert('Could not store annotation: ' + result.responseJSON.message);
    }
  });
}

recogito.TextAnnotationUI.REST.updateAnnotation = function(id, toponym, offset, gdocId, gdocPartId) { 
  var data = (gdocPartId) ? 
    '{ "gdocPartId": ' + gdocPartId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }' :
    '{ "gdocId": ' + gdocId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }';
    
  $.ajax({
    url: '../api/annotations/' + id,
    type: 'PUT',
    data: data,
    contentType : 'application/json',
    error: function(result) {
      console.log('ERROR updating annotation!');
    }
  });    
}

recogito.TextAnnotationUI.REST.deleteAnnotation = function(id, opt_callback) {
  $.ajax({
    url: '../api/annotations/' + id,
    type: 'DELETE',
    error: function(result) {
      console.log('ERROR deleting annotation!');
    }
  });    
}
