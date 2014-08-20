define(['config', 'annotation-layer'], function(config, AnnotationLayer) {
  
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
    
    annotationLayer = new AnnotationLayer(map);
  }
  
  OpenLayersMap.prototype.getCoordinateFromPixel = function(px) {
    return map.getCoordinateFromPixel(px);
  }
  
  OpenLayersMap.prototype.addAnnotations = function(annotation) {      
    annotationLayer.addAnnotations(annotation);  
  }
  
  return OpenLayersMap;
  
});
