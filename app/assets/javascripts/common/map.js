/** A generic base map component **/
define(['common/hasEvents'], function(HasEvents) {
  
  var Map = function(div, popup_fn, opt_basemap, opt_controlposition) {  
    var self = this,
        control_position = (opt_controlposition) ? opt_controlposition : 'topleft';
        Layers = {
      
          DARE : L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
                   attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                   minZoom:3,
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
            
        activeBaseLayer = (opt_basemap) ? baseLayers[opt_basemap] : Layers.AWMC;

    // We'll add the map as global field, so that subclasses can have access
    this.map = new L.Map(div, {
      center: new L.LatLng(41.893588, 12.488022),
      zoom: 5,
      zoomControl: false,
      layers: [ activeBaseLayer ]
    });

    this.map.addControl(L.control.zoom({ position: control_position }) )
    this.map.addControl(new L.Control.Layers(baseLayers, null, { position: control_position }));    
    this.map.on('baselayerchange', function(e) { 
      if (self.map.getZoom() > e.layer.options.maxZoom)
        self.map.setZoom(e.layer.options.maxZoom);
        
      self.fireEvent('baselayerchange', e);
    });

    this.annotations = {};
    this.annotationsLayer = L.featureGroup();
    this.annotationsLayer.addTo(this.map);
    
    this.sequenceLayer = L.featureGroup();
    this.sequenceLayer.addTo(this.map);
        
    // Use provided popup fuction, or create a default one
    this.createPopup = (popup_fn) ? popup_fn : function(place, annotationsWithContext) {
      return '<strong>' + place.title + '</strong><br/>' +
             '<small>' + place.names.slice(0, 8).join(', ') + '</small>';
    };
    
    // Forward click events
    this.map.on('click', function(e) { self.fireEvent('click', e); });
    
    // Close popups on Escape key
    jQuery(div).on('keyup', function(e) {
      if (e.which == 27)
        self.map.closePopup();
    });
    
    HasEvents.call(this);
  };
  
  Map.prototype = new HasEvents();
  
  /** Marker styles **/
  Map.Styles = {
    VERIFIED: { color: '#118128', fillColor: '#1bcc3f', opacity: 1, fillOpacity: 1, radius: 6 },
    NOT_VERIFIED: { color: '#808080', fillColor:'#aaa', opacity: 1, fillOpacity: 1, radius: 6 },
    SEQUENCE: { opacity: 1, weight: 2 },
    REGION: { opacity: 0.5, fillOpacity: 0.2, radius: 20 }
  };
  
  /** Returns the bounds of all annotations currently on the map **/
  Map.prototype.getAnnotationBounds = function() {
    var annotationBounds = this.annotationsLayer.getBounds(),
        sequenceBounds = this.sequenceLayer.getBounds();
        
    if (sequenceBounds.isValid())
      annotationBounds.extend(sequenceBounds);
    
    return annotationBounds;
  };
  
  /** Returns the minimum zoom level of the currently active base layer **/
  Map.prototype.getCurrentMinZoom = function() {
    var zoom;
    
    this.map.eachLayer(function(layer) {
      // Warning: this is a real hack - there doesn't seem to be a way to get
      // the active baselayer, so we get ALL layers and check if they have
      // a _url field. If so - that's a tile layer
      if (layer._url)
        if (layer.options)
          zoom = layer.options.minZoom;
    });

    return zoom;
  };
  
  Map.prototype.refresh = function() {
    this.map.invalidateSize();
  };
  
  /** Destroys the map **/  
  Map.prototype.destroy = function() {
    this.annotations = {};
    this.map.remove();
  };
  
  /** Adds an annotation to the map **/
  Map.prototype.addAnnotation = function(annotation, opt_context) {    
    var self = this, place, style, annotationsForPlace, marker, a,
    
        /** Helper function to create the marker **/
        createMarker = function(place, style) {
          var marker = L.circleMarker(place.coordinate, style);
          marker.on('click', function(e) {
            self.fireEvent('selectAnnotation', self.annotations[place.uri].annotations);
          });
          self.annotationsLayer.addLayer(marker); 
          return marker;
        };
    
    if (annotation.status == 'VERIFIED' || annotation.status == 'NOT_VERIFIED') {   
      place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;    
    
      if (place && place.coordinate) {
        style = (Map.Styles[annotation.status]) ? Map.Styles[annotation.status] : Map.Styles.NOT_VERIFIED;
              
        annotationsForPlace = this.annotations[place.uri],
        marker = (annotationsForPlace) ? annotationsForPlace.marker : false,
        a = (annotationsForPlace) ? annotationsForPlace.annotations : false;
          
        if (annotationsForPlace) {
          a.push([ annotation, opt_context ]);
        } else { 
          annotationsForPlace = {
            marker: createMarker(place, style),
            annotations: [[ annotation, opt_context ]]
          };
          this.annotations[place.uri] = annotationsForPlace;
        }
      }
    
    }
  };

  /** Adds a sequence line to the map **/  
  Map.prototype.setSequence = function(annotation, previous, next) {
    var i, coords = [], line, style,
        pushLatLon = function(annotation) {
          var c;
          
          if (annotation.place_fixed) {
            if (annotation.place_fixed.coordinate)
              coords.push(annotation.place_fixed.coordinate);
          } else if (annotation.place && annotation.place.coordinate) {
            coords.push(annotation.place.coordinate);
          }          
        };
    
    this.sequenceLayer.clearLayers();
    
    for (i = 0; i < previous.length; i++)
      pushLatLon(previous[i]);
       
    pushLatLon(annotation);
          
    for (var i = 0; i < next.length; i++)
      pushLatLon(next[i]);
    
    style = jQuery.extend(true, {}, Map.Styles.SEQUENCE);
    style.color = (Map.Styles[annotation.status]) ? Map.Styles[annotation.status].color : Map.Styles.NOT_VERIFIED.color;
    line = L.polyline(coords, style);
    line.setText('►', { repeat: true, offset: 5, attributes: { fill: style.color, 'font-size':17 }});    
    this.sequenceLayer.addLayer(line);
    this.sequenceLayer.bringToBack();
  };
  
  /** Highlights a marker by opening its popup **/
  Map.prototype.showMarker = function(annotation) {
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
        markerAndAnnotations, popupHtml;
        
    if (place) {
      markerAndAnnotations = this.annotations[place.uri];
      if (markerAndAnnotations) {
        markerAndAnnotations.marker.bindPopup(this.createPopup(place, markerAndAnnotations.annotations)).openPopup();          
      }
    }
  };
  
  /** Fits the map zoom level to cover the bounds of all annotations **/
  Map.prototype.fitToAnnotations = function() {
    var bounds = this.getAnnotationBounds();
    if (bounds.isValid()) {
      this.map.fitBounds(bounds, { animate: false });
    }
  };
  
  /** Removes all annotations from the map **/
  Map.prototype.clearAnnotations = function() {
    this.annotations = {};
    this.annotationsLayer.clearLayers();
    this.sequenceLayer.clearLayers();
  };
  
  return Map;
  
});
