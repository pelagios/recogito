/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

/**
 * Text annotation interface logic.
 * @param {Element} textDiv the DIV holding the annotatable text
 * @param {Number} gdocId the DB id of the document the text belongs to
 * @param {Number} opt_gdocPartId the DB id of the document part the text belongs to (if any)
 */
recogito.TextAnnotationUI = function(textDiv, textId, gdocId, opt_gdocPartId, opt_source) { 
  var self = this,
      getId = function(node) { return $(node).data('id'); };
   
  this._EDITOR_TEMPLATE = 
    '<div class="annotation-dialog annotation-editor">' + 
    '  <div class="header"></div>' +
    '  <div class="body">' +
    '    <span class="selection"></span>' +
    '    <span class="message"></span>' +
    '    <div class="buttons">' +
    '      <button class="button blue button-ok">OK</button>' +
    '      <button class="button dark button-cancel">Cancel</button>' +
    '    </div>' +
    '  </div>' +
    '<div>';
    
  this._PLAUSIBILITY_WARNING_TEMPLATE =
    '<div class="annotation-dialog">' +
    '  <div class="header">WARNING</div>' +
    '  <div class="body">' +
    '    <span class="message"></span>' +
    '    <div class="buttons">' +
    '      <button class="button blue button-ok">OK</button>' +
    '      <button class="button grey button-cancel">Cancel</button>' +
    '    </div>' +
    '  </div>' +
    '<div>';

  this._nodeType = 'span'; // So we can quickly replace with 'a' should it be needed
  this._gdocId = gdocId;
  this._gdocPartId = opt_gdocPartId;  
  this._source = opt_source;
  this._editor; // To keep track of the current open editor & prevent opening of multipe editors
  this._powerUserMode = false;
  
  // Toolbar
  var toolNormalAnnotation = $('.normal-annotation'),
      toolRocketAnnotation = $('.rocket-annotation'),
      toolSignOff = $('.signoff');
      
  toolNormalAnnotation.click(function(e) {
    self._powerUserMode = false;
    toolNormalAnnotation.addClass('selected');
    toolRocketAnnotation.removeClass('selected');
  });
  
  toolRocketAnnotation.click(function(e) {
    self._powerUserMode = true;  
    toolNormalAnnotation.removeClass('selected');
    toolRocketAnnotation.addClass('selected');
  });
  
  toolSignOff.click(function(e) {
    recogito.TextAnnotationUI.REST.signOff(textId, function(result) {
      if (result.success) {
        var icon = toolSignOff.find('.icon');
        var counter = toolSignOff.find('.signoff-count');
        var count = parseInt(counter.text());
        
        if (result.signed_off) {
          icon.addClass('signed');
          icon.attr('title', 'You signed off this text');
          counter.addClass('signed');
          counter.html(count + 1);
          counter.attr('title', (count + 1) + ' people have signed off this text');
        } else {
          icon.removeClass('signed');
          icon.attr('title', 'Do you think this text is complete? Click to sign it off!');
          if (count < 2)
            counter.removeClass('signed');
          counter.html(count - 1);
          counter.attr('title', (count - 1) + ' people have signed off this text');          
        }
      }
    });
  });
  
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
      var nodes = selectedRange.getNodes([1], function(e) { return e.nodeName.toLowerCase() == self._nodeType })
      if (nodes.length == 0) {
        // No span boundaries crossed
        var parent = $(selectedRange.getNodes([3])).parent().filter(self._nodeType);
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
  
  // If we have a specific annotation ID in the URL hash, the annotation should be highlighted
  $(document).ready(function() {
    var hashFragment, node;
    
    if (window.location.hash) {
      hashFragment = window.location.hash.substring(1);
      node = $(self._nodeType + '[data-id="' + hashFragment + '"]');
      if (node.length > 0) {
        node.addClass('highlighted');
        $('html, body').animate({ scrollTop: $(node).offset().top - ($(window).height() - $(node).outerHeight(true)) / 2 }, 200);
      }
    }
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
recogito.TextAnnotationUI.prototype.createAnnotation = function(msgTitle, msgDetails, toponym, x, y, offset, selectedRange, skipBatchAnnotation) {  
  var self = this;
      	
  // If silentMode == true, the popup dialog won't open (used for batch annotation)
  var create = function(skipBatch) {        
    // Create markup
    var anchor = document.createElement(self._nodeType);
    anchor.className = 'annotation corrected';
    anchor.appendChild(document.createTextNode(selectedRange.toString()));
    selectedRange.deleteContents();
    selectedRange.insertNode(anchor);

    // Store on server
    recogito.TextAnnotationUI.REST.createAnnotation(toponym, anchor, offset, self._gdocId, self._gdocPartId, self._source);
    
    // Check for other occurrences of the same toponym in the text and batch-annotate
    if (!skipBatch)
      self.batchAnnotate(toponym); 
  };

  if (msgTitle && msgDetails && !this._powerUserMode) {
	this.ifPlausible(toponym, x, y, function() { self.openEditor(msgTitle, toponym, msgDetails, x, y, create); });
  } else {
    this.ifPlausible(toponym, x, y, function() { create(skipBatchAnnotation); });
  }
}

/**
 * Updates an existing annotation.
 */
recogito.TextAnnotationUI.prototype.updateAnnotation = function(msgTitle, msgDetails, toponym, x, y, offset, selectedRange, selection, annotationId) {
  var self = this;
  
  var update = function() {
    // Store on server
    recogito.TextAnnotationUI.REST.updateAnnotation(annotationId, toponym, offset, self._gdocId, self._gdocPartId, self._source);
    
    // We'll need to replace all nodes in the markup that are fully or partially in the range
    var nodes = selectedRange.getNodes();
    
    // If the selected text ends in an annotation, well' remove that as well
    if (nodes.length > 1 && nodes[nodes.length-1].parentNode.nodeName.toLowerCase()  == self._nodeType)
      nodes.push(nodes[nodes.length-1].parentNode);

    // The plaintext within all nodes in the range   
    var text = ''; 
    $.each(nodes, function(idx, node) {
      if (node.nodeType == 3) // Text node
        text += $(node).text();
    });

    // Head - all text up to the selected toponym
    var head = text.substr(0, selectedRange.startOffset);
    
    // Tail - everything after the toponym
    var tail = text.substr(selectedRange.startOffset + toponym.length);

    // Get the correct anchor node to replace
    var nodeToReplace;
    if (nodes[0].parentNode.nodeName.toLowerCase() == self._nodeType) {
      nodeToReplace = nodes[0].parentNode;
      $.each(nodes, function(idx, node) { $(node).remove(); });
    } else { 
      nodeToReplace = nodes[0];
      $.each(nodes, function(idx, node) { 
        if (idx > 0)
          $(node).remove(); 
      });
    }
    
    $(nodeToReplace).replaceWith(head + '<' + self._nodeType + ' class="annotation corrected">' + toponym + '</' + self._nodeType + '>' + tail);
    selection.removeAllRanges();
  };
  
  if (this._powerUserMode)
    this.ifPlausible(toponym, x, y, function() { update(); });
  else 
    this.ifPlausible(toponym, x, y, function() { self.openEditor(msgTitle, toponym, msgDetails, x, y, update); });  
}

/**
 * Deletes an existing annotation.
 */
recogito.TextAnnotationUI.prototype.deleteAnnotation = function(msgTitle, msgDetails, toponym, x, y, annotationId, domNode) {
  var self = this;
  
  var del = function() {
    // Store on server
    recogito.TextAnnotationUI.REST.deleteAnnotation(annotationId);
      
    // Remove markup
    var text = domNode.text();
    domNode.replaceWith(text);
  };
  
  if (this._powerUserMode)
    del();
  else
    this.openEditor(msgTitle, toponym, msgDetails, x, y, del);  
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
    if (node.nodeType == 3) {
      var line = $(node).text();
      var startIdx = line.indexOf(' ' + toponym);
      if (startIdx > -1) {
        if (startIdx + toponym.length == line.length) {
          // Toponym at end of line -> Ok
          return true;
        } else {
          var nextChar = line.substr(startIdx + toponym.length + 1, 1);
          return ([' ', '.', ',', ';'].indexOf(nextChar) > -1)
        }
      }
    }
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
        self.createAnnotation(false, false, toponym, 0, 0, ranges.offset, ranges.selectedRange, true); 
      });
    }
  }
}


recogito.TextAnnotationUI.prototype.ifPlausible = function(toponym, x, y, ok_callback) {
  var error = false;
  
  if (toponym.length < 3)
    error = 'Your selection is very short. ';
  
  var words = toponym.split(' '); 
  if (words.length > 5)
    error = 'Your selection is very long. ';
  
  if (!error) {
    ok_callback();
  } else {
	// This selection was potentially made by mistake - we require extra confirmation
	var dialog = $(this._PLAUSIBILITY_WARNING_TEMPLATE);

    dialog.find('.message').html(error + 'Click OK if you really want to do this.');
    dialog.appendTo(document.body);
    dialog.css({ position: 'absolute', top: y + 'px', left: x + 'px' });
    dialog.find('.button-ok').click(function() { ok_callback(); dialog.remove(); });
    dialog.find('.button-cancel').focus().click(function() { dialog.remove(); });
    dialog.draggable({ handle: dialog.find('.header') });
  }
}

/**
 * Opens the editor, unless it is already open.
 */
recogito.TextAnnotationUI.prototype.openEditor = function(title, selection, msg, x, y, ok_callback) {  
  if (!this._editor) {
    this._editor = $(this._EDITOR_TEMPLATE);
  
    var self = this,
        header = this._editor.find('.header');
      
    header.html(title);
    this._editor.find('.selection').html(selection);
    this._editor.find('.message').html(msg);
    this._editor.appendTo(document.body);
    this._editor.css({ position: 'absolute', top: y + 'px', left: x + 'px' });
    this._editor.find('.button-ok').focus().click(function() { ok_callback(); self.closeEditor(); });
    this._editor.find('.button-cancel').click(function() { self.closeEditor(); });
    this._editor.draggable({ handle: header });
    
    var right = this._editor.width() + x,
        maxRight = jQuery(document.body).width();

    if (right > maxRight) {
      this._editor.css('left', (maxRight - this._editor.width() - 20) + 'px');
    }
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

recogito.TextAnnotationUI.REST.createAnnotation = function(toponym, span, offset, gdocId, gdocPartId, source) {
  var data = (gdocPartId) ? 
    '{ "gdoc_part_id": ' + gdocPartId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }' :
    '{ "gdoc_id": ' + gdocId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }';
    
  if (source) {
    var json = JSON.parse(data);
    json.source = source;
    data = JSON.stringify(json);
  }  
  
  $.ajax({
    url: '../api/annotations',
    type: 'POST',
    data: data,
    contentType : 'application/json',
    success: function(result) {
      span.setAttribute('data-id', result.id);
    },
    error: function(result) {
      alert('Could not store annotation: ' + result.responseJSON.message);
    }
  });
}

recogito.TextAnnotationUI.REST.updateAnnotation = function(id, toponym, offset, gdocId, gdocPartId, source) { 
  var data = (gdocPartId) ? 
    '{ "gdoc_part_d": ' + gdocPartId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }' :
    '{ "gdoc_id": ' + gdocId + ', "corrected_toponym": "' + toponym + '", "corrected_offset": ' + offset + ' }';

  if (source)
    data.source = source;
    
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

recogito.TextAnnotationUI.REST.signOff = function(textId, success_callback) {
  $.ajax({
    url: '../api/documents/signoff?textId=' + textId,
    type: 'POST',
    success: success_callback,
    error: function(result) {
      console.log('ERROR signing off text!');
    }
  });      
}
