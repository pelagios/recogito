define(['georesolution/common',
        'georesolution/details/detailsMap',
        'georesolution/details/searchControl',
        'georesolution/annotationContext',
        'georesolution/tagList'], function(common, Map, SearchControl, AnnotationContext, TagList) {
  
  var DetailsPopup = function(eventBroker) {
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
          
          '          <div class="quote">' +
          '            <span class="quote-text"></span>' +
          '            <p class="quote-link"><a target="_blank">Switch to Document</a></p>' +
          '          </div>' +
          
          '          <div class="tags"></div>' +
          
          '          <div class="comment">' +
          '            <textarea></textarea>' + 
          '            <button class="button dark save-comment" type="button">Save Comment</button>' +
          '          </div>' +
          '        </div>' +
          
          '        <div id="details-map"></div>' +
          
          '        <div id="details-search"></div>' +
          
          '      </div>' + // <!-- body -->
          '    </div>' +
          '  </div>' +
          '</div>'
        ),
        
        /** HTML template for the suggestions popup **/
        suggestionsTemplate = 
          '<div class="suggestions-popup">' +
          '  <div class="header">SUGGESTION' +
          '    <span class="exit icon" title="Close">&#xf00d;</span>' + 
          '  </div>' +
          '  <div class="body">' +
          '    <p>' +
          '      »<em class="toponym"></em>« was also mapped to:' +
          '    </p>' +
          '    <table class="suggestions">' + // <!-- add place info here -->
          '    </table>' +
          '  </div>' +
          '</div>',
        
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
        
        quotePreview = element.find('.quote .quote-text'),
        quoteLink = element.find('.quote .quote-link a'),
        
        commentField = element.find('.comment textarea'),
        btnStoreComment = element.find('.save-comment'),
        
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
        
        /** Opens a popup dialog suggesting previous toponym->gazetteer mappings **/
        showSuggestionDialog = function(toponym, suggestions) {
          var popup = jQuery(suggestionsTemplate),
              placeList = popup.find('.suggestions');

          popup.find('.toponym').html(toponym);
          jQuery.each(suggestions, function(idx, place) {
            var html = 
              '<tr>' +
              '  <td class="place">' +
              '    <strong>' + place.title + '</strong>' + common.Utils.categoryTag(place.category) +
              '    <br/><small>' + place.names.slice(0, 8).join(', ') +
              '    <br/>' + place.description + '</small>' +
              '  </td>' +
              '  <td class="confirm">' +
              '    <div class="btn labeled" data-uri="' + place.uri + '"><span class="icon">&#xf14a;</span> Confirm</div>' +
              '  </td>' +
              '</tr>';
              
            placeList.html(html);
          });
          
          popup.find('.exit').click(destroySuggestionsDialog);          
          popup.find('.btn').click(function(e) {
            var uri = jQuery(e.target).data('uri'),
                suggestion = jQuery.grep(suggestions, function(s) {
                  return s.uri === uri;
                });
                
            if (suggestion && suggestion.length > 0) {
              correctGazetteerMapping(suggestion[0]);
            }
          });
          
          jQuery(document.body).append(popup);  
          popup.draggable({ handle: popup.find('.header') });
        },
        
        /** Destroys the popup dialog **/
        destroySuggestionsDialog = function() {
          jQuery('.suggestions-popup').remove();
        },
    
        /** Open the details view with a new annotation **/
        show = function(annotation, previous, next, suggestions, autofit) {
          var activeStatusButton = statusButtons[annotation.status],
              context = new AnnotationContext(annotation),
              previousTail = (previous.length > 0) ? previous[previous.length - 1] : false;
              
          currentAnnotation = annotation;

          // If the user switches annotations without closing the dialog          
          destroySuggestionsDialog();
          
          // Update browser URL bar
          window.location.hash = annotation.id;
          
          // Set toponym as search term
          searchControl.resetSearch(annotation.toponym, previousTail, true);
          
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
          quotePreview.empty();    
          context.fetchContentPreview(function(preview) {
            quotePreview.html('...' + preview.pre + '<em>' + preview.toponym + '</em>' + preview.post + '...');
          });
          quoteLink.attr('href', '/recogito/annotation/' + annotation.id);
          
          // Tag list
          tagList.show(annotation.tags);
          
          // Comment field
          if (annotation.comment) {
            commentField.val(annotation.comment);
          } else {
            commentField.val('');
          }
          
          // Show popup
          element.show();
          
          // Refresh map
          map.clearAnnotations();
          map.refresh();
          map.addAnnotation(annotation, context);
          map.setSequence(annotation, previous, next);
          
          if (autofit)
            map.fitToAnnotations();
            
          map.showPopup(annotation);
                    
          searchControl.setMaxHeight(map.height());
          
          if (suggestions && suggestions.length > 0) {
            showSuggestionDialog(annotation.toponym, suggestions);
          }
        },
        
        /** Close the details view **/
        hide = function() {
          currentAnnotation = false;
          window.location.hash = '';
          destroySuggestionsDialog();
          quotePreview.empty();    
          map.clearSearchresults();
          map.clearAnnotations();
          element.hide();          
        },
        
        /** Stores a correction to the gazetteer URI **/
        correctGazetteerMapping = function(result) {                    
          if (confirm('Are you sure you want to correct the mapping to ' + result.title + '?')) {  
            var annotation = currentAnnotation;
            
            annotation.place_fixed = {
              category : result.category,
              coordinate : result.coordinate,
              names : result.names,
              title : result.title,
              uri : result.uri
            }
            annotation.status = 'VERIFIED';
            
            eventBroker.fireEvent('skipNext');
            eventBroker.fireEvent('updateAnnotation', annotation);        
          }
        },
        
        /** Stores a status change **/
        changeStatus = function(newStatus) {
          if (currentAnnotation.status != newStatus) {
            currentAnnotation.status = newStatus;
            eventBroker.fireEvent('updateAnnotation', currentAnnotation);        
            eventBroker.fireEvent('skipNext');
          }
        },
        
        /** Stores a comment update **/
        storeComment = function(comment) {
          var comment = commentField.val().trim();
          if (currentAnnotation.comment != comment) {
            currentAnnotation.comment = comment;
            eventBroker.fireEvent('updateAnnotation', currentAnnotation);
          }
        };
       
    eventBroker.addHandler(Events.SHOW_ANNOTATION_DETAILS, function(e) { show(e.annotation, e.previous, e.next, e.suggestions, e.autofit); });
    
    /** Create DOM elements **/        
    element.hide();
    jQuery(document.body).append(element);   
    
    map = new Map(document.getElementById('details-map')); // Must be instatiated after it's part of the DOM
    searchControl = new SearchControl(element.find('#details-search'), map);
    searchControl.on('selectSearchresult', correctGazetteerMapping);
    $(window).resize(function() { searchControl.setMaxHeight(map.height()); });
    
    jQuery(document.body).on('keyup', function(e) {
      var target = e.target.tagName;
      
      if (currentAnnotation) {
        if (target !== 'INPUT' && target !== 'TEXTAREA') {
          if (e.which === 37) {
            eventBroker.fireEvent('skipPrevious');
          } else if (e.which === 39) {
            eventBroker.fireEvent('skipNext');
          } 
        }
      }
    });
    
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
    btnStoreComment.click(function() {
      storeComment(commentField.val());
    });
  };
  
  return DetailsPopup;
  
});
