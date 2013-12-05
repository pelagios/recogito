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
  
  var self = this,
      baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
        attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
      }),
      selector_template = 
        '<div class="map-selector">' +
        '</div>';
        
  this._map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 5,
    layers: [baseLayer],
    minZoom: 3,
    maxZoom: 11
  });

  // List of current EGD parts
  // [{ name: { visible: true | false, tags: [ { name: ..., visible: true | false }] }]
  this._parts = {};
  
  // Part/tag visibility selector widget
  this._selector = $(selector_template);
  $(this._selector).appendTo(mapDiv);
  $(this._selector).click(function() { self._currentPart += 1; self.redraw(); });
  $(this._selector).on('click', '.map-selector-part', function(e) {
    var part = $(e.target).data('part');
    self._parts[part].visible = !self._parts[part].visible;
    self.redraw(); 
  });
  
  this._currentSequence = [];
  
  this._currentSelection;
  
  this._allAnnotations = [];
  
  this._MARKER_SIZE = 4;
      
  this._MARKER_SIZE_HOVER = 8;
  
  this._styles = { 
    
    NOT_VERIFIED: { color: '#808080', fillColor:'#aaa', radius: self._MARKER_SIZE, weight:2, opacity:1, fillOpacity: 1 },
    
    VERIFIED: { color: '#118128', fillColor:'#1bcc3f', radius: self._MARKER_SIZE, weight:2, opacity:1, fillOpacity: 1 }
    
  }
}

// Inheritance - not the nicest pattern but works for our case
pelagios.georesolution.MapView.prototype = new pelagios.georesolution.HasEvents();

pelagios.georesolution.MapView.prototype._isVisible = function(annotation) {
  var part_settings = this._parts[annotation.part];
  if (part_settings) {
    // TODO take visibility of tags into account
    return part_settings.visible;
  }
}

/**
 * Places a marker for the specified place.
 * @param {Object} annotation the place annotation
 */
pelagios.georesolution.MapView.prototype.addPlaceMarker = function(annotation) {
  var self = this;

  // Update parts & tags list
  if (!this._parts[annotation.part])
    this._parts[annotation.part] = { visible: true };

  // Update selector widget
  var html = "";
  for (name in self._parts) {
    html += '<input type="checkbox" checked="true" data-part="' + name + '" class="map-selector-part">' + name + '</input><br/>';
  }
  $(self._selector).html(html);
    
  var createMarker = function(place, style) {
    var marker = L.circleMarker(place.coordinate, style);
    
    if (self._isVisible(annotation))
      marker.addTo(self._map); 
      
    marker.on('click', function(e) {
      self.fireEvent('select', annotation);
      self._currentSelection.openPopup();
    });
  
    annotation.marker = marker  
    self._allAnnotations.push(annotation);
  }

  var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
  if (place && place.coordinate) {
    var style = undefined;
    switch(annotation.status) {
      case 'VERIFIED': 
        style = this._styles.VERIFIED;
        break;
        
      case 'NOT_VERIFIED':
        style = this._styles.NOT_VERIFIED;
        break;
    }
    
    if (style) {
      createMarker(place, style);
    }
  }
}

pelagios.georesolution.MapView.prototype.removePlaceMarker = function(annotation) {
  if (annotation.marker)
    this._map.removeLayer(annotation.marker);
}

pelagios.georesolution.MapView.prototype.emphasizePlace = function(annotation, prevN, nextN) {
  if (annotation.marker && this._isVisible(annotation)) {      
    var style = annotation.marker.options;
    style.radius = this._MARKER_SIZE_HOVER;
    annotation.marker.setStyle(style);
    annotation.marker.bringToFront();
  }
}

pelagios.georesolution.MapView.prototype.deemphasizePlace = function(annotation, prevN, nextN) {
  if (annotation.marker && this._isVisible(annotation)) {
    var style = annotation.marker.options;
    style.radius = this._MARKER_SIZE;
    annotation.marker.setStyle(style);
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
    var line = L.polyline(coords, { color: annotation.marker.options.color, opacity: opacity, weight:8 });
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

pelagios.georesolution.MapView.prototype.redraw = function() {
  this.clear();
  
  var self = this;
  $.each(this._allAnnotations, function(idx, annotation) {
    if (annotation.marker && self._isVisible(annotation))
      annotation.marker.addTo(self._map);
  });
}

/**
 * Clears the map.
 */
pelagios.georesolution.MapView.prototype.clear = function() {
  var self = this;
  $.each(this._allAnnotations, function(idx, annotation) { self._map.removeLayer(annotation.marker); });
  $.each(this._currentSequence, function(idx, marker) { self._map.removeLayer(marker) });
}
