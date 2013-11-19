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
    '      <h1>Toponym: <span class="details-popup-content-toponym"></span></h1>' +
    '      <p>Matched to Place: <span class="details-popup-content-matched-to"></span></p>' +
    '      <h3>Possible Alternatives:</h3>' +
    '      <table class="details-popup-content-candidates">' +
    '      </table>' +    
    '      <h3>Source Preview</h3>' + 
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
  
  if (place.place) {
    $('.details-popup-content-matched-to').html('<a href="' + place.place.uri + '">' + place.place.title + '</a>');
  } else {
    $('.details-popup-content-matched-to').html('-');
  }
  
  var map = this._initMap($('.details-popup-content'));
  if (place.coordinate) {
    var marker = L.marker(place.coordinate).addTo(map);  
    $('.details-popup-content-toponym').mouseover(function() {
      marker.bindPopup(place.gazetteer_title).openPopup();
    });
  }
  
  // Other candidates  
  $.getJSON('../search/' + place.toponym.toLowerCase(), function(data) {
    var html = [];
    var markers = [];
    $.each(data.results, function(idx, result) {
      if (result.uri != place.gazetteer_uri) {
        var row = $('<tr><td><a href="javascript:void(0);" class="details-popup-content-candidate-link">' + result.title + '</a></td><td>' + result.names + '</td></tr>');
        
        var marker = undefined;
        if (result.coords) {
          marker = L.marker(result.coords, { opacity: 0.5 }).addTo(map); 
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
    
    $('.details-popup-content-candidates').append(html);
    map.fitBounds(new L.featureGroup(markers).getBounds());
  });
  
  // Preview snippets
  $.getJSON('../preview?url=' + encodeURIComponent(place.source) + '&term=' + place.toponym, function(snippets) {
    $('.details-popup-content-preview').html(snippets);
  });
}

pelagios.georesolution.DetailsPopup.prototype._initMap = function(parentEl) {
  var mapDiv = document.createElement('div');
  mapDiv.className = 'details-popup-map';
  $(parentEl).prepend(mapDiv);
  
  var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
    attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
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
