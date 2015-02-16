require(['common/map', 
         'publicmap/sidePanel',
         'publicmap/mapPopup',
         'publicmap/loadIndicator'], function(Map, SidePanel, Popup, LoadIndicator) {
  
  var mapEl = jQuery('#map'),
  
      colorScale = ['#edf8e9', '#c7e9c0', '#a1d99b', '#74c476', '#31a354', '#006d2c'],
      
      maxMarkerWeight = false,
      
      minDotSize = 3,
      
      maxDotSize = 10,
  
      relatedLinks = jQuery.grep(jQuery('link'), function(el) { return jQuery(el).attr('rel') == 'related'; }),
  
      dataURL = jQuery(relatedLinks[0]).attr('href'),
      
      sidePanel = new SidePanel(document.getElementById('side-panel')),
      
      loadIndicator = new LoadIndicator(),
      
      /** Function for styling map overlays **/
      styleFn = function(annotations) {
        var colIdx = colorScale.length - 1,
            strokeColor = colorScale[colIdx];
            fillColor = colorScale[colIdx - 1];
            radius = 5,
            place = (annotations[0].place_fixed) ? annotations[0].place_fixed : annotations[0].place;
        
        if (maxMarkerWeight)
          radius = (maxDotSize - minDotSize) * annotations.length / maxMarkerWeight + minDotSize;
        
        if (place.geometry)
          return { color: strokeColor, fillColor: fillColor, opacity: 1, weight: 1.5, fillOpacity: 0.5, radius: radius };
        else
          return { color: strokeColor, fillColor: fillColor, opacity: 1, weight: 1.5, fillOpacity: 1, radius: radius };
      },
      
      map = new Map(document.getElementById('map'), Popup, styleFn, false, 'topright'),
      
      loadData = function() {
        loadIndicator.show();
        $.getJSON(dataURL, function(data) {
          var annotations = [],
          
              showPrevAnnotation = function(annotation) {
                var len = annotations.length,
                    currentIdx = jQuery.inArray(annotation, annotations),
                    prevIdx = (currentIdx - 1 + len) % len;
                
                map.showPopup(annotations[prevIdx]);
              }, 
              
              showNextAnnotation = function(annotation) {
                var currentIdx = jQuery.inArray(annotation, annotations),
                    nextIdx = (currentIdx +  1) % annotations.length;
                    
                map.showPopup(annotations[nextIdx]);
              };
          
          // Add annotations to map
          if (data.annotations) {
            jQuery.each(data.annotations, function(idx, a) {
              if (map.addAnnotation(a))
                annotations.push(a); // If the annotation had a proper location, push it to the array
            });
          } else {
            jQuery.each(data.parts, function(idx, part) {
              jQuery.merge(annotations, part.annotations);
              jQuery.each(part.annotations, function(idx, a) {
                if (map.addAnnotation(a))
                 annotations.push(a); // If the annotation had a proper location, push it to the array
              });
            });
          }
          
          // Register prev/next annotation event handlers
          Popup.on('nextAnnotation', showNextAnnotation);
          Popup.on('prevAnnotation', showPrevAnnotation);
          
          // Render map & cancel load indicator    
          map.fitToAnnotations();
          maxMarkerWeight = map.getMaxMarkerWeight();
          map.refreshStyles();
          loadIndicator.hide();
        });
      };
  
  loadData();
  
});

