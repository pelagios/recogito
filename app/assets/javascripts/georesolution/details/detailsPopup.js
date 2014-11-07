define(['georesolution/common', 'georesolution/details/detailsMap'], function(common, Map) {
  
  var DetailsView = function(eventBroker) {
    var map,
        currentAnnotation,
        element = jQuery(
          '<div id="annotation-details">' +
          '  <div id="clicktrap">' +
          '    <div id="details-popup">' +
          '      <div class="header">' +
          '        »<span class="toponym"></span>«' +
          '        <span class="header-icons">' + 
          '          <a class="prev icon" title="Skip to Previous Annotation">&#xf0d9;</a>' + 
          '          <a class="next icon" title="Skipt to Next Annotation">&#xf0da;</a>' +
          '          <a class="exit icon" title="Close">&#xf00d;</a>' + 
          '        </span>' +
          '      </div> <!-- header -->' +
          
          '      <div class="body">' +
          '        <div class="controls">' +
          '          <p class="mapping-info auto"></p>' +
          '          <p class="mapping-info corrected"></p>' +
          '          <div class="statusbar">' +
          '            <div class="status verified" title="Verified"><span class="icon">&#xf14a;</span></div>' +        
          '            <div class="status not-verified" title="Not Verified"><span class="icon">&#xf059;</span></div>' +     
          '            <div class="status false-detection" title="False Detection"><span class="icon">&#xf057;</span></div>' +   
          '            <div class="status ignore" title="Ignore this toponym"><span class="icon">&#xf05e;</span></div>' + 
          '            <div class="status no-suitable-match" title="No suitable gazetteer match available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status ambiguous" title="Multiple possible gazetteer matches available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status multiple" title="Toponym refers to multiple places"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status not-identifiable" title="Not Identifiable"><span class="icon">&#xf024;</span></div>' + 
          '          </div>' +
          '          <p class="content-preview"></p>' +
          '        </div>' +
          
          '        <div id="details-map">' +
          '          <div id="details-search">' +
          '            <div id="search-input">' +
          '              <input>' +
          '              <div class="btn search"><span class="icon">&#xf002;</span></div>' + 
          '              <div class="btn labeled fuzzy-search"><span class="icon">&#xf002;</span> Fuzzy</div>' + 
          '              <div class="btn labeled zoom-all" title="Zoom to All Results"><span class="icon">&#xf0b2;</span> All</div>' + 
          '              <div class="btn labeled clear" title="Clear Search Results"><span class="icon">&#xf05e;</span> Clear</div>' + 
          '            </div>' +
          '            <div id="search-results"></div>' +
          '          </div>' +
          '        </div>' +
          '      </div> <!-- body -->' +
          '    </div>' +
          '  </div>' +
          '</div>'
        ),
        
        /** Event map **/
        Events = eventBroker.events,
        
        /** DOM element shorthands **/
        toponym = element.find('.toponym'),
        btnPrevious = element.find('.header-icons .prev'),
        btnNext = element.find('.header-icons .next'),
        btnExit = element.find('.header-icons .exit'),

        searchContainer = element.find('#details-search'),
        searchInput = element.find('#details-search input'),
        btnSearch = element.find('#search-input .search'),
        btnFuzzySearch = element.find('#search-input .fuzzy-search'),
        btnZoomAll = element.find('#search-input .zoom-all'),
        btnClearSearch = element.find('#search-input .clear'),
        
        mappingInfoAuto = element.find('.mapping-info.auto'),
        mappingInfoCorrected = element.find('.mapping-info.corrected'),

        statusVerified = element.find('.status.verified'),
        statusNotVerified = element.find('.status.not-verified'),
        statusFalseDetection = element.find('.status.false-detection'),
        statusIgnore = element.find('.status.ignore'),
        statusNoSuitableMatch = element.find('.status.no-suitable-match'),
        statusAmbiguous = element.find('.status.ambiguous'),
        statusMultiple = element.find('.status.multiple'),
        statusNotIdentifiable = element.find('.status.not-identifiable'),
        
        contentPreview = element.find('.content-preview'),
        
        /** Status: value-to-button mapping **/
        statusButtons = {
          VERIFIED          : statusVerified,
          NOT_VERIFIED      : statusNotVerified,
          FALSE_DETECTION   : statusFalseDetection,
          IGNORE            : statusIgnore,
          NO_SUITABLE_MATCH : statusNoSuitableMatch,
          AMBIGUOUS         : statusAmbiguous,
          MULTIPLE          : statusMultiple,
          NOT_IDENTIFYABLE  : statusNotIdentifiable
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
        
        resetSearch = function(presetQuery) {
          searchInput.val(presetQuery);
          btnZoomAll.hide();
          btnClearSearch.hide();
          map.clearSearchresults();
        },
        
        /** Load the text (or, TODO, image) preview **/
        loadContentPreview = function(id) {
          jQuery.getJSON('api/annotations/' + id, function(a) {
            var startIdx, endIdx, pre, post;
            
            if (a.context) {
              startIdx = a.context.indexOf(a.toponym);
              endIdx = startIdx + a.toponym.length;
              
              if (startIdx > -1 && endIdx <= a.context.length) {
                pre = a.context.substring(0, startIdx);
                post = a.context.substring(endIdx);
              
                contentPreview.html('...' + pre + '<em>' + a.toponym + '</em>' + post + '...');
              }
            }    
          });
        },
    
        /** Open the details view with a new annotation **/
        show = function(annotation, previous, next, autofit) {
          var activeStatusButton = statusButtons[annotation.status];
          currentAnnotation = annotation;
          
          // Update browser URL bar
          window.location.hash = annotation.id;
          
          // Set toponym as search term
          resetSearch(annotation.toponym);
          
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
          loadContentPreview(annotation.id);
          
          // Show popup
          element.show();
          
          // Refresh map
          map.clearAnnotations();
          map.refresh();
          map.addAnnotation(annotation);
          map.addSequence(annotation, previous, next);
          map.showMarker(annotation);
          
          if (autofit)
            map.fitToAnnotations();
            
          searchInput.focus();
        },
        
        /** Close the details view **/
        hide = function() {
          currentAnnotation = false;
          window.location.hash = '';
          map.clearSearchresults();
          map.clearAnnotations();
          element.hide();          
        },
        
        /** Gazetteer search **/
        search = function(query) {
          map.clearSearchresults();
          jQuery.getJSON('api/search/place?query=' + query, function(response) {
            map.showSearchresults(response);
            btnZoomAll.show();
            btnClearSearch.show();
          });
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
    map = new Map(document.getElementById('details-map'), document.getElementById('search-results'));     
    
    /** Header events **/
    btnPrevious.click(function() { eventBroker.fireEvent('skipPrevious'); });
    btnNext.click(function() { eventBroker.fireEvent('skipNext'); });
    btnExit.click(hide);
    
    /** Map events **/
    map.on('selectSearchresult', correctGazetteerMapping);
    searchInput.keypress(function(e) {
      if (e.which == 13)
        search(e.target.value.toLowerCase());
    });
    searchContainer.dblclick(stopPropagation);
    searchContainer.mousemove(stopPropagation);
    
    btnSearch.click(function() { search(searchInput.val().toLowerCase()); });
    btnFuzzySearch.click(function() { search(searchInput.val().toLowerCase() +  '~'); });
    btnZoomAll.click(function() { map.fitToSearchresults(); });
      //  btnClearSearch = element.find('#search-input .clear'),
    
    /** Controls events **/
    jQuery.each(statusButtons, function(status, button) {
      button.click(function() { changeStatus(status); });
    });
  };
  
  return DetailsView;
  
});
