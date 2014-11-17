define(['georesolution/common', 'common/map'], function(common, MapBase) {

  var OverviewMap = function(mapDiv) {
 
    var self = this,
    
        emphasizedMarkers = [],
    
        setMarkerSize = function(annotation, size) {
          var marker = self.getMarkerForAnnotation(annotation);
          if (marker) {
            marker.setRadius(size);
            marker.bringToFront();
          }
        };
 
    /****                ****/
    /**                    **/
    /** Privileged methods **/
    /**                    **/
    /****                ****/
    
    this.emphasizePlace = function(annotation) {
      // Clear all other markers that may still be emphasized
      jQuery.each(emphasizedMarkers, function(idx, marker) {
        marker.setRadius(self.Styles.VERIFIED.radius);
      });
      emphasizedMarkers = [];
      
      setMarkerSize(annotation, self.Styles.VERIFIED.radius * 2);
    }

    this.deemphasizePlace = function(annotation) {
      setMarkerSize(annotation, self.Styles.VERIFIED.radius);
    }
    
    MapBase.apply(this, [ mapDiv ]);
  }
  OverviewMap.prototype = Object.create(MapBase.prototype);
  
  return OverviewMap;

});
