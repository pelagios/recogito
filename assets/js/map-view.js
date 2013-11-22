/**
 * The map component of the UI.
 * 
 * Emits the following events:
 * 'select' ... when a place was selected on a map
 * 
 * @param {Element} mapDiv the DIV to hold the Leaflet map
 * @constructor
 */
pelagios.georesolution.MapView = function(mapDiv) {
  // Inheritance - not the nicest pattern but works for our case
  pelagios.georesolution.HasEvents.call(this);
  
  var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
    attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
  });
        
  this._map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 5,
    layers: [baseLayer],
    minZoom: 3,
    maxZoom: 11
  });
  
  this._currentSequence = [];
  
  this._allMarkers = [];
}

// Inheritance - not the nicest pattern but works for our case
pelagios.georesolution.MapView.prototype = new pelagios.georesolution.HasEvents();

/**
 * Places a marker for the specified place.
 * @param {Object} annotation the place annotation
 */
pelagios.georesolution.MapView.prototype.addPlaceMarker = function(annotation) {
  var self = this,
      STYLE_AUTOMATCH_NO_FIX = { color: '#0000ff', radius: 5, fillOpacity: 0.8 },
      STYLE_AUTOMATCH_FIXED = { color: '#0000ff', radius: 5, fillOpacity: 0.3 },
      STYLE_CORRECTED = { color: '#ff0000', radius: 5, fillOpacity: 0.8 };

  if (annotation.place_fixed && annotation.place_fixed.coordinate) { 
    var markerFixed = L.circleMarker(annotation.place_fixed.coordinate, STYLE_CORRECTED);
    
    if (annotation.place && annotation.place.coordinate) {
      var connectingLine;
      
      var showConnection = function() {
        connectingLine = L.polyline([annotation.place_fixed.coordinate, annotation.place.coordinate], STYLE_CORRECTED);
        connectingLine.addTo(self._map);
      }; 
      
      var hideConnection = function() {
        self._map.removeLayer(connectingLine);
      };
      
      markerFixed.on('mouseover', function(e) { showConnection(); });
      markerFixed.on('mouseout', function(e) { hideConnection(); });
      
      var markerAutomatch = L.circleMarker(annotation.place.coordinate, STYLE_AUTOMATCH_FIXED);
      markerAutomatch.on('mouseover', function(e) { showConnection(); });
      markerAutomatch.on('mouseout', function(e) { hideConnection(); });
      markerAutomatch.addTo(this._map);
      
      self._allMarkers.push(markerAutomatch);
    } 
      
    self._allMarkers.push(markerFixed);
    markerFixed.addTo(this._map);
    annotation.marker = markerFixed;
  } else if (annotation.place && annotation.place.coordinate) {
    var marker = L.circleMarker(annotation.place.coordinate, STYLE_AUTOMATCH_NO_FIX);
    marker.addTo(this._map); 
    marker.on('click', function(e) {
      marker.bindPopup(annotation.toponym + ' (<a href="' + annotation.source + '" target="_blank">Source</a>)').openPopup(); 
      self.fireEvent('select', annotation);
    });
    self._allMarkers.push(marker);

    annotation.marker = marker    
  }
}

/**
 * Highlights the specified place on the map.
 * @param {Object} annotation the place annotation
 * @param {Array.<Object>} prevN the previous annotations in the list (if any)
 * @param {Array.<Object>} nextN the next annotations in the list (if any)
 */
pelagios.georesolution.MapView.prototype.highlightPlace = function(annotation, prevN, nextN) {
  var self = this;

  // Utility function to draw the sequence line
  var drawSequenceLine = function(coords, style) {
    var line = L.polyline(coords, style);
    self._currentSequence.push(line);
    line.addTo(self._map);
  }
  
  if (annotation.marker) {
    this._map.panTo(annotation.marker.getLatLng());
    annotation.marker.bindPopup(annotation.toponym + ' (<a href="' + annotation.source + '" target="_blank">Source</a>)').openPopup();
                
    // Clear sequence polylines
    for (idx in this._currentSequence) {
      this._map.removeLayer(this._currentSequence[idx]);
    }
    this._currentSequence = [];
    
    if (prevN && prevN.length > 0) {
      var coords = [annotation.marker.getLatLng()];
      for (idx in prevN)
        coords.push(prevN[idx].marker.getLatLng());
        
      drawSequenceLine(coords, { color: '#ff0000', opacity: 1 });      
    }

    if (nextN && nextN.length > 0) {
      coords = [annotation.marker.getLatLng()];
      for (idx in nextN)
        coords.push(nextN[idx].marker.getLatLng());
        
      drawSequenceLine(coords, { color: '#00ff00', opacity: 1 });
    }
  } else {
    this._map.closePopup();
  }
}

/**
 * Clears the map.
 */
pelagios.georesolution.MapView.prototype.clear = function() {
  var self = this;
  $.each(this._allMarkers, function(idx, marker) { self._map.removeLayer(marker); });
  $.each(this._currentSequence, function(idx, marker) { self._map.removeLayer(marker) });
}
