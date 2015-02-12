/** A generic base map component **/
define(['common/hasEvents'], function(HasEvents) {
  
  var Map = function(div, opt_popup_fn, opt_style_fn, opt_basemap, opt_controlposition) {  
    var self = this,
        
        popupTimer = false,
        
        annotations = {},
        placesByAnnotations = {},
        
        maxAnnotationsPerPlace = 1,
        shapeMarkerLayer = L.featureGroup(),
        pointMarkerLayer = L.featureGroup(),
        sequenceLayer = L.featureGroup(),
        
        /** Use provided popup function, or init a basic default **/
        createPopup = (opt_popup_fn) ? opt_popup_fn : function(place, annotationsWithContext) {
          return '<strong>' + place.title + '</strong><br/>' +
                 '<small>' + place.names.slice(0, 8).join(', ') + '</small>';
        },

        createStyle = (opt_style_fn) ? opt_style_fn : function(annotations) {
          var place = (annotations[0].place_fixed) ? annotations[0].place_fixed : annotations[0].place,
              color = '#118128', fillColor = '#1bcc3f';
          
          jQuery.each(annotations, function(idx, a) {
            if (a.status === 'NOT_VERIFIED') {
              color = '#808080';
              fillColor = '#aaa';
            }
          });
          
          if (place.geometry) {
            return { color: color, fillColor: fillColor, opacity: 1, weight: 1.5, fillOpacity: 0.4 };
          } else {
            return { color: color, fillColor: fillColor, opacity: 1, weight: 1.5, fillOpacity: 1, radius: 5 };
          }
        },
        
        /** Map layers **/
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
                 
          BingSatellite : new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"), 
          
          BingRoad : new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn", { type: 'Road' }), 
          
          OSM  : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	                 attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                 })
                 
        },
        
        /** Base Layer dictionary **/
        baseLayers = 
          { 'Bing Satellite': Layers.BingSatellite, 
            'Bing Road': Layers.BingRoad,
            'OpenStreetMap': Layers.OSM,
            'Empty Base Map (<a href="http://awmc.unc.edu/wordpress/tiles/map-tile-information" target="_blank">AWMC</a>)': Layers.AWMC, 
            'Roman Empire Base Map (<a href="http://imperium.ahlfeldt.se/" target="_blank">DARE</a>)': Layers.DARE },
            
        /** Use provided active baselayer or AWMC **/
        activeBaseLayer = (opt_basemap) ? baseLayers[opt_basemap] : Layers.AWMC,
        
        /** Position of the map controls **/
        control_position = (opt_controlposition) ? opt_controlposition : 'topleft',
        
        /** Helper to init the global this.map member **/
        initMap = function() {
          var map = new L.Map(div, {
            center: new L.LatLng(41.893588, 12.488022),
            zoom: 5,
            zoomControl: false,
            layers: [ activeBaseLayer ]
          });

          map.addControl(L.control.zoom({ position: control_position }) )
          map.addControl(new L.Control.Layers(baseLayers, null, { position: control_position }));    
          map.on('baselayerchange', function(e) { 
            if (map.getZoom() > e.layer.options.maxZoom) {
              map.setZoom(e.layer.options.maxZoom);
            }
        
            self.fireEvent('baselayerchange', e);
          });
          
          // Forward click events
          map.on('click', function(e) { self.fireEvent('click', e); });
          
          return map;
        };
  
    /** We make the map public so that subclasses can access it **/
    this.map = initMap();
    shapeMarkerLayer.addTo(this.map);
    pointMarkerLayer.addTo(this.map);
    sequenceLayer.addTo(this.map);
    
    // Close popups on Escape key
    jQuery(div).on('keyup', function(e) {
      if (e.which == 27)
        self.map.closePopup();
    });
    
    /****                ****/
    /**                    **/
    /** Privileged methods **/
    /**                    **/
    /****                ****/
     
    /** Returns the bounds of all annotations currently on the map **/
    this.getAnnotationBounds = function() {
      var pointMarkerBounds = pointMarkerLayer.getBounds(),
          shapeMarkerBounds = shapeMarkerLayer.getBounds(),
          sequenceBounds = sequenceLayer.getBounds();
        
      if (shapeMarkerBounds.isValid())
        pointMarkerBounds.extend(shapeMarkerBounds);
        
      if (sequenceBounds.isValid())
        pointMarkerBounds.extend(sequenceBounds);
    
      return pointMarkerBounds;
    };
  
    /** Returns the minimum zoom level of the currently active base layer **/
    this.getCurrentMinZoom = function() {
      var zoom;
    
      self.map.eachLayer(function(layer) {
        // Warning: this is a real hack - there doesn't seem to be a way to get
        // the active baselayer, so we get ALL layers and check if they have
        // a _url field. If so - that's a tile layer
        if (layer._url)
          if (layer.options)
            zoom = layer.options.minZoom;
      });

      return zoom;
    };
  
    /** Redraws the map **/
    this.refresh = function() {
      self.map.invalidateSize();
    };
  
    /** Destroys the map **/  
    this.destroy = function() {
      annotations = {};
      self.map.remove();
    };
    
    /** Returns the marker for a particular annotation **/
    this.getMarkerForAnnotation = function(annotation) {
      var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
          record, marker;
        
      if (place) {
        record = annotations[place.uri];
        if (record) {
          return record.marker;          
        }
      }
    };
  
    /** Adds an annotation to the map **/
    this.addAnnotation = function(annotation, opt_context) {    
      var self = this, place, style, annotationsForPlace, marker, a,
    
          /** Helper function to create the marker **/
          createMarker = function(place, annotationsForPlace) {
            var marker; 

            if (place.geometry) {
              marker = L.geoJson(place.geometry, createStyle(annotationsForPlace)); // TODO adapt style for shapes
              shapeMarkerLayer.addLayer(marker); 
            } else {
              marker = L.circleMarker(place.coordinate, createStyle(annotationsForPlace));
              pointMarkerLayer.addLayer(marker); 
            }
                
            marker.on('click', function(e) {
              self.showPopup(annotation);
              self.fireEvent('select', jQuery.map(annotations[place.uri].annotations, function(tuple) { 
                return tuple[0];
              }));
            });
              
            return marker;
          };
    
      if (annotation.status == 'VERIFIED' || annotation.status == 'NOT_VERIFIED') {   
        place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;    
    
        if (place && place.coordinate) {
          annotationsForPlace = annotations[place.uri],
          marker = (annotationsForPlace) ? annotationsForPlace.marker : false,
          a = (annotationsForPlace) ? annotationsForPlace.annotations : false;
          
          if (annotationsForPlace) {
            a.push([ annotation, opt_context ]);
            if (a.length > maxAnnotationsPerPlace)
              maxAnnotationsPerPlace = a.length;
          } else { 
            // New marker
            annotationsForPlace = {
              marker: createMarker(place, [ annotation ]),
              annotations: [[ annotation, opt_context ]]
            };
            annotations[place.uri] = annotationsForPlace;
          }
          
          placesByAnnotations[annotation.id] = place.uri;
        }
      }
      
      pointMarkerLayer.bringToFront();
    };
    
    /** Returns the maximum marker weight, i.e. number of annotations on a single marker **/
    this.getMaxMarkerWeight = function() {
      return maxAnnotationsPerPlace;
    }
    
    /** Helper function that updates all marker styles **/
    this.refreshStyles = function() {
      jQuery.each(annotations, function(uri, annotationsForPlace) {
        var marker = annotationsForPlace.marker,
            annotations = jQuery.map(annotationsForPlace.annotations, function(tuple) {
              return tuple[0];
            });
            
        marker.setStyle(createStyle(annotations));
      });
    },
    
    /** Redraws an annotation **/
    this.updateAnnotation = function(annotation) {
      self.removeAnnotation(annotation.id);
      self.addAnnotation(annotation);
    };
    
    /** Removes an annotation from the map **/
    this.removeAnnotation = function(id) {
      var placeURI = placesByAnnotations[id],
          recordForThisPlace = (placeURI) ? annotations[placeURI] : false,
          markerForThisPlace = (recordForThisPlace) ? recordForThisPlace.marker : false,
          annotationsForPlace = (recordForThisPlace) ? recordForThisPlace.annotations : false,
          annotationToRemove, idxToRemove;
      
      if (annotationsForPlace) {
        annotationToRemove = jQuery.grep(annotationsForPlace, function(tuple, idx) {
          var annotation = tuple[0]; // tuple[1] is the context - we don't need this here
          return annotation.id === id;
        });
        
        if (annotationToRemove.length === 1) {
          idxToRemove = annotationsForPlace.indexOf(annotationToRemove[0]);
          if (idxToRemove > -1) {
            annotationsForPlace.splice(idxToRemove, 1);
          
            // Check if empty - if so, remove the whole thing
            if (annotationsForPlace.length === 0) {
              shapeMarkerLayer.removeLayer(markerForThisPlace);
              pointMarkerLayer.removeLayer(markerForThisPlace);
              delete annotations[placeURI];
            }
          }
        }
      }
    };

    /** Adds a sequence line to the map **/  
    this.setSequence = function(annotation, previous, next) {
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
    
      sequenceLayer.clearLayers();
    
      for (i = 0; i < previous.length; i++)
        pushLatLon(previous[i]);
       
      pushLatLon(annotation);
          
      for (var i = 0; i < next.length; i++)
        pushLatLon(next[i]);
    
      style = { opacity: 1, weight: 2 };
      style.color = (annotation.status === 'VERIFIED') ? '#118128' : '#808080';
      line = L.polyline(coords, style);
      line.setText('►', { repeat: true, offset: 5, attributes: { fill: style.color, 'font-size':17 }});    
      sequenceLayer.addLayer(line);
      sequenceLayer.bringToFront();
    };
  
    /** Highlights a marker by opening its popup **/
    this.showPopup = function(annotation) {
      var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place,
          markerAndAnnotations, popupHtml;
        
      if (place) {
        markerAndAnnotations = annotations[place.uri];
        if (markerAndAnnotations) {
          // We're using a short timeout interval so that people can quickly skim through
          // the table without ugly popup jitter
          if (popupTimer) {
            window.clearTimeout(popupTimer);
          }
          
          popupTimer = window.setTimeout(function() {
            if (markerAndAnnotations.marker) { // The marker may have been detached in the meantime
              markerAndAnnotations.marker.bindPopup(createPopup(place, markerAndAnnotations.annotations)).openPopup();          
            }
          }, 100);
        } else {
          self.map.closePopup();
        }
      } else {
        self.map.closePopup();
      }
    };
    
    /** Hides the current popup **/
    this.hidePopup = function() {
      self.map.closePopup();
    },
  
    /** Fits the map zoom level to cover the bounds of all annotations **/
    this.fitToAnnotations = function() {
      var bounds = self.getAnnotationBounds();
      if (bounds.isValid()) {
        self.map.fitBounds(bounds, { animate: false });
      }
    };
  
    /** Removes all annotations from the map **/
    this.clearAnnotations = function() {
      annotations = {};
      shapeMarkerLayer.clearLayers();
      pointMarkerLayer.clearLayers();
      sequenceLayer.clearLayers();
    };
    
    HasEvents.call(this);
  };
  
  Map.prototype = new HasEvents();
  
  return Map;
  
});
