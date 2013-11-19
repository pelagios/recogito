/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * @param {Object} place the place
 * @constructor
 */
pelagios.georesolution.DetailsPopup = function(place) {  
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
    '    </div>' +
    '  </div>' +
    '</div>';
    
  this.element = $(template);
  $(this.element).appendTo(document.body);
  $('.details-popup-header-exit').click(function() { self.destroy(); });
  $('.details-popup-content-toponym').html(place.toponym);
    
  var map = this._initMap($('.details-popup-content'));
  if (place.coordinate)
    L.marker(place.coordinate).addTo(map);  
    
  if (place.gazetteer_uri) {
    $('.details-popup-content-matched-to').html('<a href="' + place.gazetteer_uri + '">' + place.gazetteer_title + '</a>');
  } else {
    $('.details-popup-content-matched-to').html('-');
  }
  
  // Other candidates  
  $.getJSON('../search/' + place.toponym.toLowerCase(), function(data) {
    var html = [];
    $.each(data.results, function(idx, result) {
      if (result.uri != place.gazetteer_uri) {
        if (result.coords)
          L.marker(result.coords).addTo(map); 
      
        var row = $('<tr><td><a href="javascript:void(0);" class="details-popup-content-candidate-link">' + result.title + '</a></td><td>' + result.names + '</td></tr>');
        $(row).find('.details-popup-content-candidate-link').click(function(e) { 
          // TODO implement
        });
        html.push(row);
      }
    });
    
    $('.details-popup-content-candidates').append(html);
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
