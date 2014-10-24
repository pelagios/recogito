define(['georesolution/common'], function(common) {
  
  var map,
      annotations = {},
      annotationsLayer,
      searchresults = {},
      searchresultsLayer,
      clearResultsButton = $('<div id="clear-results">Clear Results</div>'),
      
      Styles = {
        VERIFIED: { color: '#118128', fillColor: '#1bcc3f', opacity: 1, fillOpacity: 1, radius: 6 },
        NOT_VERIFIED: { color: '#808080', fillColor:'#aaa', opacity: 1, fillOpacity: 1, radius: 6 },
        SEQUENCE: { opacity: 0.7, weight: 8 },
        REGION: { opacity: 0.5, fillOpacity: 0.2, radius: 20 }
      };
  
  /** Code that is common across the main and the details map **/
  var Map = function(div) {        
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
        
        bingLayer = 
          new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"),
        
        osmLayer = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	        attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
        }),
        
        baseLayers = 
          { 'Satellite': bingLayer, 
            'OSM': osmLayer,
            'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': awmcLayer, 
            'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': dareLayer };
            
    // Configure the map
    map = new L.Map(div, {
      center: new L.LatLng(41.893588, 12.488022),
      zoom: 5,
      layers: [awmcLayer]
    });
    map.addControl(new L.Control.Layers(baseLayers, null, { position: 'topleft' }));
    map.on('baselayerchange', function(e) { 
      if (map.getZoom() > e.layer.options.maxZoom)
        map.setZoom(e.layer.options.maxZoom);
    });

    annotationsLayer = L.featureGroup();
    annotationsLayer.addTo(map);
    
    searchresultsLayer = L.featureGroup();
    searchresultsLayer.addTo(map);
    
    $(div).on('click', '.searchresult-popup', function(e) {
      self.fireEvent('selectSearchresult', searchresults[e.target.href].result);
      return false;
    });
    
    map.on('click', function(e) {
      self.fireEvent('click', e);
    });
    
    clearResultsButton.hide();
    clearResultsButton.on('click', function() { self.clearSearchresults(); });
    $(div).append(clearResultsButton);
    
    common.HasEvents.call(this);
  };
  
  Map.prototype = new common.HasEvents();
  
  Map.prototype.addAnnotation = function(annotation) {    
    var self = this, 
        place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
        style = (Styles[annotation.status]) ? Styles[annotation.status] : Styles.NOT_VERIFIED;
    
        /** Helper function to create the marker **/
        createMarker = function(place, style) {
          var marker = L.circleMarker(place.coordinate, style);
          marker.on('click', function(e) {
            self.fireEvent('selectAnnotation', annotations[place.uri].annotations);
          });
          
          annotationsLayer.addLayer(marker); 
        };
    
    if ((place && place.coordinate) && (annotation.status == 'VERIFIED' || annotation.status == 'NOT_VERIFIED')) {
      var annotationsForPlace = annotations[place.uri],
          marker = (annotationsForPlace) ? annotationsForPlace.marker : false,
          a = (annotationsForPlace) ? annotationsForPlace.annotations : false;
          
      if (annotationsForPlace) {
        a.push(annotation);
      } else { 
        annotationsForPlace = {
          marker: createMarker(place, style),
          annotations: [annotation]
        };
      }      
    }
  };
  
  Map.prototype.addSequence = function(annotation, prev_annotations, next_annotations) {
    var i, coords = [], line,
        pushLatLon = function(annotation) {
          var c;
          
          if (annotation.place_fixed) {
            if (annotation.place_fixed.coordinate)
              coords.push(annotation.place_fixed.coordinate);
          } else if (annotation.place && annotation.place.coordinate) {
            coords.push(annotation.place.coordinate);
          }          
        };
    
    for (i = 0; i < prev_annotations.length; i++)
      pushLatLon(prev_annotations[i]);
       
    pushLatLon(annotation);
          
    for (var i = 0; i < next_annotations.length; i++)
      pushLatLon(next_annotations[i]);
    
    var style = $.extend(true, {}, Styles.SEQUENCE);
    style.color = (Styles[annotation.status]) ? Styles[annotation.status].color : Styles.NOT_VERIFIED.color;
    line = L.polyline(coords, style);
    line.setText('►', { repeat: true, offset: 3, attributes: { fill: '#fff', 'font-size':10 }});    
    annotationsLayer.addLayer(line);
    line.bringToBack();
  };
  
  Map.prototype.addSearchresult = function(result) {
    if (result.coordinate) {
      var marker = L.marker(result.coordinate);
      marker.bindPopup(
        '<strong>' + result.title + '</strong>' +
        '<br/>' +
        '<small>' + result.names.slice(0, 10).join(', ') + '</small>' +
        '<br/>' + 
        '<a href="' + result.uri + '" class="searchresult-popup" onclick="return false;">' + common.Utils.formatGazetteerURI(result.uri)) + '</a>';
      
      searchresults[result.uri] = { result: result, marker: marker };
      searchresultsLayer.addLayer(marker);
      clearResultsButton.show();
    }
  };
  
  Map.prototype.selectSearchresult = function(uri) {
    var result = searchresults[uri];
    if (result)
      result.marker.openPopup();
  }
  
  Map.prototype.fitToAnnotations = function() {
    var bounds = annotationsLayer.getBounds();
    if (bounds.isValid())
      map.fitBounds(annotationsLayer.getBounds());
  }
  
  Map.prototype.fitToSearchresults = function() {
    var bounds,
        searchBounds = searchresultsLayer.getBounds(),
        annotationBounds = annotationsLayer.getBounds();
    
    bounds = (searchBounds.isValid()) ? searchBounds : annotationBounds;
    if (annotationBounds.isValid())
      bounds.extend(annotationBounds);
      
    if (bounds)
      map.fitBounds(bounds);
  };
  
  Map.prototype.clearSearchresults = function() {
    searchresultsLayer.clearLayers();
    clearResultsButton.hide();
    this.fitToAnnotations();
  };
  
  Map.prototype.destroy = function() {
    annotations = {};
    searchresults = {};
    map.remove();
  }
  
  return Map;
  
});
