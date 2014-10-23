define(['georesolution/common'], function(common) {
  
  /** Code that is common across the main and the details map **/
  var Map = function(div) {
    var dareLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
          attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
          maxZoom:11
        }),
        
        awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),
        
        bingLayer = 
          new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"),
        
        osmLayer = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	        attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
        }),
        
        map = new L.Map(div, {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 5,
          layers: [dareLayer]
        }),
        
        baseLayers = 
          { 'Satellite': bingLayer, 
            'OSM': osmLayer,
            'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': awmcLayer, 
            'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': dareLayer };
            
    // Configure the map
    map.addControl(new L.Control.Layers(baseLayers, null, { position: 'topleft' }));
    map.on('baselayerchange', function(e) { 
      if (map.getZoom() > e.layer.options.maxZoom)
        map.setZoom(e.layer.options.maxZoom);
    });
    
    // Lookup table place URI -> { marker: {}, annotations: [] }
    this.annotations = {};
    this.map = map;
  };
  
  Map.prototype.addAnnotation = function(annotation) {
    var self = this, 
        place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
    
        /** Helper function to create the marker **/
        createMarker = function(place, style) {
          var marker = L.circleMarker(place.coordinate, style);
    
          // if (self._isVisible(annotation))
            marker.addTo(self.map); 
      
          /*
          marker.on('click', function(e) {
            self.fireEvent('select', annotation);
            self._currentSelection.openPopup();
          });
          */
  

        };
    
    if ((place && place.coordinate) && (annotation.status == 'VERIFIED' || annotation.status == 'NOT_VERIFIED')) {
      var annotationsForPlace = this.annotations[place.uri],
          marker = (annotationsForPlace) ? annotationsForPlace.marker : false,
          annotations = (annotationsForPlace) ? annotationsForPlace.annotations : false;
          
      if (annotationsForPlace) {
        annotation.marker = marker;
        annotations.push(annotation);
      } else { 
        annotationsForPlace = {
          marker: createMarker(place),
          annotations: [annotation]
        };
      }      
    }
  };
  
  return Map;
  
});
