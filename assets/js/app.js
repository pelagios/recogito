/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * The main application constructor
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @param {Element] mapDiv the DIV to hold the Leaflet map
 * @constructor
 */
pelagios.georesolution.CorrectionTool = function(tableDiv, mapDiv, dataURL) {  
  var self = this;
  
  /** @private **/
  var map = new pelagios.georesolution.MapView(mapDiv);
  
  /** @private **/
  var table = new pelagios.georesolution.TableView(tableDiv, function() {
    map.clear();
    if (self.places)
      $.each(self.places, function(idx, place) { map.addPlaceMarker(place) })
  });
  
  /** @private **/
  this._places = [];
  
  map.onSelect = function(place) { table.selectByPlaceURI(place.place.uri); };
  table.onSelectionChanged = function(args, place) { 
    var prev2 = self.getPrevN(args.rows[0], 2);
    var next2 = self.getNextN(args.rows[0], 2);
    map.highlightPlace(place, prev2, next2); 
  };

  $.getJSON(dataURL, function(data) {
    // Flatten & repackage response
    var places = [];
    var runningIdx = 0;
    $.each(data.parts, function(idx, part) {
      $.each(part.places, function(idx, place) {
        place.idx = runningIdx;
        place.source = part.source;
        place.worksheet = part.title;
        places.push(place);
        runningIdx++;
      });
    });
    
    // Set data on table
    table.setData(places, true);
    table.render();
    
    // Set data on map
    $.each(places, function(idx, place) { map.addPlaceMarker(place) })
    
    self.places = places;
  });
}

pelagios.georesolution.CorrectionTool.prototype._getNeighbours = function(idx, n, step) {
  if (!n)
    n = 2;
            
  var neighbours = [];
  var ctr = 1;
  while (neighbours.length < n) {   
    if (idx + ctr * step >= this.places.length)
      break;
      
    if (idx + ctr * step < 0)
      break;
              
    if (this.places[idx + ctr * step].marker)
      neighbours.push(this.places[idx + ctr * step]);
      
    ctr++;
  }
      
  return neighbours;
}

pelagios.georesolution.CorrectionTool.prototype.getNextN = function(idx, n)  {
  return this._getNeighbours(idx, n, 1);
}

pelagios.georesolution.CorrectionTool.prototype.getPrevN = function(idx, n)  {
  return this._getNeighbours(idx, n, -1);
}

pelagios.georesolution.Utils = {
  
  normalizePleiadesURI: function(uri) {
    if (uri.indexOf('#this') < 0) {
      return uri;
    } else {
      return uri.substring(0, uri.indexOf('#this'));
    }
  }
  
}
