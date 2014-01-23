/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

/**
 * A popup containing 'batch processing' features that affect a list of annotations 
 * as a whole.
 * 
 * Emits the following events:
 * 'update' .............. when a correction is saved 
 * 
 * @param {Object} annotations the annotations
 */
recogito.BatchPopup = function(annotations) {
  // Inheritance - not the nicest pattern but works for our case
  recogito.HasEvents.call(this);
  
  var self = this,
      template =
        '<div class="clicktrap">' +
        '  <div class="popup">' +
        '    <div class="popup-header">' +
        '     <span id="batch-header-title"></span>' +
        '      <a class="popup-exit">&#xf00d;</a>' +
        '    </div>' +
        '    <div class="popup-content">' +
        '      <div class="popup-content-inner">' +
        '        <p><strong>Unique toponyms:</strong> <span id="batch-unique-toponyms"></span></p>' + 
        '        <p><strong>Unique gazetteer IDs:</strong> <span id="batch-unique-uris"></span></p>' + 
        '        <p><strong>Unique tags:</strong> <span id="batch-unique-tags"></span></p>' +         
        '      </div>' +
        '    </div>' +
        '  </div>' +
        '</div>';
        
  // Details Popup DOM element
  this.element = $(template);
  this.element.appendTo(document.body);
  $('.popup-exit').click(function() { self.destroy(); });
  
  // Populate the template
  $('#batch-header-title').html(annotations.length + ' Annotations Selected');
  $('#batch-unique-toponyms').html(recogito.BatchPopup.uniqueToponyms(annotations).join(', '));
  $('#batch-unique-uris').html(recogito.BatchPopup.uniqueGazetteerURIs(annotations).join(', '));
  $('#batch-unique-tags').html(recogito.BatchPopup.uniqueTags(annotations).join(', '));
}

// Inheritance - not the nicest pattern but works for our case
recogito.BatchPopup.prototype = new recogito.HasEvents();

/**
 * Destroys the popup.
 */
recogito.BatchPopup.prototype.destroy = function() {
  $(this.element).remove();
}

/** Helper functions **/

/**
 * Returns the list of unique toponyms that occur in the annotations.
 * @param {Array.<Object>} annotations the annotations
 */
recogito.BatchPopup.uniqueToponyms = function(annotations) {
  var unique = [];
  $.each(annotations, function(idx, annotation) {
    if (unique.indexOf(annotation.toponym) == -1)
      unique.push(annotation.toponym);
  }); 
  return unique;
}

/**
 * Returns the list of unique gazetteer URIs that occur in the annotations.
 * @param {Array.<Object>} annotations the annotations
 */
recogito.BatchPopup.uniqueGazetteerURIs = function(annotations) {
  var unique = [];
  $.each(annotations, function(idx, annotation) {
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
    if (place)
      if (unique.indexOf(place.uri) == -1)
        unique.push(place.uri);
  }); 
  return unique;
}

/**
 * Returns the list of unique tags that occur in the annotations.
 * @param {Array.<Object>} annotations the annotations
 */
recogito.BatchPopup.uniqueTags = function(annotations) {
  var unique = [];
  $.each(annotations, function(idx, annotation) {
    if (annotation.tags) {
      $.each(annotation.tags, function(idx, tag) {
        if (unique.indexOf(tag) == -1)
          unique.push(tag);
      });
    }
  });
  return unique;
}


