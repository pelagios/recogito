define(['config', 'imageannotation/annotation-layer'], function(config, AnnotationLayer) {
  
  var annotationLayer;
  
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
    
    this.map = new ol.Map({
      target: mapDiv,
      layers: [ new ol.layer.Tile({ source: tileSource }) ],
      view: new ol.View({
        projection: projection,
        center: [config.width / 2, - config.height / 2],
        zoom: 0,
        minResolution: 0.5
      })
    });
    
    this.map.getView().fitExtent([0, - config.height, config.width, 0], this.map.getSize());
    
    annotationLayer = new AnnotationLayer(this);
  }
  
  OpenLayersMap.prototype.getCoordinateFromPixel = function(px) {
    return this.map.getCoordinateFromPixel(px);
  }
  
  // TODO need to revert this!
  OpenLayersMap.prototype.addLayer = function(layer) {
    this.map.addLayer(layer);
  }
  
  OpenLayersMap.prototype.getResolution = function() {
    return this.map.getView().getResolution();
  }
  
  OpenLayersMap.prototype.on = function(event, callback) {
    this.map.on(event, callback);
  }
  
  OpenLayersMap.prototype.toViewportCoordinates = function(annotation, opt_buffer) {
    var buffer = (opt_buffer) ? opt_buffer : 0;    
    var resolution = this.map.getView().getResolution();
    var geom = annotation.shapes[0].geometry;
    
    var anchor = this.map.getPixelFromCoordinate([ geom.x, - geom.y ]);
    
    var a = { x: anchor[0], y: anchor[1] },
        b = {
          x: a.x + Math.cos(geom.a) * geom.l / resolution,
          y: a.y - Math.sin(geom.a) * geom.l / resolution
        },
        c = {
          x: b.x - geom.h / resolution * Math.sin(geom.a),
          y: b.y - geom.h / resolution * Math.cos(geom.a)
        },
        d = {
          x: a.x - geom.h / resolution * Math.sin(geom.a),
          y: a.y - geom.h / resolution * Math.cos(geom.a)      
        };
    var top = Math.min(a.y, b.y, c.y, d.y),
        right = Math.max(a.x, b.x, c.x, d.x),
        bottom = Math.max(a.y, b.y, c.y, d.y),
        left = Math.min(a.x, b.x, c.x, d.x);

    var bounds = {
      left: Math.round(left) - buffer,
      top: Math.round(top) - buffer,
      width: Math.round(right - left) + 2 * buffer,
      height: Math.round(bottom - top) + 2 * buffer
    };
    
    console.log(bounds);
    return bounds;
  }
  
  OpenLayersMap.prototype.addAnnotations = function(annotation) {      
    annotationLayer.addAnnotations(annotation);  
  }
  
  OpenLayersMap.prototype.removeAnnotation = function(id) {
    annotationLayer.removeAnnotation(id);
  }
  
  return OpenLayersMap;
  
});
