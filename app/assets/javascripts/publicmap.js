require(['common/annotationContext',
         'common/formatting', 
         'common/map', 
         'publicmap/sidePanel',
         'publicmap/loadIndicator'], function(AnnotationContext, Formatting, Map, SidePanel, LoadIndicator) {
  
  var mapEl = jQuery('#map'),
  
      relatedLinks = jQuery.grep(jQuery('link'), function(el) { return jQuery(el).attr('rel') == 'related'; }),
  
      dataURL = jQuery(relatedLinks[0]).attr('href'),
      
      sidePanel = new SidePanel(document.getElementById('side-panel')),
      
      loadIndicator = new LoadIndicator(),
      
      /** Handler function that gets fired when user clicks a marker **/
      popupFn = function(place, annotationsWithContext) {
        var html = jQuery('<div class="map-popup">' +
                          '  <div class="title">' + place.title + ' ' + Formatting.categoryTag(place.category) + '</div>' +
                          '  <div class="names">' + place.names.slice(0, 8).join(', ') + '</div>' + 
                          '  <div class="gazetteer-link"><a href="' + place.uri + '" target="_blank">' + Formatting.formatGazetteerURI(place.uri) +'</a></div>' +
                          '  <div class="content-preview">' +
                          '    <span class="quote"></span>' +
                          '  </div>' +
                          '  <div class="footer">' +
                          '    <span class="prev icon">' +
                          '      <span class="prev-place" title="Previous place in document">&#xf100;</span>' +
                          '      <span class="prev-quote" title="Previous reference to ' + place.title + '">&#xf104;</span>' +
                          '    </span>' +
                          '    <span class="progress"></span>' +
                          '    <span class="next icon">' + 
                          '      <span class="next-quote" title="Next reference to ' + place.title + '">&#xf105;</span>' +
                          '      <span class="next-place" title="Next place in document">&#xf101;</span>' +
                          '    </span>' +
                          '  </div>' +
                          '</div>'),
                 
            /** Progress indication footer field **/
            progress = html.find('.progress'),
            
            /** Footer buttons **/
            btnPrevQuote = html.find('.prev-quote'),
            btnNextQuote = html.find('.next-quote'),
                 
            /** Current quote 'pointer' **/
            currentQuote = 0,
            
            updateProgress = function() {
              progress.html((currentQuote + 1) + '/' + annotationsWithContext.length + ' References');
              
              if (currentQuote === 0)
                btnPrevQuote.addClass('disabled');     
              else 
                btnPrevQuote.removeClass('disabled');
              
              if (currentQuote === annotationsWithContext.length - 1)
                btnNextQuote.addClass('disabled');
              else
                btnNextQuote.removeClass('disabled');
            };
                        
        // Init 'progress indicator' and display first quote in list
        updateProgress();
        displayQuote(annotationsWithContext[currentQuote], html);
             
        btnPrevQuote.click(function() {
          if (currentQuote > 0) {
            currentQuote -= 1;
            displayQuote(annotationsWithContext[currentQuote], html);
            updateProgress();
          }
        });
        
        btnNextQuote.click(function() {
          if (currentQuote < annotationsWithContext.length - 1) {
            currentQuote += 1;
            displayQuote(annotationsWithContext[currentQuote], html);
            updateProgress();
          }
        });
        
        return html[0];
      },
      
      displayQuote  = function(annotationWithContext, el) {   
        var context = annotationWithContext[1];
        
        if (!context) {
          context = new AnnotationContext(annotationWithContext[0]);
          annotationWithContext[1] = context;
        }
                           
        context.fetchContentPreview(function(snippet) {
          var html;
          
          if (snippet.pre) 
            html = '... '  + snippet.pre + '<em>';
          else
            html = '&raquo;';
            
          html += snippet.toponym;
          
          if (snippet.post)
            html += '</em>' + snippet.post + ' ...';
          else
            html += '&laquo;';
          
          el.find('.quote').html(html);
        });
      },
      
      map = new Map(document.getElementById('map'), popupFn, undefined, 'topright'),
      
      loadData = function() {
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
      };
  
  loadData();
  
});

