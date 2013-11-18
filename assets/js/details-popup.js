/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * @param {Object} place the place
 * @constructor
 */
pelagios.georesolution.DetailsPopup = function(place) {  
  this.element = this._initView(place);  
}

pelagios.georesolution.DetailsPopup.prototype._initView = function(place) {
  // Popup  
  var clicktrap = document.createElement('div');
  clicktrap.className = 'clicktrap';
  document.body.appendChild(clicktrap);
  
  var popup = document.createElement('div');
  popup.className = 'details-popup';
  clicktrap.appendChild(popup);

  // Header  
  var popupHeader = document.createElement('div');
  popupHeader.className = 'details-popup-header';
  
  var btnExit = document.createElement('a');
  btnExit.className = 'details-popup-header-exit';
  btnExit.innerHTML = 'EXIT';
  var self = this;
  $(btnExit).click(function() { self.destroy(); });
  popupHeader.appendChild(btnExit);
  popup.appendChild(popupHeader);
  
  // Content
  var popupContent = document.createElement('div');
  popupContent.className = 'details-popup-content';
  popup.appendChild(popupContent);
  
  var map = this._initMap(popupContent);
  
  var h1 = document.createElement('h1');
  h1.innerHTML = 'Toponym: <em>&quot;' + place.toponym + '&quot;</em>';
  popupContent.appendChild(h1);
  
  var details = document.createElement('p');
  details.innerHTML = 
    'Matched to Place: <a href="' + place.gazetteer_uri + '">' + place.gazetteer_title + '</a> ' +
    '(' + place.gazetteer_names + ')';
  popupContent.appendChild(details);
  
  // Map
  if (place.coordinate)
    L.marker(place.coordinate).addTo(map); 

  // Other candidates (via fuzzy index search)  
  var otherCandidates = document.createElement('p');
  popupContent.appendChild(otherCandidates);
  $.getJSON('../search/' + place.toponym.toLowerCase(), function(data) {
    var searchResults = '<h3>Possible Alternatives:</h3>' +
                        '<table>';
    $.each(data.results, function(idx, result) {
      if (result.uri != place.gazetteer_uri) {
        if (result.coords)
          L.marker(result.coords).addTo(map); 
      
        searchResults += '<tr><td><a href="' + result.uri + '">' + result.title + '</a></td><td>' + result.names + '</td></tr>';
      }
    });
    
    otherCandidates.innerHTML = searchResults + '</table>';
  });
  
  return clicktrap;
}


pelagios.georesolution.DetailsPopup.prototype._initMap = function(parentEl) {
  var mapDiv = document.createElement('div');
  mapDiv.className = 'details-popup-map';
  parentEl.appendChild(mapDiv);
  
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
  document.body.removeChild(this.element);
}
