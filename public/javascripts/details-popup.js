/**
 * A popup showing all details about a single annotation, with extra functionality
 * to make manual corrections to it.
 * 
 * Emits the following events:
 * 'save' .............. when a correction is saved 
 * 'markedAsFalse' ..... when an annotation is marked as a false detection
 * 'notIdentifiable' ... when an annotation is marked as a 'not identifiable' place
 * 
 * @param {Object} annotation the annotation
 * @param {Array.<Object>} prev_annotations previous annotations in the list (if any)
 * @param {Array.<Object>} next_annotations next annotations in the list (if any)
 * @constructor
 */
pelagios.georesolution.DetailsPopup = function(annotation, prev_annotations, next_annotations) {
  // Inheritance - not the nicest pattern but works for our case
  pelagios.georesolution.HasEvents.call(this);
    
  var self = this,
      template = 
        '<div class="clicktrap">' +
        '  <div class="details">' +
        '    <div class="details-header">' +
        '      <a class="details-button details-button-exit">EXIT</a>' +
        '    </div>' +
        '    <div class="details-content">' +
        '      <div class="details-content-sidebar">' +
        '        <div class="details-content-search">' +
        '          <div class="details-content-search-container">' +
        '            <span>Search</span> <input class="details-content-search-input">' +
        '          </div>'+
        '          <table class="details-content-search-results">' +
        '          </table>' +
        '        </div>' +
        '      </div>' +
        '      <h1>' + 
        '        &quot;<span class="details-content-toponym"></span>&quot; ' +
        '        <span class="details-content-source">in Online Source <span class="details-content-source-label"></span></span>' + 
        '      </h1>' +
        '      <table class="details-content-meta">' +
        '        <tr><td><strong>Auto-Match</strong></td><td class="details-content-auto-match"></td></tr>' +
        '        <tr><td><strong>Correction</strong></td><td class="details-content-correction"></td></tr>' +
        '      </table>' +
        '      <a class="details-button details-button-false-detection">FALSE DETECTION</a> <a class="details-button details-button-not-identifiable">NOT IDENTIFY-ABLE</a>' +
        '      <h3>Source Text Snippets</h3>' + 
        '      <div class="details-content-preview">' +
        '      </div>' +
        '      <h3>Possible Alternatives</h3>' +
        '      <table class="details-content-candidates">' +
        '      </table>' +    
        '    </div>' +
        '  </div>' +
        '</div>';
    
  // Details Popup DOM element
  this.element = $(template);
  $(this.element).appendTo(document.body);
  
  // Leaflet map
  var map = this._initMap($('.details-content-sidebar'));

  /**
   * Generates a view of a search result by rendering an HTML table row and attach a marker to the map
   * @param {Object} result the search result
   * @param {Object!} opt_style the map marker style
   */
  var displaySearchResult = function(result, opt_style) {
    var warning = (result.coords) ? '' : '<span title="Place has no coordinates" class="icon no-coords">&#xf041;</span>'     
    var tr = $('<tr><td>' + warning + '</td><td><a href="javascript:void(0);" class="details-content-candidate-link">' + result.title + '</a></td><td>' + result.names + '</td></tr>');
    var marker = undefined;
    if (result.coords) {
      if (opt_style)
        marker = L.circleMarker(result.coords, opt_style).addTo(map); 
      else
        marker = L.marker(result.coords).addTo(map);
        
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
      annotation.place_fixed.coordinate = result.coords;
        
      self.fireEvent('save', annotation);        
      self.destroy();
    }
  };
    
  // Populate the template
  $('.details-button-exit').click(function() { self.destroy(); });
  $('.details-content-toponym').html(annotation.toponym);
  $('.details-content-source-label').html('<a href="' + annotation.source + '" target="_blank">' + annotation.part + '</a>');
  $('.details-button-false-detection').click(function() {
    if (confirm('This will remove the place from the list. Are you sure?')) {
      self.fireEvent('markedAsFalse', annotation);
      self.destroy();
    }
  });
  $('.details-button-not-identifiable').click(function() {
    if (confirm('This will mark the place as not identifiable and flag it for future investigation. Are you sure?')) {
      self.fireEvent('notIdentifiable', annotation);
      self.destroy();
    }
  }); 
  
  if (annotation.place) {
    var meta = '<a href="http://pelagios.org/api/places/' + 
                encodeURIComponent(pelagios.georesolution.Utils.normalizePleiadesURI(annotation.place.uri)) +
               '" target="_blank">' + annotation.place.title + '</a><br/>' +
               annotation.place.names;
               
    if (!annotation.place.coordinate)
      meta += '<br/>No coordinates for this place! <span class="table-no-coords">!</span></a>';
               
    $('.details-content-auto-match').html(meta);
  } else {
    $('.details-content-auto-match').html('-');
  }
  
  if (annotation.place_fixed) {
    var meta = '<a href="http://pelagios.org/api/places/' + 
                encodeURIComponent(pelagios.georesolution.Utils.normalizePleiadesURI(annotation.place_fixed.uri)) +
               '" target="_blank">' + annotation.place_fixed.title + '</a><br/>' +
               annotation.place_fixed.names;
               
    if (!annotation.place_fixed.coordinate)
      meta += '<br/>No coordinates for this place! <span class="table-no-coords">!</span></a>';
               
    $('.details-content-correction').html(meta);
  } else {
    $('.details-content-correction').html('-');
  }
  
  // Popuplate the map
  if (prev_annotations && next_annotations) {
    var coords = [];
    
    for (var i = prev_annotations.length - 1; i > -1; i--)
      coords.push(prev_annotations[i].marker.getLatLng());
      
    if (annotation.place_fixed && annotation.place_fixed.coordinate)
      coords.push(annotation.place_fixed.coordinate);
    else if (annotation.place && annotation.place.coordinate)
      coords.push(annotation.place.coordinate);
      
    for (var i = 0; i < next_annotations.length; i++)
      coords.push(next_annotations[i].marker.getLatLng());
      
    var line = L.polyline(coords, { color:'blue', opacity:0.8 });
    map.fitBounds(line.getBounds());
    line.addTo(map);
  }
  
  // Marker for auto-match
  if (annotation.place && annotation.place.coordinate) {
    var marker = L.circleMarker(annotation.place.coordinate, { color:'blue', opacity:1, fillOpacity:0.6 }).addTo(map);    
    var popup = '<strong>Auto-Match:</strong> ' + annotation.place.title;
    marker.on('mouseover', function(e) { marker.bindPopup(popup).openPopup(); });
    $('.details-content-auto-match').mouseover(function() { marker.bindPopup(popup).openPopup(); });
  }
  
  // Marker for manual correction (if any)
  if (annotation.place_fixed && annotation.place_fixed.coordinate) {
    var markerFixed = L.circleMarker(annotation.place_fixed.coordinate, { color:'red', opacity:1, fillOpacity:0.6 }).addTo(map);   
    var popupFixed =   '<strong>Correction:</strong> ' + annotation.place_fixed.title;
    markerFixed.on('mouseover', function(e) { markerFixed.bindPopup(popupFixed).openPopup(); });
    $('.details-content-correction').mouseover(function() { markerFixed.bindPopup(popupFixed).openPopup(); });
  }
  
  // Other candidates list
  $.getJSON('search?query=' + annotation.toponym.toLowerCase(), function(data) {
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
  
  // Preview snippets
  $.getJSON('preview?url=' + encodeURIComponent(annotation.source) + '&term=' + annotation.toponym, function(snippets) {
    var highlight = function(snippet) {
      var startIdx = snippet.indexOf(annotation.toponym);
      var endIdx = startIdx + annotation.toponym.length;
      if (startIdx > -1 && endIdx <= snippet.length) {
        var pre = snippet.substring(0, startIdx);
        var post = snippet.substring(endIdx);
        return pre + '<em>' + annotation.toponym + '</em>' + post;
      } else { 
        return snippet;
      }
    }
    
    if (snippets.length > 0) {
      var preview = '';
      $.each(snippets, function(idx, snippet) {
        preview += '<p>...' + highlight(snippet) + "...</p>";        
      });
      $('.details-content-preview').html(preview);
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
      
      $.getJSON('search?query=' + e.target.value.toLowerCase(), function(response) {
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
pelagios.georesolution.DetailsPopup.prototype = new pelagios.georesolution.HasEvents();

/**
 * Initializes the Leaflet map
 * @param {Element} parentEl the DOM element to attach to 
 * @private
 */
pelagios.georesolution.DetailsPopup.prototype._initMap = function(parentEl) {
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
pelagios.georesolution.DetailsPopup.prototype.destroy = function() {
  $(this.element).remove();
}
