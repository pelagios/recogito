/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * The application entry point.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @param {Element} mapDiv the DIV to hold the Leaflet map
 * @param {String} dataURL the URL from where to retrieve the JSON data
 * @constructor
 */
pelagios.georesolution.CorrectionTool = function(tableDiv, mapDiv, footerDiv, dataURL) {  
  var self = this,
      map = new pelagios.georesolution.MapView(mapDiv),
      table = new pelagios.georesolution.TableView(tableDiv),
      footer = new pelagios.georesolution.Footer(footerDiv);
  
  // Set up inter-component eventing
  map.on('select', function(place) { table.selectByPlaceURI(place.place.uri); });
  
  table.on('selectionChanged', function(args, place) { 
    var prev2 = table.getPrevN(args.rows[0], 2);
    var next2 = table.getNextN(args.rows[0], 2);
    map.highlightPlace(place, prev2, next2); 
  });
  
  table.on('update', function(annotation) {
  	self._storeEdit(annotation);
    map.clear();
    if (self.annotations)
      $.each(self.annotations, function(idx, place) { map.addPlaceMarker(place) })
  });

  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    // Flatten & repackage response
    var annotations = [];
    var runningIdx = 0;
    $.each(data.parts, function(idx, part) {
      $.each(part.annotations, function(idx, annotation) {
        annotation.idx = runningIdx;
        annotation.source = part.source;
        annotation.part = part.title;
        annotations.push(annotation);
        runningIdx++;
      });
    });
    
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

// Stores edits to the database
pelagios.georesolution.CorrectionTool.prototype._storeEdit = function(annotation) {
  var payload = {
    'id': annotation.id,
    'toponym': annotation.toponym,
    'status': annotation.status,
  };
  
  if (annotation.place_fixed)
    payload.fix = annotation.place_fixed.uri;
  
  $.ajax({
    type: 'PUT',
    url: 'annotations/' + annotation.id,
    contentType: 'application/json',
    data: JSON.stringify(payload) 
  });
};

/**
 * A simple base class that takes care of event subcription.
 * @contructor
 */
pelagios.georesolution.HasEvents = function() { 
  this.handlers = {}
}

/**
 * Adds an event handler to this component. Refer to the docs of the components
 * for information about supported events.
 * @param {String} event the event name
 * @param {Function} handler the handler function
 */
pelagios.georesolution.HasEvents.prototype.on = function(event, handler) {  
  this.handlers[event] = handler;
}

/**
 * Fires an event.
 * @param {String} event the event name
 * @param {Object} e the event object
 * @param {Object} args the event arguments
 */
pelagios.georesolution.HasEvents.prototype.fireEvent = function(event, e, args) {
  if (this.handlers[event])
    this.handlers[event](e, args);     
}

/**
 * Helpers and utility methods.
 */
pelagios.georesolution.Utils = {
  
  /** Normalizes Pleiades URIs by stripping the trailing '#this' (if any) **/
  normalizePleiadesURI: function(uri) {
    if (uri.indexOf('#this') < 0) {
      return uri;
    } else {
      return uri.substring(0, uri.indexOf('#this'));
    }
  }
  
}
