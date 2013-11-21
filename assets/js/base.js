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
pelagios.georesolution.CorrectionTool = function(tableDiv, mapDiv, dataURL) {  
  var self = this,
      map = new pelagios.georesolution.MapView(mapDiv),
      table = new pelagios.georesolution.TableView(tableDiv);
  
  // Set up inter-component eventing
  map.on('select', function(place) { table.selectByPlaceURI(place.place.uri); });
  
  table.on('selectionChanged', function(args, place) { 
    var prev2 = table.getPrevN(args.rows[0], 2);
    var next2 = table.getNextN(args.rows[0], 2);
    map.highlightPlace(place, prev2, next2); 
  });
  
  table.on('update', function() {
    map.clear();
    if (self.places)
      $.each(self.places, function(idx, place) { map.addPlaceMarker(place) })
  });

  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    // Flatten & repackage response
    var places = [];
    var runningIdx = 0;
    $.each(data.parts, function(idx, part) {
      $.each(part.places, function(idx, place) {
        place.idx = runningIdx;
        place.source = part.source;
        place.worksheet = part.title;
        places.push(place);
        runningIdx++;
      });
    });
    
    // Set data on table
    table.setData(places, true);
    table.render();
    
    // Set data on map
    $.each(places, function(idx, place) { map.addPlaceMarker(place) });
    
    self.places = places;
  });
}

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
