/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

/**
 * The application entry point.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @param {Element} mapDiv the DIV to hold the Leaflet map
 * @param {String} dataURL the URL from where to retrieve the JSON data
 * @constructor
 */
recogito.MapCorrectionUI = function(tableDiv, mapDiv, footerDiv, dataURL) {  
  var self = this,
      map = new recogito.MapView(mapDiv),
      table = new recogito.TableView(tableDiv),
      footer = new recogito.Footer(footerDiv);
  
  // Set up inter-component eventing
  map.on('select', function(annotation) { 
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
    table.selectByPlaceURI(place.uri); 
  });
  
  table.on('selectionChanged', function(args, annotation) { 
    var prev3 = table.getPrevN(args.rows[0], 3);
    var next2 = table.getNextN(args.rows[0], 2);
    map.selectPlace(annotation, prev3, next2); 
  });
  
  table.on('update', function(annotation) {
  	self._storeToDB(annotation);
    
  	if (annotation.marker)
  	  map.removePlaceMarker(annotation);
      
    map.addPlaceMarker(annotation);
  	
    if (self.annotations)
      footer.setData(self.annotations);
  });
  
  table.on('mouseover', function(annotation) { map.emphasizePlace(annotation); });
  table.on('mouseout', function(annotation) { map.deemphasizePlace(annotation); });

  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    // Flatten & repackage response
    var annotations = [];
    var runningIdx = 0;
        
    if (data.annotations) {
      $.each(data.annotations, function(idx, annotation) {
        annotation.idx = runningIdx;
        annotation.source = data.source;
        annotation.part = "";
        annotations.push(annotation);
        runningIdx++;
      });
    } else if (data.parts) {
      $.each(data.parts, function(idx, part) {
        $.each(part.annotations, function(idx, annotation) {
          annotation.idx = runningIdx;
          annotation.source = part.source;
          annotation.part = part.title;
          annotations.push(annotation);
          runningIdx++;
        });
      });
    }
    
    // Set data on table
    table.setData(annotations, true);
    table.render();
    
    // Set data on map
    $.each(annotations, function(idx, annotation) { map.addPlaceMarker(annotation) });
    
    // Set data on Footer
    footer.setData(annotations);
    
    self.annotations = annotations;
  });
}

/**
 * Stores an updated annotation to the database.
 * @private
 */
recogito.MapCorrectionUI.prototype._storeToDB = function(annotation) {
  var payload = {
    'id': annotation.id,
    'status': annotation.status
  };
  
  if (annotation.tags)
    payload.tags = annotation.tags.join(",");
  
  if (annotation.place_fixed)
    payload.corrected_uri = annotation.place_fixed.uri;
    
  if (annotation.comment)
    payload.comment = annotation.comment;
  
  $.ajax({
    type: 'PUT',
    url: 'api/annotations/' + annotation.id,
    contentType: 'application/json',
    data: JSON.stringify(payload) 
  });
};

/**
 * A footer element for the UI that displays basic document metadata.
 * @param {Element} footerDiv the DIV to hold the footer
 */
recogito.Footer = function(footerDiv) {
  this.element = $(footerDiv).find('#footer-info');
}

/**
 * Sets the document metadata on the footer.
 * @param {Object} the annotation data
 */
recogito.Footer.prototype.setData = function(data) {
  var count = function(status) {
    var list = $.grep(data, function(annotation, idx) { return annotation.status == status; });
    return list.length;
  }
  
  var total = data.length;
  var verified = count('VERIFIED');
  var not_identifyable = count('NOT_IDENTIFYABLE');
  var false_detection = count('FALSE_DETECTION');
  var ignore = count('IGNORE');
  var complete = verified / (total - not_identifyable - false_detection - ignore);
  
  $(this.element).html(
    data.length + ' Annotations &nbsp; ' + 
    '<span class="icon">&#xf14a;</span> ' + verified + ' &nbsp; ' + 
    '<span class="icon">&#xf024;</span> ' + not_identifyable + ' &nbsp; ' + 
    '<span class="icon">&#xf057;</span> ' + false_detection + ' &nbsp;' +
    '<span class="icon">&#xf05e;</span> ' + ignore + ' &nbsp; - &nbsp; ' + 

    (complete * 100).toFixed(1) + '% Complete');
}

/**
 * A simple base class that takes care of event subcription.
 * @contructor
 */
recogito.HasEvents = function() { 
  this.handlers = {}
}

/**
 * Adds an event handler to this component. Refer to the docs of the components
 * for information about supported events.
 * @param {String} event the event name
 * @param {Function} handler the handler function
 */
recogito.HasEvents.prototype.on = function(event, handler) {  
  this.handlers[event] = handler;
}

/**
 * Fires an event.
 * @param {String} event the event name
 * @param {Object} e the event object
 * @param {Object} args the event arguments
 */
recogito.HasEvents.prototype.fireEvent = function(event, e, args) {
  if (this.handlers[event])
    this.handlers[event](e, args);     
}

/**
 * A tag editor component to be used in the Details & Batch popups.
 * @param {Element} parent the DOM element to attach the editor to 
 * @param {Number} offsetTop the top offset for placing the editor
 * @param {Number} offsetLeft the left offset for placing the editor
 * @param {Function} onEnter a callback for catching the 'ENTER' keypress
 * @param {Function} onEscape a callback for catching the 'Escape' keypress
 */
recogito.TagEditor = function(parent, offsetTop, offsetLeft, onEnter, onEscape) {
  var self = this;
  
  this.element = $('<div class="popup-tag-editor"><input type="text" placeholder="Tags, separated by comma..."></input></div>');
  this.element.css('top', (offsetTop +  3) + 'px');
  this.element.css('left', offsetLeft + 'px');
  this.element.appendTo(parent); 
  this.element.click(function(e) { e.stopPropagation(); });
  
  var textField = this.element.find('input')[0];    
  textField.focus()
  this.element.keydown(function(e) {
    if (e.keyCode == 13) {
      // Enter
      if (onEnter)
        onEnter(textField.value.split(","));
        
      self.destroy();
    } else if (e.keyCode == 27) {
      // Escape
      if (onEscape)
        onEscape(self);
        
      self.destroy();
    }    
  });
}

recogito.TagEditor.prototype.destroy = function() {
  $(this.element).remove();
}

/**
 * Helpers and utility methods.
 */
recogito.Utils = {
  
  /** Normalizes Pleiades URIs by stripping the trailing '#this' (if any) **/
  normalizePleiadesURI: function(uri) {
    if (uri.indexOf('#this') < 0) {
      return uri;
    } else {
      return uri.substring(0, uri.indexOf('#this'));
    }
  },
  
  formatCategory: function(category, opt_template) {
    if (!category)
      return '';
      
    var screenName;
    
    if (category == 'SETTLEMENT')
      screenName = 'Settlement';
    else if (category == 'REGION')
      screenName = 'Region';
    else if (category == 'NATURAL_FEATURE')
      screenName = 'Natural Feature'
    else if (category == 'ETHNOS')
      screenName = 'Ethnos';
    else if (category == 'MAN_MADE_STRUCTURE')
      screenName = 'Built Structure';
    else
      screenName = category;
      
    if (opt_template)
      return opt_template.replace('{{category}}', screenName);
    else
      return screenName
  }
  
}
