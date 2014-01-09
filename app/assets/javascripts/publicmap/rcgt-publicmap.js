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
  var popupTemplate = 
    '<div class="publicmap-popup">' + 
    '  <span class="toponym">»{{toponym}}«</span> ({{title}})' +
    '  <p class="context">{{context}}</p>' +
    '  {{source}}' + 
    '  <p class="link">{{pelagios-link}}</p>' +
    '</div>';
    
  var highlightToponym = function(text, toponym) {
    var startIdx = text.indexOf(toponym);
    var endIdx = startIdx + toponym.length;
    if (startIdx > -1 && endIdx <= text.length) {
      var pre = text.substring(0, startIdx);
      var post = text.substring(endIdx);
      return pre + '<em>' + toponym + '</em>' + post;
    }
  };
  
  var loadDetails = function(annotationID, marker) {
    $.getJSON('/recogito/api/annotations/' + annotationID, function(a) {            
      var place = (a.place_fixed) ? a.place_fixed : a.place;
      var html = popupTemplate
                   .replace('{{toponym}}', a.toponym)
                   .replace('{{title}}', place.title)
                   .replace('{{pelagios-link}}', '<a target="_blank" href="http://pelagios.org/api/places/' + encodeURIComponent(place.uri) + '">Further resources about ' + place.title + '</a>');
                   
      if (a.source)
        html = html.replace('{{source}}', '<p class="link"><a href="' + a.source + '" target="_blank">Source Text</a></p>');
      else
        html = html.replace('{{source}}', '');
    
      if (a.context)
        html = html.replace('{{context}}', '...' + highlightToponym(a.context, a.toponym) + '...')
      else
        html = html.replace('{{context}}', '');
      
      marker.bindPopup(html).openPopup();
    });
  };
  
  if (annotation.status == 'VERIFIED') {
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
    if (place && place.coordinate) {
      var marker = L.circleMarker(place.coordinate, this._styles.VERIFIED).addTo(this._map);
      marker.on('click', function() { loadDetails(annotation.id, marker); });
    }
  }
}
