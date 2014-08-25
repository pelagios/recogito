define(['config', 'annotation/image/annotation-layer'], function(config, AnnotationLayer) {
  
  var map, annotationLayer;
  
  var OpenLayersMap = function(mapDiv) {

    var projection = new ol.proj.Projection({
      code: 'ZOOMIFY',
      units: 'pixels',
      extent: [0, 0, config.width, config.height]
    });

    var tileSource = new ol.source.Zoomify({
      url: config.url,
      size: [ config.width, config.height ]
    });
    
    map = new ol.Map({
      target: mapDiv,
      layers: [ new ol.layer.Tile({ source: tileSource }) ],
      view: new ol.View({
        projection: projection,
        center: [config.width / 2, - config.height / 2],
        zoom: 0
      })
    });
    
    annotationLayer = new AnnotationLayer(this);
  }
  
  OpenLayersMap.prototype.getCoordinateFromPixel = function(px) {
    return map.getCoordinateFromPixel(px);
  }
  
  // TODO need to revert this!
  OpenLayersMap.prototype.addLayer = function(layer) {
    map.addLayer(layer);
  }
  
  OpenLayersMap.prototype.getResolution = function() {
    return map.getView().getResolution();
  }
  
  OpenLayersMap.prototype.on = function(event, callback) {
    map.on(event, callback);
  }
  
  OpenLayersMap.prototype.toViewportCoordinates = function(bounds, opt_buffer) {
    var buffer = (opt_buffer) ? opt_buffer : 0,
        topLeft = map.getPixelFromCoordinate([ bounds.left, - bounds.top ]),
        bottomRight = map.getPixelFromCoordinate([ bounds.left + bounds.width, bounds.height - bounds.top ]);
    
    return {
      left: Math.round(topLeft[0] - buffer),
      top: Math.round(topLeft[1] - buffer),
      width: Math.round(bottomRight[0] - topLeft[0]) + 2 * buffer,
      height: Math.round(bottomRight[1] - topLeft[1]) + 2 * buffer
    };
  }
  
  OpenLayersMap.prototype.addAnnotations = function(annotation) {      
    annotationLayer.addAnnotations(annotation);  
  }
  
  OpenLayersMap.prototype.removeAnnotation = function(id) {
    annotationLayer.removeAnnotation(id);
  }
  
  return OpenLayersMap;
  
});
