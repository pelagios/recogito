/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * @param {Object} place the place
 * @constructor
 */
pelagios.georesolution.DetailsPopup = function(place, opt_callback) {  
  var self = this,
      template = 
    '<div class="clicktrap">' +
    '  <div class="details-popup">' +
    '    <div class="details-popup-header">' +
    '      <a class="details-popup-header-exit">EXIT</a>' +
    '    </div>' +
    '    <div class="details-popup-content">' +
    '      <h1>' + 
    '        &quot;<span class="details-popup-content-toponym"></span>&quot; ' +
    '        <span class="details-popup-content-source">in Online Source <span class="details-popup-content-source-label"></span></span>' + 
    '      </h1>' +
    '      <table class="details-popup-content-meta">' +
    '        <tr><td><strong>Auto-Match</strong></td><td class="details-popup-content-auto-match"></td></tr>' +
    '        <tr><td><strong>Correction</strong></td><td class="details-popup-content-correction"></td></tr>' +
    '      </table>' +
    '      <h3>Possible Alternatives</h3>' +
    '      <table class="details-popup-content-candidates">' +
    '      </table>' +    
    '      <h3>Source Text Snippets</h3>' + 
    '      <div class="details-popup-content-preview">' +
    '      </div>' +
    '    </div>' +
    '  </div>' +
    '</div>';
    
  var correctWithResult = function(result) {
    if (confirm('Are you sure you want to correct the mapping to ' + result.title + '?')) {
      if (!place.place_fixed)
        place.place_fixed = { };
        
      place.place_fixed.title = result.title;
      place.place_fixed.names = result.names;
      place.place_fixed.uri = result.uri;    
      place.place_fixed.coordinate = result.coords;
            
      // TODO API call - write to GDocs
      self.destroy();
            
      if (opt_callback)
        opt_callback(place);
    }
  }
    
  this.element = $(template);
  $(this.element).appendTo(document.body);
  $('.details-popup-header-exit').click(function() { self.destroy(); });
  $('.details-popup-content-toponym').html(place.toponym);
  $('.details-popup-content-source-label').html('<a href="' + place.source + '" target="_blank">' + place.worksheet + '</a>');
  
  if (place.place) {
    var meta = '<a href="http://pelagios.org/api/places/' + 
                encodeURIComponent(pelagios.georesolution.Utils.normalizePleiadesURI(place.place.uri)) +
               '" target="_blank">' + place.place.title + '</a><br/>' +
               place.place.names;
               
    if (!place.place.coordinate)
      meta += '<br/>No coordinates for this place! <span class="table-no-coords">!</span></a>';
               
    $('.details-popup-content-auto-match').html(meta);
  } else {
    $('.details-popup-content-auto-match').html('-');
  }
  
  if (place.place_fixed) {
    var meta = '<a href="http://pelagios.org/api/places/' + 
                encodeURIComponent(pelagios.georesolution.Utils.normalizePleiadesURI(place.place_fixed.uri)) +
               '" target="_blank">' + place.place_fixed.title + '</a><br/>' +
               place.place_fixed.names;
               
    if (!place.place_fixed.coordinate)
      meta += '<br/>No coordinates for this place! <span class="table-no-coords">!</span></a>';
               
    $('.details-popup-content-correction').html(meta);
  } else {
    $('.details-popup-content-correction').html('-');
  }
  
  var map = this._initMap($('.details-popup-content'));
  if (place.place && place.place.coordinate) {
    var marker = L.circleMarker(place.place.coordinate, { color:'blue', opacity:1, fillOpacity:0.6 }).addTo(map);    
    var popup = '<strong>Auto-Match:</strong> ' + place.place.title;
    marker.on('mouseover', function(e) { marker.bindPopup(popup).openPopup(); });
    $('.details-popup-content-auto-match').mouseover(function() { marker.bindPopup(popup).openPopup(); });
  }
  
  if (place.place_fixed && place.place_fixed.coordinate) {
    var markerFixed = L.circleMarker(place.place_fixed.coordinate, { color:'red', opacity:1, fillOpacity:0.6 }).addTo(map);   
    var popupFixed =   '<strong>Correction:</strong> ' + place.place_fixed.title;
    markerFixed.on('mouseover', function(e) { markerFixed.bindPopup(popupFixed).openPopup(); });
    $('.details-popup-content-correction').mouseover(function() { markerFixed.bindPopup(popupFixed).openPopup(); });
  }
  
  // Other candidates  
  $.getJSON('../search/' + place.toponym.toLowerCase(), function(data) {
    var html = [];
    var markers = [];
    $.each(data.results, function(idx, result) {
      var placeURI = (place.place) ? place.place.uri : undefined;
      var currentURI = (place.place_fixed) ? place.place_fixed.uri : placeURI;
      if (result.uri != currentURI) {
        var row = $('<tr><td><a href="javascript:void(0);" class="details-popup-content-candidate-link">' + result.title + '</a></td><td>' + result.names + '</td></tr>');
        
        var marker = undefined;
        if (result.coords) {
          marker = L.circleMarker(result.coords, { color:'#0055ff', radius:5, stroke:false, fillOpacity:0.8 }).addTo(map); 
          marker.on('click', function(e) { correctWithResult(result); });
          marker.on('mouseover', function(e) { 
            marker.bindPopup(result.title).openPopup();
            $(row).addClass('hilighted'); 
          });
          marker.on('mouseout', function(e) { $(row).removeClass('hilighted'); });
          markers.push(marker);
        }
      
        if (marker) {
          $(row).mouseover(function() {
            marker.bindPopup(result.title).openPopup();
          });
        }
        
        $(row).find('.details-popup-content-candidate-link').click(function(e) { 
          correctWithResult(result);
        });
        html.push(row);
      }
    });

    if (html.length == 0) {
      $('.details-popup-content-candidates').html('<p>No alternatives found.</p>');
    } else {
      $('.details-popup-content-candidates').append(html);
    }
    
    if (markers.length > 0)
      map.fitBounds(new L.featureGroup(markers).getBounds());
  });
  
  // Preview snippets
  $.getJSON('../preview?url=' + encodeURIComponent(place.source) + '&term=' + place.toponym, function(snippets) {
    
    var highlight = function(snippet) {
      var startIdx = snippet.indexOf(place.toponym);
      var endIdx = startIdx + place.toponym.length;
      if (startIdx > -1 && endIdx <= snippet.length) {
        var pre = snippet.substring(0, startIdx);
        var post = snippet.substring(endIdx);
        return pre + '<em>' + place.toponym + '</em>' + post;
      } else { 
        return snippet;
      }
    }
    
    if (snippets.length > 0) {
      var preview = '';
      $.each(snippets, function(idx, snippet) {
        preview += '<p>...' + highlight(snippet) + "...</p>";        
      });
      $('.details-popup-content-preview').html(preview);
    }
  });
}

pelagios.georesolution.DetailsPopup.prototype._initMap = function(parentEl) {
  var mapDiv = document.createElement('div');
  mapDiv.className = 'details-popup-map';
  $(parentEl).prepend(mapDiv);
  
  var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
    attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012'
  });
  
  var map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022), // TODO fit to place coords
    zoom: 3,
    layers: [baseLayer],
    minZoom: 3,
    maxZoom: 11
  });

  return map;
}

pelagios.georesolution.DetailsPopup.prototype.destroy = function() {
  $(this.element).remove();
}
