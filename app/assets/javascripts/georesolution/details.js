define(['georesolution/common', 'georesolution/details/detailsMap'], function(common, Map) {

  /**
   * A popup showing all details about a single annotation, with extra functionality
   * to make manual corrections to it.
   * 
   * Emits the following events:
   * 'update' .............. when a correction is saved 
   * 
   * @param {Object} annotation the annotation
   * @param {Array.<Object>} prev_annotations previous annotations in the list (if any)
   * @param {Array.<Object>} next_annotations next annotations in the list (if any)
   */
  var DetailsPopup = function(annotation, prev_annotations, next_annotations, opt_basemap) {
    // Inheritance - not the nicest pattern but works for our case
    common.HasEvents.call(this);
    
    window.location.hash = annotation.id;
    
    var self = this,
        template = 
          '<div class="clicktrap">' +
          '  <div class="popup details">' +
          '    <div class="popup-header">' +
          '      »<span class="details-header-toponym"></span>«' +
          '      <span class="details-header-source">  <span class="details-header-source-label"></span></span>' + 
          '      <span class="popup-header-icons">' + 
          '        <a class="details-skip-prev icon" title="Skip to Previous Toponym">&#xf0d9;</a>' + 
          '        <a class="details-skip-next icon" title="Skipt to Next Toponym">&#xf0da;</a>' +
          '        <a class="popup-exit" title="Close">&#xf00d;</a>' + 
          '      </span>' +
          '    </div>' +
          '    <div class="popup-content">' +
          '      <div id="details-content-topsection">' +
          '        <div id="details-map"></div>' +
          '        <div id="details-content-search">' +
          '          <div id="details-content-search-suggestions">ALTERNATIVE SUGGESTIONS</div>' +
          '          <div id="details-content-search-textsearch">' + 
          '            <span class="icon">&#xf002;</span>&nbsp;&nbsp;<input class="details-content-search-input">' + 
          '          </div>' +
          '        </div>' +
          '        <div id="details-content-placeinfo">' +
          '          <p id="details-content-automatch"></p>' +
          '          <p id="details-content-correction"></p>' +
          '          <div id="details-content-status">' +
          '            <div class="status-button status-button-verified" title="Verified"><span class="icon">&#xf14a;</span></div>' +        
          '            <div class="status-button status-button-not-verified" title="Not Verified"><span class="icon">&#xf059;</span></div>' +     
          '            <div class="status-button status-button-false-detection" title="False Detection"><span class="icon">&#xf057;</span></div>' +   
          '            <div class="status-button status-button-ignore" title="Ignore this toponym"><span class="icon">&#xf05e;</span></div>' + 
          '            <div class="status-button status-button-no-suitable-match" title="No suitable gazetteer match available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status-button status-button-ambiguous" title="Multiple possible gazetteer matches available"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status-button status-button-multiple" title="Toponym refers to multiple places"><span class="icon">&#xf024;</span></div>' + 
          '            <div class="status-button status-button-not-identifyable" title="Not Identifiable"><span class="icon">&#xf024;</span></div>' + 
          '          </div>' +
          '        </div>' +
          '        <div class="details-content-preview"></div>' +
          '      </div>' + // details-content-topsection
          '      <div id="details-content-bottomsection">' +
          '        <table id="details-content-searchresults"></table>' +          
          '        <div id="details-content-tags">' +
          '          <h3><span class="icon">&#xf02b;</span>&nbsp;&nbsp;Tags</h3>' +
          '          <div class="tag-list"></div>' +
          '        </div>' +
          '        <div id="details-content-comment">' +
          '          <h3><span class="icon">&#xf075;</span>&nbsp;&nbsp;Comment</h3>' +
          '          <button class="details-comment-button button dark" type="button">SAVE</button>' +
          '          <textarea class="details-comment-textarea"></textarea>' + 
          '        </div>' +
          '        <div id="details-content-history">' +
          '          <h3><span class="icon">&#xf040;</span>&nbsp;&nbsp;Edit History</h3>' +
          '        </div>' +
          '      </div>' + // details-content-bottomsection
          '    </div>' + // popup-content
          '  </div>' + // details
          '</div>'; // clicktrap
    
    // Details Popup DOM element
    this.element = $(template);
    this.element.appendTo(document.body);
  
    // Leaflet map
    var map = new Map(document.getElementById('details-map'), opt_basemap);
    map.on('baselayerchange', function(e) { self.fireEvent('baselayerchange', e); });

    /**
     * Generates a view of a search result by rendering an HTML table row and attach a marker to the map
     * @param {Object} result the search result
     * @param {Object!} opt_style the map marker style
     */
    var displaySearchResult = function(result, opt_style) {
      var category = (result.category) ? common.Utils.formatCategory(result.category) : 'uncategorized',
          warning = (result.coordinate) ? '<td></td>' : '<td><span title="Place has no coordinates" class="icon no-coords">&#xf041;</span></td>',
          tableRow = $(
            '<tr>' + 
            '  <td>' + common.Utils.categoryTag(result.category) + '</td>' + warning + 
            '  <td><a href="' + result.uri + '" title="' + category + ' - ' + result.uri + ' (' + result.names + ')" class="details-content-candidate-link">' + result.title + '</a></td>' +
            '  <td>' + result.description + '</td>' + 
            '</tr>');
            
      if (result.coordinate)
        // If there's a coordinate, add to map
        map.addSearchresult(result);
      else
        // If not, skip the map and call saveCorrection on click
        tableRow.find('a').click(function(e) { 
          saveCorrection(result); 
          return false;
        });
      
      return tableRow;      
    };
    
    /**
     * Saves a manual correction by updating the place data from a search result
     * @param {Object} result the search result
     */
    var saveCorrection = function(result) {
      if (confirm('Are you sure you want to correct the mapping to ' + result.title + '?')) {
        if (!annotation.place_fixed)
          annotation.place_fixed = { };
        
        annotation.place_fixed.title = result.title;
        annotation.place_fixed.names = result.names;
        annotation.place_fixed.uri = result.uri;    
        annotation.place_fixed.coordinate = result.coordinate;
        annotation.status = 'VERIFIED';
        annotation.status = 'VERIFIED';
        
        self.fireEvent('update', annotation);        
        self.fireEvent('skip-next');
      }
    };

    map.on('selectSearchresult', saveCorrection);
    
    // Populate the template
    $('.popup-exit').click(function() { self.destroy(); });
    $('.details-header-toponym').html(annotation.toponym);
    $('.details-skip-prev').click(function() { self.fireEvent('skip-prev'); });
    $('.details-skip-next').click(function() { self.fireEvent('skip-next'); });
    
    var sourceLabel = '';
    if (annotation.part)
      sourceLabel += ' in ' + annotation.part;
    if (annotation.source)
      sourceLabel += ' <a href="' + annotation.source + '" target="_blank" title="Visit External Source">&#xf08e;</a>';
    $('.details-header-source-label').html(sourceLabel);
  
    // Automatch info
    if (annotation.place) {
      var meta = '<a href="http://pelagios.org/api/places/' + 
                 encodeURIComponent(common.Utils.normalizePleiadesURI(annotation.place.uri)) +
                 '" target="_blank">' + annotation.place.title + '</a> ' +
                 common.Utils.categoryTag(annotation.place.category) + '<br/>';
                 
      if (annotation.place.description)
        meta += annotation.place.description + '<br/>';
        
      if (annotation.place.names)
        meta += annotation.place.names.slice(0, 8).join(', ') + '<br/>';
                              
      if (!annotation.place.coordinate)
        meta += '<span class="icon no-coords ">&#xf041;</span>No coordinates for this place!</a>';
               
      $('#details-content-automatch').html(meta);
    } else {
      $('#details-content-automatch').html('-');
    }
  
    // Expert correction info
    if (annotation.place_fixed) {
      var meta = '<a href="http://pelagios.org/api/places/' + 
                 encodeURIComponent(common.Utils.normalizePleiadesURI(annotation.place_fixed.uri)) +
                 '" target="_blank">' + annotation.place_fixed.title + '</a> ' +
                 common.Utils.categoryTag(annotation.place_fixed.category) + '<br/>';
                 
      if (annotation.place_fixed.description)
        meta += annotation.place_fixed.description + '<br/>';
        
      if (annotation.place_fixed.names)
        meta += annotation.place_fixed.names.slice(0, 8).join(', ') + '<br/>';
        
      if (!annotation.place_fixed.coordinate)
        meta += '<span class="icon no-coords ">&#xf041;</span>No coordinates for this place!</a>';
               
      $('#details-content-correction').html(meta);
    } else {
      $('#details-content-correction').html('-');
    }
  
    // Tags
    var tagList = new common.TagList($('.tag-list'), annotation.tags);  
    tagList.on('update', function(tags) {
      annotation.tags = tags;
      self.fireEvent('update', annotation);
    });
  
    // Status info & buttons
    if (annotation.status == 'VERIFIED') {
      $('.status-button-verified').addClass('active');
    } else if (annotation.status == 'NOT_VERIFIED') {
      $('.status-button-not-verified').addClass('active');
    } else if (annotation.status == 'FALSE_DETECTION') {
      $('.status-button-false-detection').addClass('active');
    } else if (annotation.status == 'IGNORE') {
      $('.status-button-ignore').addClass('active');
    } else if (annotation.status == 'NO_SUITABLE_MATCH') {
      $('.status-button-no-suitable-match').addClass('active');
    } else if (annotation.status == 'AMBIGUOUS') {
      $('.status-button-ambiguous').addClass('active');
    } else if (annotation.status == 'MULTIPLE') {
      $('.status-button-multiple').addClass('active');
    } else if (annotation.status == 'NOT_IDENTIFYABLE') {
      $('.status-button-not-identifyable').addClass('active');   
    }
  
    // Status buttons
    var changeStatus = function(button, status) {
      button.click(function() {
        if (annotation.status != status) {
          annotation.status = status;
          self.fireEvent('update', annotation);
          self.fireEvent('skip-next'); 
        }
      });
    };
  
    // Button 'verified'
    changeStatus($('.status-button-verified'), 'VERIFIED');
    changeStatus($('.status-button-not-verified'), 'NOT_VERIFIED');
    changeStatus($('.status-button-false-detection'), 'FALSE_DETECTION');
    changeStatus($('.status-button-ignore'), 'IGNORE');
    changeStatus($('.status-button-no-suitable-match'), 'NO_SUITABLE_MATCH');
    changeStatus($('.status-button-ambiguous'), 'AMBIGUOUS');
    changeStatus($('.status-button-multiple'), 'MULTIPLE');
    changeStatus($('.status-button-not-identifyable'), 'NOT_IDENTIFYABLE');
  
    // Comment
    var commentTextArea = $('.details-comment-textarea');
    if (annotation.comment)
      commentTextArea.val(annotation.comment);
  
    $('.details-comment-button').click(function(e) {
      var comment = commentTextArea.val();
      if (annotation.comment != comment) {
        annotation.comment = comment;
        self.fireEvent('update', annotation);
      }
    });
      
    // Populate the map
    map.addAnnotation(annotation);
    map.addSequence(annotation, prev_annotations, next_annotations);
  
    // Other candidates list
    $('#details-content-searchresults').on('click', 'a', function(e) {
      map.selectSearchresult(e.target.href);
      return false;
    });
    
    if (annotation.toponym) {
      $.getJSON('api/search/place?query=' + annotation.toponym.toLowerCase(), function(data) {
        var html = [],
            automatchURI = (annotation.place) ? annotation.place.uri : undefined,
            relevantURI = (annotation.place_fixed) ? annotation.place_fixed.uri : automatchURI;
    
        $.each(data.results, function(idx, result) {
          if (result.uri != relevantURI) {
            html.push(displaySearchResult(result, { color:'#0055ff', radius:5, stroke:false, fillOpacity:0.8 }));
          }
        });
        map.fitToSearchresults();

        if (html.length == 0) {
          $('#details-content-searchresults').html('<tr><td>No alternatives found.</td></tr>');
        } else {
          $('#details-content-searchresults').append(html);
        }
      });
    }
  
    // Toponym context (i.e. fulltext preview snippet)
    $.getJSON('api/annotations/' + annotation.id, function(a) {
      if (a.context) {
        var startIdx = a.context.indexOf(a.toponym);
        var endIdx = startIdx + a.toponym.length;
        if (startIdx > -1 && endIdx <= a.context.length) {
          var pre = a.context.substring(0, startIdx);
          var post = a.context.substring(endIdx);
          $('.details-content-preview').html('...' + pre + '<em>' + a.toponym + '</em>' + post + '...');
        }
      }    
    });
  
    // Text search
    var markers = [];
    $('.details-content-search-input').keypress(function(e) {
      if (e.which == 13) {
        // Clear previous results (if any)
        $('#details-content-searchresults').html('');
        $.each(markers, function(idx, marker) { map.removeLayer(marker); });
        markers = [];
      
        $.getJSON('api/search/place?query=' + e.target.value.toLowerCase(), function(response) {
          var html = [];
          $.each(response.results, function(idx, result) {
            var displayedResult = displaySearchResult(result)
            html.push(displayedResult);
          
            if (displayedResult.marker)
              markers.push(displayedResult.marker);
          });
        
          if (html.length == 0) {       
            $('#details-content-searchresults').html('»<tr><td>No results for &quot;' + response.query + '«</td></tr>');
          } else {
            $('#details-content-searchresults').append(html);
          }
        
          // map.fitToSearchresults();
        });
      }
    });
    
    this.map = map;
    $('.details-content-search-input').focus();
  }

  // Inheritance - not the nicest pattern but works for our case
  DetailsPopup.prototype = new common.HasEvents();

  /** 
   * Destroys the popup.
   */
  DetailsPopup.prototype.destroy = function() {
    this.map.destroy();
    window.location.hash = '';
    $(this.element).remove();
  }
  
  return DetailsPopup;

});
