/**
 * The map component of the UI.
 * @param {Element} mapDiv the DIV to hold the Leaflet map
 * @constructor
 */
pelagios.georesolution.MapView = function(mapDiv) {
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
}

/**
 * Places a marker for the specified place.
 * @param {Object} place the place
 * @return the marker
 */
pelagios.georesolution.MapView.prototype.addPlaceMarker = function(place) {
  var self = this,
      STYLE_AUTOMATCH_NO_FIX = { color: '#0000ff', radius: 5, fillOpacity: 0.8 },
      STYLE_AUTOMATCH_FIXED = { color: '#0000ff', radius: 5, fillOpacity: 0.3 },
      STYLE_CORRECTED = { color: '#ff0000', radius: 5, fillOpacity: 0.8 };

  if (place.place_fixed && place.place_fixed.coordinate) { 
    var markerFixed = L.circleMarker(place.place_fixed.coordinate, STYLE_CORRECTED);
    
    if (place.place && place.place.coordinate) {
      var connectingLine;
      
      var showConnection = function() {
        connectingLine = L.polyline([place.place_fixed.coordinate, place.place.coordinate], STYLE_CORRECTED);
        connectingLine.addTo(self._map);
      }; 
      
      var hideConnection = function() {
        self._map.removeLayer(connectingLine);
      };
      
      markerFixed.on('mouseover', function(e) { showConnection(); });
      markerFixed.on('mouseout', function(e) { hideConnection(); });
      
      var markerAutomatch = L.circleMarker(place.place.coordinate, STYLE_AUTOMATCH_FIXED);
      markerAutomatch.on('mouseover', function(e) { showConnection(); });
      markerAutomatch.on('mouseout', function(e) { hideConnection(); });
      markerAutomatch.addTo(this._map);
    }    
    
    markerFixed.addTo(this._map);
    place.marker = markerFixed;
  } else if (place.place && place.place.coordinate) {
    var marker = L.circleMarker(place.place.coordinate, STYLE_AUTOMATCH_NO_FIX);
    marker.addTo(this._map); 
    marker.on('click', function(e) {
      marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup(); 
      if (self.onSelect) 
        self.onSelect(place);
    });

    place.marker = marker    
  }
}

/**
 * Highlights the specified place on the map.
 * @param {Object} place the place
 */
pelagios.georesolution.MapView.prototype.highlightPlace = function(place, prevN, nextN) {
  var self = this;

  // Utility function to draw the sequence line
  var drawSequenceLine = function(coords, style) {
    var line = L.polyline(coords, style);
    self._currentSequence.push(line);
    line.addTo(self._map);
  }
  
  if (place.marker) {
    this._map.panTo(place.marker.getLatLng());
    place.marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup();
                
    // Clear sequence polylines
    for (idx in this._currentSequence) {
      this._map.removeLayer(this._currentSequence[idx]);
    }
    this._currentSequence = [];
    
    if (prevN && prevN.length > 0) {
      var coords = [place.marker.getLatLng()];
      for (idx in prevN)
        coords.push(prevN[idx].marker.getLatLng());
        
      drawSequenceLine(coords, { color: '#ff0000', opacity: 1 });      
    }

    if (nextN && nextN.length > 0) {
      coords = [place.marker.getLatLng()];
      for (idx in nextN)
        coords.push(nextN[idx].marker.getLatLng());
        
      drawSequenceLine(coords, { color: '#00ff00', opacity: 1 });
    }
  }
}

/**
 * Clears the map.
 */
pelagios.georesolution.MapView.prototype.clear = function() {
  
}
