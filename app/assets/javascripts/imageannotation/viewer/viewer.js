define(['imageannotation/config', 'imageannotation/viewer/annotationLayer'], function(Config, AnnotationLayer) {
  
  var _map;
  
  var Viewer = function(divId, eventBroker) {        
    var projection = new ol.proj.Projection({
          code: 'ZOOMIFY',
          units: 'pixels',
          extent: [0, 0, Config.width, Config.height]
        }),
        
        tileSource = new ol.source.Zoomify({
          url: Config.url,
          size: [ Config.width, Config.height ]
        });
        
    _map = new ol.Map({
             target: divId,
             layers: [ new ol.layer.Tile({ source: tileSource }) ],
             view: new ol.View({
               projection: projection,
               center: [Config.width / 2, - Config.height / 2],
               zoom: 0,
               minResolution: 0.5
             })
           });
    
    
    var annotationLayer = new AnnotationLayer($('#' + divId).parent(), this, eventBroker);    
    _map.addLayer(annotationLayer.getLayer());
    _map.getView().fitExtent([0, - Config.height, Config.width, 0], _map.getSize());
  };
  
  /** Helper function to compute the viewport bounds of an annotation **/
  Viewer.prototype.toViewportBounds = function(annotation, opt_buffer) {
    var buffer = (opt_buffer) ? opt_buffer : 0;    
    var resolution = _map.getView().getResolution();
    var geom = annotation.shapes[0].geometry;
    
    var anchor = _map.getPixelFromCoordinate([ geom.x, - geom.y ]);
    var angle = geom.a - _map.getView().getRotation();
    
    var a = { x: anchor[0], y: anchor[1] },
        b = {
          x: a.x + Math.cos(angle) * geom.l / resolution,
          y: a.y - Math.sin(angle) * geom.l / resolution
        },
        c = {
          x: b.x - geom.h / resolution * Math.sin(angle),
          y: b.y - geom.h / resolution * Math.cos(angle)
        },
        d = {
          x: a.x - geom.h / resolution * Math.sin(angle),
          y: a.y - geom.h / resolution * Math.cos(angle)      
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
    
    return bounds;
  };
  
  Viewer.prototype.on = function(event, callback) {
    _map.on(event, callback);
  };
  
  Viewer.prototype.getCoordinateFromPixel = function(px) {
    return _map.getCoordinateFromPixel(px);
  };
  
  return Viewer;
  
});
