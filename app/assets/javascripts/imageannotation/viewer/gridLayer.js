define(['imageannotation/config'], function(Config) {

  var GridLayer = function(map, eventBroker) {
    var  tileSource = new ol.source.Zoomify({
           url: '/recogito/static/images/behaim-grid/',
           size: [ Config.width, Config.height ]
         }),

         tileLayer = new ol.layer.Tile({ source: tileSource });

    map.addLayer(tileLayer);
  };

  return GridLayer;

});
