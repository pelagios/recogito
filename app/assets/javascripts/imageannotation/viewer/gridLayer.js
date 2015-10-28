define(['imageannotation/config'], function(Config) {

  var GridLayer = function(map, eventBroker) {
    var  size = [ Config.width, Config.height ],

         behaimNorth = new ol.source.Zoomify({
           url: '/recogito/static/images/grid-behaim-n/',
           size: size
         }),

         behaimSouth = new ol.source.Zoomify({
           url: '/recogito/static/images/grid-behaim-s/',
           size: size
         }),

         tileLayers = [
           false,
           new ol.layer.Tile({ source: behaimNorth }),
           new ol.layer.Tile({ source: behaimSouth })
         ],

         currentLayerIdx = 0,

         cycleLayer = function() {
           if (currentLayerIdx !== 0)
             map.removeLayer(tileLayers[currentLayerIdx]);

           currentLayerIdx = (currentLayerIdx + 1) % tileLayers.length;

           if (currentLayerIdx !== 0)
             map.addLayer(tileLayers[currentLayerIdx]);
         };

    jQuery(document).keypress(function(e) {
      var key = e.keyCode || e.which;
      if (key === 71) // Shift + G
        cycleLayer();
    });

  };

  return GridLayer;

});
