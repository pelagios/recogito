/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

recogito.PublicMap = function(mapDiv, dataURL) {
  var self = this,
      dareLayer = L.tileLayer('http://pelagios.org/tilesets/imperium/{z}/{x}/{y}.png', {
    	  attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>'
      }),     
      awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
        attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                     'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                     'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                     '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
      });
      bingLayer = new L.BingLayer("Au8CjXRugayFe-1kgv1kR1TiKwUhu7aIqQ31AjzzOQz0DwVMjkF34q5eVgsLU5Jn"),
      layer_switcher_template = 
        '<div class="publicmap-layerswitcher">' +
        '  <div class="publicmap-layerswitcher-all">' +
        '    <table>' +
        '      <tr>' + 
        '        <td><input type="checkbox" checked="true" class="switch-all"></input></td>' +
        '        <td>All</td>'
        '      </tr>' +
        '    </table>' +
        '  </div>' +
        '</div>';
        
  this._map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 5,
    layers: [awmcLayer, bingLayer, dareLayer],
    minZoom: 3
  });
  
  var baseLayers = { 'Satellite': bingLayer, 'Empty Base Map': awmcLayer, 'Roman Empire Base Map': dareLayer };
  this._map.addControl(new L.Control.Layers(baseLayers, null, { position: 'topleft' }));
  
  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    var palette = new recogito.ColorPalette();
    
    if (data.annotations) {
      var layerGroup = L.layerGroup();
      layerGroup.addTo(self._map);
      $.each(data.annotations, function(annotationIdx, annotation) {
        self.addPlaceMarker(annotation, layerGroup, palette.getDarkColor(0), palette.getLightColor(0));
      });     
    } else { 
      var layers = '<table>' +
                 '  <tr class="table-header"><td></td><td>Title</td><td># Toponyms</td><td></td>';
      var layerGroups = [];
    
      $.each(data.parts, function(partIdx, part) {
        layers += '<tr>' +
                    '<td><input type="checkbox" checked="true" data-part="' + partIdx + '" class="switch"></input></td>' +
                    '<td class="part-title" style="background-color:' + palette.getDarkColor(partIdx) + '">' + part.title + '</td>' +
                    '<td class="centered">' + part.annotations.length + '</td>';
        if (part.source)
          layers += '<td><a href="' + part.source + '" target="_blank">Text Online</a></td>';
        
        layers += '</tr>';
    
        var layerGroup = L.layerGroup();
        layerGroup.addTo(self._map);
        layerGroups.push(layerGroup);
      
        $.each(part.annotations, function(annotationIdx, annotation) {
          self.addPlaceMarker(annotation, layerGroup, palette.getDarkColor(partIdx), palette.getLightColor(partIdx));
        });
      });
      layers += '</table>';
    
      var layer_switcher = $(layer_switcher_template);
      layer_switcher.prepend(layers);
      layer_switcher.appendTo(mapDiv);
    
      layer_switcher.on('change', '.switch', function(e) {
        var part = parseInt($(e.target).data('part'), 10);
        var checked = $(e.target).prop('checked');
        if (checked)
          self._map.addLayer(layerGroups[part]);
        else
          self._map.removeLayer(layerGroups[part]);
      });
    
      layer_switcher.on('change', '.switch-all', function(e) {
        var checked = $(e.target).prop('checked');
        $('.switch').prop('checked', checked).trigger('change');
      });
    }
  });
  
  this._styles = { 
        
    VERIFIED: { color: '#118128', fillColor:'#1bcc3f', radius: 4, weight:2, opacity:1, fillOpacity: 1 }
    
  }
  
}

recogito.PublicMap.prototype.addPlaceMarker = function(annotation, layerGroup, stroke, fill) {
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
      var style = { color: stroke, fillColor:fill, radius: 4, weight:2, opacity:1, fillOpacity: 1 }
      var marker = L.circleMarker(place.coordinate, style);
      marker.on('click', function() { loadDetails(annotation.id, marker); });
      layerGroup.addLayer(marker);
    }
  }
}

recogito.ColorPalette = function() {
  this.dark = [ '#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf' ];
  this.light = [ '#aec7e8', '#ffbb78', '#98df8a', '#ff9896', '#c5b0d5', '#c49c94', '#f7b6d2', '#c7c7c7', '#dbdb8d', '#9edae5' ];
}

recogito.ColorPalette.prototype.getDarkColor = function(idx) {
  return this.dark[idx % this.dark.length];
}

recogito.ColorPalette.prototype.getLightColor = function(idx) {
  return this.light[idx % this.light.length]
}


