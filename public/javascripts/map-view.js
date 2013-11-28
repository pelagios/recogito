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
  
  this._currentSelection;
  
  this._allMarkers = [];
  
  this._styles = { 
    
    DEFAULT: { color: '#6464dd', fillColor:'#98BBF5', radius: 4, weight:2, opacity:1, fillOpacity: 1 },
    
    DEFAULT_HOVER: { color: '#6464dd', fillColor:'#98BBF5', radius: 8, weight:2, opacity:1, fillOpacity: 1 },
        
    CORRECTION: { color: '#ff0000', radius: 5, fillOpacity: 0.8 }
    
  }
}

// Inheritance - not the nicest pattern but works for our case
pelagios.georesolution.MapView.prototype = new pelagios.georesolution.HasEvents();

/**
 * Places a marker for the specified place.
 * @param {Object} annotation the place annotation
 */
pelagios.georesolution.MapView.prototype.addPlaceMarker = function(annotation) {
  var self = this;

  if (annotation.place_fixed && annotation.place_fixed.coordinate) { 
    var markerFixed = L.circleMarker(annotation.place_fixed.coordinate, this._styles.CORRECTION);
    
    if (annotation.place && annotation.place.coordinate) {
      var connectingLine;
      
      var showConnection = function() {
        connectingLine = L.polyline([annotation.place_fixed.coordinate, annotation.place.coordinate], this._styles.CORRECTION);
        connectingLine.addTo(self._map);
      }; 
      
      var hideConnection = function() {
        self._map.removeLayer(connectingLine);
      };
      
      markerFixed.on('mouseover', function(e) { showConnection(); });
      markerFixed.on('mouseout', function(e) { hideConnection(); });
      
      var markerAutomatch = L.circleMarker(annotation.place.coordinate, this._styles.DEFAULT);
      markerAutomatch.on('mouseover', function(e) { showConnection(); });
      markerAutomatch.on('mouseout', function(e) { hideConnection(); });
      markerAutomatch.addTo(this._map);
      
      self._allMarkers.push(markerAutomatch);
    } 
      
    self._allMarkers.push(markerFixed);
    markerFixed.addTo(this._map);
    annotation.marker = markerFixed;
  } else if (annotation.place && annotation.place.coordinate) {
    var marker = L.circleMarker(annotation.place.coordinate, this._styles.DEFAULT);
    marker.addTo(this._map); 
    marker.on('click', function(e) {
      marker.bindPopup(annotation.toponym + ' (<a href="' + annotation.source + '" target="_blank">Source</a>)').openPopup(); 
      self.fireEvent('select', annotation);
    });
    self._allMarkers.push(marker);

    annotation.marker = marker    
  }
}

pelagios.georesolution.MapView.prototype.emphasizePlace = function(annotation, prevN, nextN) {
  if (annotation.marker) {
    annotation.marker.setStyle(this._styles.DEFAULT_HOVER);
    annotation.marker.bringToFront();
  }
}

pelagios.georesolution.MapView.prototype.deemphasizePlace = function(annotation, prevN, nextN) {
  if (annotation.marker) {
    annotation.marker.setStyle(this._styles.DEFAULT);
  }
}

/**
 * Highlights the specified place on the map.
 * @param {Object} annotation the place annotation
 * @param {Array.<Object>} prevN the previous annotations in the list (if any)
 * @param {Array.<Object>} nextN the next annotations in the list (if any)
 */
pelagios.georesolution.MapView.prototype.selectPlace = function(annotation, prevN, nextN) {
  var self = this;

  // Utility function to draw the sequence line
  var drawSequenceLine = function(coords, opacity) {
    var line = L.polyline(coords, { color: self._styles.DEFAULT.color, opacity: opacity, weight:8 });
    line.setText('►', { repeat: true, offset: 3, attributes: { fill: '#fff', 'font-size':10 }});    
    self._currentSequence.push(line);
    line.addTo(self._map);
    line.bringToBack();
  }

  // Clear previous selection
  if (this._currentSelection) {
    this._map.removeLayer(this._currentSelection);
    delete this._currentSelection;
  }
  
  // Clear previous sequence
  for (idx in this._currentSequence) {
    this._map.removeLayer(this._currentSequence[idx]);
  }
  this._currentSequence = [];
  
  if (annotation.marker) {
    this._map.panTo(annotation.marker.getLatLng());
    this._currentSelection = L.marker(annotation.marker.getLatLng());
    this._currentSelection.bindPopup(annotation.toponym + ' (<a href="' + annotation.source + '" target="_blank">Source</a>)');
    this._currentSelection.addTo(self._map);
                
    if (prevN && prevN.length > 0) {
      var coords = [];
      for (idx in prevN)
        coords.push(prevN[idx].marker.getLatLng());
      coords.push(annotation.marker.getLatLng());
        
      drawSequenceLine(coords, 1);      
    }

    if (nextN && nextN.length > 0) {
      coords = [annotation.marker.getLatLng()];
      for (idx in nextN)
        coords.push(nextN[idx].marker.getLatLng());
        
      drawSequenceLine(coords, 0.3);
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
