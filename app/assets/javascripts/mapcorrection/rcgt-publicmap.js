/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

recogito.PublicMap = function(mapDiv, dataURL) {
  var self = this,
      baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
        attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
      });
        
  this._map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 5,
    layers: [baseLayer],
    minZoom: 3,
    maxZoom: 11
  });
  
  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    $.each(data.parts, function(idx, part) {
      $.each(part.annotations, function(idx, annotation) {
        self.addPlaceMarker(annotation);
      });
    });
  });
  
  this._styles = { 
        
    VERIFIED: { color: '#118128', fillColor:'#1bcc3f', radius: 4, weight:2, opacity:1, fillOpacity: 1 }
    
  }
  
}

recogito.PublicMap.prototype.addPlaceMarker = function(annotation) {
  if (annotation.status == 'VERIFIED') {
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
    if (place && place.coordinate) {
      L.circleMarker(place.coordinate, this._styles.VERIFIED).addTo(this._map);
    }
  }
}
