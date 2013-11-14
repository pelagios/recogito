/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * @param {Object} place the place
 * @constructor
 */
pelagios.georesolution.DetailsPopup = function(place) {
  var clicktrap = document.createElement('div');
  clicktrap.className = 'clicktrap';
  document.body.appendChild(clicktrap);
  
  // this._initInfo(parentEl, this._initMap(parentEl), place);  
}

/*
pelagios.tools.FixGeoResolutionWidget.prototype._initInfo = function(parentEl, map, place) {
  var h1 = document.createElement('h1');
  h1.innerHTML = place.toponym;
  parentEl.appendChild(h1);
  
  if (place.coordinate)
    L.circleMarker(place.coordinate).addTo(map); 
  
  var p = document.createElement('p');
  p.innerHTML = '<a href="' + place.gazetteer_uri + '">' + place.gazetteer_uri + '</a>';
  parentEl.appendChild(p);
  
  $.getJSON('../search/' + place.toponym.toLowerCase(), function(data) {
    var searchResults = data.results.length + ' results: <br/>';
    $.each(data.results, function(idx, place) {
      if (place.coords)
        L.circleMarker(place.coords).addTo(map); 
      
      searchResults += place.title + ' (' + place.names + ')<br/>';
    });
    
    p.innerHTML = p.innerHTML + '<br/>' + searchResults;
  });
}

pelagios.tools.FixGeoResolutionWidget.prototype._initMap = function(parentEl) {
  var mapDiv = document.createElement('div');
  mapDiv.className = 'detail-map';
  parentEl.appendChild(mapDiv);
  
  var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
        attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
      }),
  
      map = new L.Map(mapDiv, {
        center: new L.LatLng(41.893588, 12.488022), // TODO fit to place coords
        zoom: 3,
        layers: [baseLayer],
        minZoom: 3,
        maxZoom: 11
      });

  return map;
}
*/
