/** A generic base map component **/
define(['common/hasEvents'], function(HasEvents) {
  
  var annotations = {},
      annotationsLayer;
  
  /** Code that is common across the main and the details map **/
  var Map = function(div, opt_active_basemap) {  
    var self = this,
        Layers = {
      
          DARE : L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
                   attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                   maxZoom:11
                 }), 
                 
          AWMC : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                   attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                     'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                     'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                     '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                 }), 
                 
          Bing : new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"), 
          
          OSM  : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	                 attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                 })
                 
        },
        
        baseLayers = 
          { 'Satellite': Layers.Bing, 
            'OSM': Layers.OSM,
            'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': Layers.AWMC, 
            'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': Layers.DARE },
            
        activeBaseLayer = (opt_active_basemap) ? baseLayers[opt_active_basemap] : Layers.AWMC;
    
    // Configure the map
    this.map = new L.Map(div, {
      center: new L.LatLng(41.893588, 12.488022),
      zoom: 5,
      layers: [ activeBaseLayer ]
    });
    this.map.addControl(new L.Control.Layers(baseLayers, null, { position: 'topleft' }));
    this.map.on('baselayerchange', function(e) { 
      if (self.map.getZoom() > e.layer.options.maxZoom)
        self.map.setZoom(e.layer.options.maxZoom);
        
      // Forward
      self.fireEvent('baselayerchange', e);
    });

    annotationsLayer = L.featureGroup();
    annotationsLayer.addTo(this.map);
    
    // Forward click events
    this.map.on('click', function(e) {
      self.fireEvent('click', e);
    });
    
    HasEvents.call(this);
  };
  Map.prototype = new HasEvents();
  
  Map.Styles = {
    VERIFIED: { color: '#118128', fillColor: '#1bcc3f', opacity: 1, fillOpacity: 1, radius: 6 },
    NOT_VERIFIED: { color: '#808080', fillColor:'#aaa', opacity: 1, fillOpacity: 1, radius: 6 },
    SEQUENCE: { opacity: 0.7, weight: 8 },
    REGION: { opacity: 0.5, fillOpacity: 0.2, radius: 20 }
  };
  
  Map.prototype.addAnnotation = function(annotation) {    
    var self = this, 
        place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
        style = (Map.Styles[annotation.status]) ? Map.Styles[annotation.status] : Map.Styles.NOT_VERIFIED;
    
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
    
    var style = $.extend(true, {}, Map.Styles.SEQUENCE);
    style.color = (Map.Styles[annotation.status]) ? Map.Styles[annotation.status].color : Map.Styles.NOT_VERIFIED.color;
    line = L.polyline(coords, style);
    line.setText('►', { repeat: true, offset: 3, attributes: { fill: '#fff', 'font-size':10 }});    
    annotationsLayer.addLayer(line);
    line.bringToBack();
  };
  
  Map.prototype.getAnnotationBounds = function() {
    return annotationsLayer.getBounds();
  };
  
  Map.prototype.fitToAnnotations = function() {
    var bounds = annotationsLayer.getBounds();
    if (bounds.isValid())
      map.fitBounds(annotationsLayer.getBounds());
  };
  
  Map.prototype.clearSearchresults = function() {
    searchresultsLayer.clearLayers();
    clearResultsButton.hide();
    this.fitToAnnotations();
  };
  
  Map.prototype.destroy = function() {
    annotations = {};
    this.map.remove();
  };
  
  return Map;
  
});
