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
      STYLE_AUTOMATCH_NO_FIX = { color: '#0000ff', radius: 4, fillOpacity: 0.8 },
      STYLE_AUTOMATCH_FIXED = { color: '#0000ff', radius: 4, fillOpacity: 0.3 },
      STYLE_CORRECTED = { color: '#ff0000', radius: 4, fillOpacity: 0.8 };

  if (place.coordinate_fixed) { 
    var markerFixed = L.circleMarker(place.coordinate_fixed, STYLE_CORRECTED);
    markerFixed.addTo(this._map);
    
    var markerAutomatch = L.circleMarker(place.coordinate, STYLE_AUTOMATCH_FIXED);
    markerAutomatch.addTo(this._map);
    
    return markerFixed;
  } else {
    var marker = L.circleMarker(place.coordinate, STYLE_AUTOMATCH_NO_FIX);
    marker.addTo(this._map); 
    marker.on('click', function(e) {
      marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup(); 
      if (self.onSelect) 
        self.onSelect(place);
    });
    
    return marker;
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
  
  if (place.coordinate) {
    this._map.panTo(place.coordinate);
    place.marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup();
                
    // Clear sequence polylines
    for (idx in this._currentSequence) {
      this._map.removeLayer(this._currentSequence[idx]);
    }
    this._currentSequence = [];
    
    if (prevN && prevN.length > 0) {
      var coords = [place.coordinate];
      for (idx in prevN)
        coords.push(prevN[idx].coordinate);
        
      drawSequenceLine(coords, { color: '#ff0000', opacity: 1 });      
    }

    if (nextN && nextN.length > 0) {
      coords = [place.coordinate];
      for (idx in nextN)
        coords.push(nextN[idx].coordinate);
        
      drawSequenceLine(coords, { color: '#00ff00', opacity: 1 });
    }
  }
}


/*              
  /** Connecting line between a place and a manual fix **
  var lastFixConnection = undefined;
        
  // TODO eliminate code duplication
  if (place.fixedCoordinate) {
    place.marker = L.circleMarker(place.fixedCoordinate, markerStyleCorrected);
    place.marker.addTo(map); 
    place.marker.on('click', function(e) {
      place.marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup(); 

      if (lastFixConnection) {
        map.removeLayer(lastFixConnection);
        lastFixConnection = undefined;
      }

      if (place.coordinate) {
        var connection = [ place.coordinate, place.fixedCoordinate ];
        lastFixConnection = L.polyline(connection, { color: 'yellow', opacity: 1 });
        lastFixConnection.addTo(map);
      }
    });
  }
*/
