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
        '<div class="batch"></div>';
        
  // Details Popup DOM element
  this.element = $(template);
  this.element.appendTo(document.body);
  
  console.log(annotations);
}

// Inheritance - not the nicest pattern but works for our case
recogito.BatchPopup.prototype = new recogito.HasEvents();

/**
 * Destroys the popup.
 */
recogito.BatchPopup.prototype.destroy = function() {
  $(this.element).remove();
}

