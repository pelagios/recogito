define(['georesolution/common', 'common/map'], function(common, MapBase) {

  var OverviewMap = function(mapDiv) {
 
    var self = this,
    
        emphasizedMarkers = [],
        
        MARKER_SIZE = 5;
    
        resizeMarker = function(annotation, size) {
          var marker = self.getMarkerForAnnotation(annotation);
          if (marker) {
            if (marker.setRadius)
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
      var marker = resizeMarker(annotation, MARKER_SIZE * 2);
      
      // Clear all other markers that may still be emphasized
      jQuery.each(emphasizedMarkers, function(idx, marker) {
        if (marker.setRadius)
          marker.setRadius(MARKER_SIZE);
      });
      emphasizedMarkers = [];
      
      if (marker)
        emphasizedMarkers.push(marker);
    }

    this.deemphasizePlace = function(annotation) {
      resizeMarker(annotation, MARKER_SIZE);
    }
    
    MapBase.apply(this, [ mapDiv ]);
  }
  OverviewMap.prototype = Object.create(MapBase.prototype);
  
  return OverviewMap;

});
