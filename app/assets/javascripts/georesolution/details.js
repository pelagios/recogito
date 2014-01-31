define(['georesolution/common'], function(common) {

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
  var DetailsPopup = function(annotation, prev_annotations, next_annotations) {
    // Inheritance - not the nicest pattern but works for our case
    common.HasEvents.call(this);
    
    var self = this,
        template = 
          '<div class="clicktrap">' +
          '  <div class="popup details">' +
          '    <div class="popup-header">' +
          '      »<span class="details-header-toponym"></span>«' +
          '      <span class="details-header-source">in <span class="details-header-source-label"></span></span>' + 
          '      <a class="popup-exit">&#xf00d;</a>' +
          '    </div>' +
          '    <div class="popup-content">' +
          '      <div class="popup-content-inner">' +
          '        <div class="details-content-sidebar">' +
          '          <div class="details-content-search">' +
          '            <div class="details-content-search-container">' +
          '              <span>Search</span> <input class="details-content-search-input">' +
          '            </div>'+
          '            <table class="details-content-search-results">' +
          '            </table>' +
          '          </div>' +
          '        </div>' +
          '        <div class="details-content-placeinfo">' +
          '          <p class="details-content-automatch"></p>' +
          '          <p class="details-content-correction"></p>' +
          '        </div>' +
          '        <div class="popup-tags">' +
          '        </div>' +
          '        <div class="details-button details-button-verified"><span class="icon">&#xf14a;</span><span class="caption">VERIFIED</span></div>' +        
          '        <div class="details-button details-button-not-verified"><span class="icon">&#xf059;</span><span class="caption">NOT VERIFIED</span></div>' +   
          '        <div class="details-button details-button-not-identifyable"><span class="icon">&#xf024;</span><span class="caption">NOT IDENTIFYABLE</span></div>' +   
          '        <div class="details-button details-button-false-detection"><span class="icon">&#xf057;</span><span class="caption">FALSE DETECTION</span></div>' +   
          '        <div class="details-button details-button-ignore"><span class="icon">&#xf05e;</span><span class="caption">IGNORE/DUPLICATE</span></div>' + 
          '        <h3>Comment</h3>' +
          '        <div class="details-comment">' +
          '          <textarea class="details-comment-textarea"></textarea>' + 
          '          <input class="details-comment-button" type="button" value="SAVE"></input>' +
          '        </div>' +
          '        <h3>Source Text Snippets</h3>' + 
          '        <div class="details-content-preview">' +
          '        </div>' +
          '        <h3>Possible Alternatives</h3>' +
          '        <table class="details-content-candidates">' +
          '        </table>' +   
          '      </div>' + 
          '    </div>' +
          '  </div>' +
          '</div>';
    
    // Details Popup DOM element
    this.element = $(template);
    this.element.appendTo(document.body);
  
    // Leaflet map
    var map = this._initMap($('.details-content-sidebar'));

    /**
     * Generates a view of a search result by rendering an HTML table row and attach a marker to the map
     * @param {Object} result the search result
     * @param {Object!} opt_style the map marker style
    */
    var displaySearchResult = function(result, opt_style) {
      var warning = (result.coordinate) ? '' : '<span title="Place has no coordinates" class="icon no-coords">&#xf041;</span>'     
      var tr = $('<tr><td>' + warning + '</td><td><a href="javascript:void(0);" class="details-content-candidate-link">' + result.title + '</a></td><td>' + result.names + '</td></tr>');
      var marker = undefined;
      if (result.coordinate) {
        if (opt_style)
          marker = L.circleMarker(result.coordinate, opt_style).addTo(map); 
        else
          marker = L.marker(result.coordinate).addTo(map);
        
        marker.on('click', function(e) { saveCorrection(result); });
        marker.on('mouseover', function(e) { 
          marker.bindPopup(result.title).openPopup();
          $(tr).addClass('hilighted'); 
        });
        marker.on('mouseout', function(e) { 
          marker.closePopup();
          $(tr).removeClass('hilighted'); 
        });
      }
     
      var candidateLink = $(tr).find('.details-content-candidate-link');
      if (marker) {
        candidateLink.mouseover(function() { marker.bindPopup(result.title).openPopup(); });
        candidateLink.mouseout(function() { marker.closePopup(); });
      }
      candidateLink.click(function(e) { saveCorrection(result); });
    
      return { html: tr, marker: marker };
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
        
        self.fireEvent('update', annotation);        
        self.destroy();
      }
    };
    
    // Populate the template
    $('.popup-exit').click(function() { self.destroy(); });
    $('.details-header-toponym').html(annotation.toponym);
    $('.details-header-source-label').html(annotation.part + ' <a href="' + annotation.source + '" target="_blank" title="Visit External Source">&#xf08e;</a>');
  
    // Automatch info
    if (annotation.place) {
      var meta = annotation.place.title + 
                 common.Utils.formatCategory(annotation.place.category, ' ({{category}})') + '<br/>' +
                 annotation.place.names + '<br/>' +
                 '<a href="http://pelagios.org/api/places/' + 
                 encodeURIComponent(common.Utils.normalizePleiadesURI(annotation.place.uri)) +
                 '" target="_blank">' + annotation.place.uri + '</a>'; 
                              
      if (!annotation.place.coordinate)
        meta += '<br/><span class="icon no-coords ">&#xf041;</span>No coordinates for this place!</a>';
               
      $('.details-content-automatch').html(meta);
    } else {
      $('.details-content-automatch').html('-');
    }
  
    // Expert correction info
    if (annotation.place_fixed) {
      var meta = annotation.place_fixed.title + 
                 common.Utils.formatCategory(annotation.place_fixed.category, ' ({{category}})') + '<br/>' +
                 annotation.place_fixed.names + '<br/>' +
                 '<a href="http://pelagios.org/api/places/' + 
                 encodeURIComponent(common.Utils.normalizePleiadesURI(annotation.place_fixed.uri)) +
                 '" target="_blank">' + annotation.place_fixed.uri + '</a>'; 
               
      if (!annotation.place_fixed.coordinate)
        meta += '<br/><span class="icon no-coords ">&#xf041;</span>No coordinates for this place!</a>';
               
      $('.details-content-correction').html(meta);
    } else {
      $('.details-content-correction').html('-');
    }
  
    // Tags
    var tagList = new common.TagList($('.popup-tags'), annotation.tags);  
    tagList.on('update', function(tags) {
      annotation.tags = tags;
      self.fireEvent('update', annotation);
    });
  
    // Status info & buttons
    if (annotation.status == 'VERIFIED') {
      $('.details-button-verified').addClass('active');
    } else if (annotation.status == 'NOT_VERIFIED') {
      $('.details-button-not-verified').addClass('active');
    } else if (annotation.status == 'NOT_IDENTIFYABLE') {
      $('.details-button-not-identifyable').addClass('active');
    } else if (annotation.status == 'FALSE_DETECTION') {
      $('.details-button-false-detection').addClass('active');
    } else if (annotation.status == 'IGNORE') {
      $('.details-button-ignore').addClass('active');
    }
  
    // Status buttons
    var changeStatus = function(button, status) {
      button.click(function() {
        if (annotation.status != status) {
          annotation.status = status;
          self.fireEvent('update', annotation);
          self.destroy(); 
        }
      });
    };
  
    // Button 'verified'
    changeStatus($('.details-button-verified'), 'VERIFIED');
    changeStatus($('.details-button-not-verified'), 'NOT_VERIFIED');
    changeStatus($('.details-button-not-identifyable'), 'NOT_IDENTIFYABLE');
    changeStatus($('.details-button-false-detection'), 'FALSE_DETECTION');
    changeStatus($('.details-button-ignore'), 'IGNORE');
  
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
      
    // Popuplate the map
    if (annotation.marker) {
      // Marker for auto-match
      if (annotation.place && annotation.place.coordinate) {
        var marker = L.circleMarker(annotation.place.coordinate, { color:'blue', opacity:1, fillOpacity:0.6 }).addTo(map);    
        var popup = '<strong>Auto-Match:</strong> ' + annotation.place.title;
        marker.on('mouseover', function(e) { marker.bindPopup(popup).openPopup(); });
        $('.details-content-automatch').mouseover(function() { marker.bindPopup(popup).openPopup(); });
      }
  
      // Marker for manual correction (if any)
      if (annotation.place_fixed && annotation.place_fixed.coordinate) {
        var markerFixed = L.circleMarker(annotation.place_fixed.coordinate, { color:'red', opacity:1, fillOpacity:0.6 }).addTo(map);   
        var popupFixed =   '<strong>Correction:</strong> ' + annotation.place_fixed.title;
        markerFixed.on('mouseover', function(e) { markerFixed.bindPopup(popupFixed).openPopup(); });
        $('.details-content-correction').mouseover(function() { markerFixed.bindPopup(popupFixed).openPopup(); });
      }
  
      // Sequence
      if (prev_annotations && next_annotations) {
        var coords = [];
    
        for (var i = 0; i < prev_annotations.length; i++)
          coords.push(prev_annotations[i].marker.getLatLng());
       
        if (annotation.place_fixed && annotation.place_fixed.coordinate)
          coords.push(annotation.place_fixed.coordinate);
        else if (annotation.place && annotation.place.coordinate)
          coords.push(annotation.place.coordinate);
      
        for (var i = 0; i < next_annotations.length; i++)
          coords.push(next_annotations[i].marker.getLatLng());
      
        var line = L.polyline(coords, { color:annotation.marker.options.color, opacity:1, weight:8 });
        line.setText('►', { repeat: true, offset: 3, attributes: { fill: '#fff', 'font-size':10 }});    
        map.fitBounds(line.getBounds());
        line.addTo(map);
        line.bringToBack();
      }
    } else {
      map.setView([48.69096, 9.14062], 4);
    }
  
    // Other candidates list
    $.getJSON('api/search/place?query=' + annotation.toponym.toLowerCase(), function(data) {
      var html = [],
          automatchURI = (annotation.place) ? annotation.place.uri : undefined,
          relevantURI = (annotation.place_fixed) ? annotation.place_fixed.uri : automatchURI;
    
      $.each(data.results, function(idx, result) {
        if (result.uri != relevantURI) {
          html.push(displaySearchResult(result, { color:'#0055ff', radius:5, stroke:false, fillOpacity:0.8 }).html);
        }
      });

      if (html.length == 0) {
        $('.details-content-candidates').html('<p>No alternatives found.</p>');
      } else {
        $('.details-content-candidates').append(html);
      }
    });
  
    // Toponym context (i.e. fulltext preview snippet)
    $.getJSON('api/annotations/' + annotation.id, function(a) {
      if (a.context) {
        var startIdx = a.context.indexOf(a.toponym);
        var endIdx = startIdx + a.toponym.length;
        if (startIdx > -1 && endIdx <= a.context.length) {
          var pre = a.context.substring(0, startIdx);
          var post = a.context.substring(endIdx);
          $('.details-content-preview').html(pre + '<em>' + a.toponym + '</em>' + post);
        }
      }    
    });
  
    // Text search
    var markers = [];
    $('.details-content-search-input').keypress(function(e) {
      if (e.charCode == 13) {
        // Clear previous results (if any)
        $('.details-content-search-results').html('');
        $.each(markers, function(idx, marker) { map.removeLayer(marker); });
        markers = [];
      
        $.getJSON('api/search/place?query=' + e.target.value.toLowerCase(), function(response) {
          var html = [];
          $.each(response.results, function(idx, result) {
            var displayedResult = displaySearchResult(result)
            html.push(displayedResult.html);
          
            if (displayedResult.marker)
              markers.push(displayedResult.marker);
          });
        
          if (html.length == 0) {
            $('.details-content-search-results').html('<p>No results for &quot;' + response.query + '</p>');
          } else {
            $('.details-content-search-results').append(html);
          }
        
          map.fitBounds(new L.featureGroup(markers).getBounds());
        });
      }
    });
  }

  // Inheritance - not the nicest pattern but works for our case
  DetailsPopup.prototype = new common.HasEvents();

  /**
   * Initializes the Leaflet map
   * @param {Element} parentEl the DOM element to attach to 
   * @private
   */
  DetailsPopup.prototype._initMap = function(parentEl) {
    var mapDiv = document.createElement('div');
    mapDiv.className = 'details-map';
    $(parentEl).prepend(mapDiv);
  
    var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
      attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012'
    });
  
    var map = new L.Map(mapDiv, {
      layers: [baseLayer],
      minZoom: 3,
      maxZoom: 11
    });

    return map;
  }

  /** 
   * Destroys the popup.
   */
  DetailsPopup.prototype.destroy = function() {
    $(this.element).remove();
  }
  
  return DetailsPopup;

});
