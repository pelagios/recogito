define(['imageannotation/config', 'imageannotation/events', 'imageannotation/viewer/annotationLayer'], function(Config, Events, AnnotationLayer) {
  
  var _map;
  
  var Viewer = function(divId, eventBroker) {      
    var div = jQuery('#' + divId),
    
        divSize = [ div.outerWidth(), div.outerHeight() ],
    
        projection = new ol.proj.Projection({
          code: 'ZOOMIFY',
          units: 'pixels',
          extent: [0, 0, Config.width, Config.height]
        }),
        
        tileSource = new ol.source.Zoomify({
          url: Config.url,
          size: [ Config.width, Config.height ]
        }),
        
        tileLayer = new ol.layer.Tile({ source: tileSource }),
        
        map = new ol.Map({
          target: divId,
          layers: [ tileLayer ],
          renderer: (Config.WEBGL_ENABLED) ? 'webgl' : undefined,
          view: new ol.View({
            projection: projection,
            center: [Config.width / 2, - Config.height / 2],
            zoom: 0,
            minResolution: 0.125
          })
        }),
        
        annotationLayer = new AnnotationLayer($('#' + divId).parent(), map, eventBroker);    
    
    map.getView().fitExtent([0, - Config.height, Config.width, 0], divSize);
    
    // Handlers for brightness/contrast user settings
    eventBroker.addHandler(Events.SET_BRIGHTNESS, function(value) {
      if(!isNaN(value) && (Math.abs(value) < Number.POSITIVE_INFINITY))
        tileLayer.setBrightness(value / 100);
    });
    
    eventBroker.addHandler(Events.SET_CONTRAST, function(value) {
      if(!isNaN(value) && (Math.abs(value) < Number.POSITIVE_INFINITY))
        tileLayer.setContrast(value / 100);
    });
    
    // Slightly ugly - but we need to wrap getCoordinateFromPixel so that the drawing canvas can use it
    _map = map;
  };
  
  Viewer.prototype.getCoordinateFromPixel = function(coord) {
    return _map.getCoordinateFromPixel(coord);
  };
  
  return Viewer;
  
});
