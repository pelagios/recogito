define(['georesolution/common',
        'georesolution/details/detailsMap',
        'georesolution/details/searchControl',
        'georesolution/annotationContext',
        'georesolution/tagList'], function(common, Map, SearchControl, AnnotationContext, TagList) {
  
  var DetailsView = function(eventBroker) {
    var map, 
        searchControl,
        currentAnnotation,
        
        element = jQuery(
          '<div id="annotation-details">' +
          '  <div id="clicktrap">' +
          '    <div id="details-popup">' +
          '      <div class="header">' +
          '        »<span class="toponym"></span>«' +
          '        <span class="header-icons">' + 
          '          <a class="prev icon" title="Skip to Previous Annotation">&#xf0d9;</a>' + 
          '          <a class="next icon" title="Skip to Next Annotation">&#xf0da;</a>' +
          '          <a class="exit icon" title="Close">&#xf00d;</a>' + 
          '        </span>' +
          '      </div>' + //  <!-- header -->
          
          '      <div class="body">' +
          '        <div class="controls">' +
          '          <p class="mapping-info auto"></p>' +
          '          <p class="mapping-info corrected"></p>' +
          '          <div class="statusbar">' +
          '            <div class="status verified" title="Verified"><span class="icon">&#xf14a;</span></div>' +        
          '            <div class="status not-verified" title="Not Verified"><span class="icon">&#xf059;</span></div>' +     
          '            <div class="status ignore" title="Ignore this toponym"><span class="icon">&#xf05e;</span></div>' + 
          '            <div class="status no-suitable-match" title="No suitable gazetteer match available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status ambiguous" title="Multiple possible gazetteer matches available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status multiple" title="Toponym refers to multiple places"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status not-identifiable" title="Not Identifiable"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status false-detection" title="False Detection"><span class="icon">&#xf057;</span></div>' +   
          '          </div>' +
          '          <p class="quote"></p>' +
          '          <div class="tags">' +
          '          </div>' +
          '        </div>' +
          
          '        <div id="details-map"></div>' +
          
          '        <div id="details-search"></div>' +
          
          '      </div>' + // <!-- body -->
          '    </div>' +
          '  </div>' +
          '</div>'
        ),
        
        /** Events **/
        Events = eventBroker.events,
        
        /** DOM element shorthands **/
        toponym = element.find('.toponym'),
        btnPrevious = element.find('.header-icons .prev'),
        btnNext = element.find('.header-icons .next'),
        btnExit = element.find('.header-icons .exit'),

        mappingInfoAuto = element.find('.mapping-info.auto'),
        mappingInfoCorrected = element.find('.mapping-info.corrected'),

        statusVerified = element.find('.status.verified'),
        statusNotVerified = element.find('.status.not-verified'),
        statusIgnore = element.find('.status.ignore'),
        statusNoSuitableMatch = element.find('.status.no-suitable-match'),
        statusAmbiguous = element.find('.status.ambiguous'),
        statusMultiple = element.find('.status.multiple'),
        statusNotIdentifiable = element.find('.status.not-identifiable'),
        statusFalseDetection = element.find('.status.false-detection'),
        
        tagList = new TagList(element.find('.tags')),
        
        contentPreview = element.find('.quote'),
        
        /** Status: value-to-button mapping **/
        statusButtons = {
          VERIFIED          : statusVerified,
          NOT_VERIFIED      : statusNotVerified,
          IGNORE            : statusIgnore,
          NO_SUITABLE_MATCH : statusNoSuitableMatch,
          AMBIGUOUS         : statusAmbiguous,
          MULTIPLE          : statusMultiple,
          NOT_IDENTIFYABLE  : statusNotIdentifiable,
          FALSE_DETECTION   : statusFalseDetection
        },
        
        /** Shorthand **/
        stopPropagation = function(e) { e.stopPropagation() },
        
        /** Helper to populate the 'mapping info paragraphs **/
        setMappingInfo = function(place, paragraph) {
          if (place) {
            var html = 
              '<a href="' + place.uri + '" target="_blank">' + place.title + '</a> ' +
              common.Utils.categoryTag(place.category) + '<br/>';
              
            if (place.description)
              html += '<em>' + place.description + '</em><br/>';
              
            if (place.names)
              html += place.names.slice(0, 8).join(', ') + '<br/>';
                              
            if (!place.coordinate)
              html += '<span class="icon no-coords ">&#xf041;</span>No coordinates for this place!</a>';
               
            paragraph.html(html);          
          } else {
            paragraph.html('-');
          }
        },
    
        /** Open the details view with a new annotation **/
        show = function(annotation, previous, next, autofit) {
          var activeStatusButton = statusButtons[annotation.status],
              context = new AnnotationContext(annotation),
              previousTail = (previous.length > 0) ? previous[previous.length - 1] : false;
              
          currentAnnotation = annotation;
          
          // Update browser URL bar
          window.location.hash = annotation.id;
          
          // Set toponym as search term
          searchControl.resetSearch(annotation.toponym, previousTail);
          
          // Populate the template
          toponym.html(annotation.toponym);
          setMappingInfo(annotation.place, mappingInfoAuto);
          setMappingInfo(annotation.place_fixed, mappingInfoCorrected);
          
          // Status bar
          for (status in statusButtons)
            statusButtons[status].removeClass('active');
            
          if (activeStatusButton)
            activeStatusButton.addClass('active');

          // Text/image preview        
          context.fetchContentPreview(function(preview) {
            contentPreview.html('...' + preview.pre + '<em>' + preview.toponym + '</em>' + preview.post + '...');
          });
          
          // Tag list
          tagList.show(annotation.tags);
          
          // Show popup
          element.show();
          
          // Refresh map
          map.clearAnnotations();
          map.refresh();
          map.addAnnotation(annotation, context);
          map.addSequence(annotation, previous, next);
          
          if (autofit)
            map.fitToAnnotations();
            
          map.showMarker(annotation);
                    
          searchControl.setMaxHeight(map.height());
          searchControl.focus();
        },
        
        /** Close the details view **/
        hide = function() {
          currentAnnotation = false;
          window.location.hash = '';
          contentPreview.html('');    
          map.clearSearchresults();
          map.clearAnnotations();
          element.hide();          
        },
        
        /** Stores a correction to the gazetteer URI **/
        correctGazetteerMapping = function(result) {
          if (confirm('Are you sure you want to correct the mapping to ' + result.title + '?')) {            
            currentAnnotation.place_fixed = {
              category : result.category,
              coordinate : result.coordinate,
              names : result.names,
              title : result.title,
              uri : result.uri
            }
            currentAnnotation.status = 'VERIFIED';
        
            eventBroker.fireEvent('updateAnnotation', currentAnnotation);        
            eventBroker.fireEvent('skipNext');
          }
        },
        
        /** Stores a status change **/
        changeStatus = function(newStatus) {
          if (currentAnnotation.status != newStatus) {
            currentAnnotation.status = newStatus;
            eventBroker.fireEvent('updateAnnotation', currentAnnotation);        
            eventBroker.fireEvent('skipNext');
          }
        };
       
    eventBroker.addHandler(Events.SHOW_ANNOTATION_DETAILS, function(e) { show(e.annotation, e.previous, e.next, e.autofit); });
    
    /** Create DOM elements **/        
    element.hide();
    jQuery(document.body).append(element);   
    
    map = new Map(document.getElementById('details-map')); // Must be instatiated after it's part of the DOM
    searchControl = new SearchControl(element.find('#details-search'), map);
    searchControl.on('selectSearchresult', correctGazetteerMapping);
    $(window).resize(function() { searchControl.setMaxHeight(map.height()); });
    
    /** Header events **/
    btnPrevious.click(function() { eventBroker.fireEvent('skipPrevious'); });
    btnNext.click(function() { eventBroker.fireEvent('skipNext'); });
    btnExit.click(hide);
    
    /** Map events **/
    map.on('selectSearchresult', correctGazetteerMapping);
    map.on('verify', function() { changeStatus('VERIFIED'); });
    map.on('findAlternatives', function() { search(currentAnnotation.toponym); });
    map.on('skipNext', function() { eventBroker.fireEvent('skipNext'); });
    
    /** Controls events **/
    jQuery.each(statusButtons, function(status, button) {
      button.click(function() { changeStatus(status); });
    });
    tagList.on('update', function(tags) {
      currentAnnotation.tags = tags;
      eventBroker.fireEvent('updateAnnotation', currentAnnotation);   
    });
  };
  
  return DetailsView;
  
});
