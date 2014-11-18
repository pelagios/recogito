define(['georesolution/common', 'common/map'], function(common, MapBase) {

  var OverviewMap = function(mapDiv) {
 
    var self = this,
    
        emphasizedMarkers = [],
    
        setMarkerSize = function(annotation, size) {
          var marker = self.getMarkerForAnnotation(annotation);
          if (marker) {
            marker.setRadius(size);
            marker.bringToFront();
            return marker;
          }
        };
 
    /****                ****/
    /**                    **/
    /** Privileged methods **/
    /**                    **/
    /****                ****/
    
    this.emphasizePlace = function(annotation) {
      var marker = setMarkerSize(annotation, self.Styles.VERIFIED.radius * 2);
      
      // Clear all other markers that may still be emphasized
      jQuery.each(emphasizedMarkers, function(idx, marker) {
        marker.setRadius(self.Styles.VERIFIED.radius);
      });
      emphasizedMarkers = [];
      
      if (marker)
        emphasizedMarkers.push(marker);
    }

    this.deemphasizePlace = function(annotation) {
      setMarkerSize(annotation, self.Styles.VERIFIED.radius);
    }
    
    MapBase.apply(this, [ mapDiv ]);
  }
  OverviewMap.prototype = Object.create(MapBase.prototype);
  
  return OverviewMap;

});
