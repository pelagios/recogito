require(['common/map', 'publicmap/sidepanel', 'publicmap/loadindicator', 'georesolution/annotationContext'], function(Map, SidePanel, LoadIndicator, AnnotationContext) {
  
  var relatedLinks = jQuery.grep(jQuery('link'), function(el) { return jQuery(el).attr('rel') == 'related'; }),
  
      dataURL = jQuery(relatedLinks[0]).attr('href'),
      
      sidePanel = new SidePanel(document.getElementById('side-panel')),
      
      loadIndicator = new LoadIndicator(),
      
      /** Handler function that gets fired when user clicks a marker **/
      popupFn = function(place, annotationsWithContext) {
        var html =
                 '<div class="map-popup">' +
                 '  <div class="title">' + place.title + '</div>' +
                 '  <div class="names">' + place.names.join(', ') + '</div>' +
                 '  <div class="description">' + place.description + '</div>' +
                 '  <div class="content-preview">' +
                 '    Appears ' + annotationsWithContext.length + ' times in this document.' +
                 '    <span class="quote"></span>' +
                 '  </div>' +
                 '  <div class="footer">' +
                 '    <span class="prev icon">' +
                 '      <span class="prev-place">&#xf100;</span>' +
                 '      <span class="prev-quote">&#xf104;</span>' +
                 '    </span>' +
                 '    <span class="next icon">' + 
                 '      <span class="next-quote">&#xf105;</span>' +
                 '      <span class="next-place">&#xf101;</span>' +
                 '    </span>' +
                 '  </div>' +
                 '</div>',
                    
          // TODO just a hack
          context = new AnnotationContext(annotationsWithContext[0][0]);
                  
        context.fetchContentPreview(function(snippet) {
          jQuery('.map-popup .content-preview .quote').html('... ' + snippet.pre + '<em>' + snippet.toponym + '</em>' + snippet.post + ' ...');
        });
        
        return html;
      },
      
      map = new Map(document.getElementById('map'), popupFn, undefined, 'topright');
    
  // TODO temporary hack
  loadIndicator.show();
  $.getJSON(dataURL, function(data) {
    if (data.annotations) {
      jQuery.each(data.annotations, function(idx, a) {
        map.addAnnotation(a);
      });
    } else {
      jQuery.each(data.parts, function(idx, part) {
        jQuery.each(part.annotations, function(idx, a) {
          map.addAnnotation(a);
        });
      });
    }
    
    map.fitToAnnotations();
    loadIndicator.hide();
  });
  
});

