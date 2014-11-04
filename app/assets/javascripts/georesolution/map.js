define(['georesolution/common'], function(common) {
  
  /**
   * The map component of the UI.
   * 
   * Emits the following events:
   * 'select' ... when a place was selected on a map
   * 
   * @param {Element} mapDiv the DIV to hold the Leaflet map
   * @constructor
   */
  var MapView = function(mapDiv) {
    // Inheritance - not the nicest pattern but works for our case
    common.HasEvents.call(this);
  
    var self = this,
        dareLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
          attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
          maxZoom:11
        }),
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),
        bingLayer = new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"),
        osmLayer = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	        attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
        }), 
        selector_template = 
          '<div class="map-selector">' +
          '</div>';
          
    this._map = new L.Map(mapDiv, {
      center: new L.LatLng(41.893588, 12.488022),
      zoom: 5,
      layers: [dareLayer],
      minZoom: 3
    });
    
    var baseLayers = { 'Satellite': bingLayer, 
                       'OSM': osmLayer,
                       'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': awmcLayer, 
                       'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': dareLayer };
    this._map.addControl(new L.Control.Layers(baseLayers, null, { position: 'topleft' }));

    this._map.on('baselayerchange', function(e) { 
      if (self._map.getZoom() > e.layer.options.maxZoom)
        self._map.setZoom(e.layer.options.maxZoom);
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
  }

  // Inheritance - not the nicest pattern but works for our case
  MapView.prototype = new common.HasEvents();

  MapView.prototype._isVisible = function(annotation) {
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
  MapView.prototype.addPlaceMarker = function(annotation) {
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
      if (annotation.status == 'VERIFIED' || annotation.status == 'NOT_VERIFIED')
        createMarker(place, getStyle(annotation.status, place.category));
    }
  }

  MapView.prototype.removePlaceMarker = function(annotation) {
    if (annotation.marker) {
      this._map.removeLayer(annotation.marker);
      annotation.marker = false;
    }
  }

  MapView.prototype.emphasizePlace = function(annotation, prevN, nextN) {
    if (annotation.marker && this._isVisible(annotation)) {      
      var style = annotation.marker.options;
      style.radius = style.radius * 2;
      annotation.marker.setStyle(style);
      annotation.marker.bringToFront();
    }
  }

  MapView.prototype.deemphasizePlace = function(annotation, prevN, nextN) {
    if (annotation.marker && this._isVisible(annotation)) {
      var style = annotation.marker.options;
      style.radius = style.radius * 0.5;
      annotation.marker.setStyle(style);
    }
  }

  /**
   * Highlights the specified place on the map.
   * @param {Object} annotation the place annotation
   * @param {Array.<Object>} prevN the previous annotations in the list (if any)
   * @param {Array.<Object>} nextN the next annotations in the list (if any)
   */
  MapView.prototype.selectPlace = function(annotation, prevN, nextN) {
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

  MapView.prototype.redraw = function() {
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
  MapView.prototype.clear = function() {
    var self = this;
    $.each(this._allAnnotations, function(idx, annotation) { self._map.removeLayer(annotation.marker); });
    $.each(this._currentSequence, function(idx, marker) { self._map.removeLayer(marker) });
  }
  
  function getStyle(status, category) {
    var color, fillColor, opacity = 1, fillOpacity = 1, radius = 4;
    
    if (status == 'VERIFIED') {
      color = '#118128';
      fillColor ='#1bcc3f';
    } else {
      color = '#808080';
      fillColor ='#aaa';
    }
    
    if (category == 'REGION') {
      opacity = 0.5;
      fillOpacity = 0.2;
      radius = 20;
    }
    
    return { color: color, fillColor: fillColor, radius: radius, opacity: opacity, fillOpacity: fillOpacity };
  }

  return MapView;

});
