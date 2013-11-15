/** Namespaces **/
var pelagios = (window.pelagios) ? window.pelagios : { };
pelagios.georesolution = (pelagios.georesolution) ? pelagios.georesolution : { };

/**
 * The main application constructor
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @param {Element] mapDiv the DIV to hold the Leaflet map
 * @constructor
 */
pelagios.georesolution.CorrectionTool = function(tableDiv, mapDiv) {
  // TODO for testing only!
  var dataURL = 'test/mock-data.json';
  
  /** @private **/
  this._grid = this._initTable(tableDiv);
  
  /** @private **/
  this._map = this._initMap(mapDiv);

  var self = this;
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
    self._grid.setData(places, true);
    self._grid.render();
    
    // Set data on map
    $.each(places, function(idx, place) {
      if (place.coordinate)
        self.addPlaceMarker(place)
    })
  });
}

pelagios.georesolution.CorrectionTool.prototype.addPlaceMarker = function(place) {
  var STYLE_AUTOMATCH = { color: '#0000ff', radius: 4, fillOpacity: 0.8 };
  var STYLE_CORRECTION = { };

  var marker = L.circleMarker(place.coordinate, STYLE_AUTOMATCH);
  marker.addTo(this._map); 
  marker.on('click', function(e) {
    marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup(); 
    // var rowsToSelect = findRowsWithGID(grid, place.gazetteer_uri);
    // if (rowsToSelect.length > 0) {
    //   grid.setSelectedRows(rowsToSelect);
    //  grid.scrollRowIntoView(rowsToSelect[0], true);
    // }
  });
}

/**
 * Initializes the SlickGrid table.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @private
 */
pelagios.georesolution.CorrectionTool.prototype._initTable = function(tableDiv) {
  // A custom formatter for Pleiades URIs
  var pleiadesFormatter = function (row, cell, value, columnDef, dataContext) {
    if (value) {
      if (value.indexOf('http://pleiades.stoa.org') == 0) {
        var id =  value.substring(32);
        if (id.indexOf('#') > -1)
          id = id.substring(0, id.indexOf('#'));
          
        return '<a href="' + value + '" target="_blank">pleiades:' + id + '</a>';
      } else {
        return value;
      }
    }
  }

  var columns = [{ name: '#', field: 'idx', id: 'idx' },
                 { name: 'Toponym', field: 'toponym', id: 'toponym' },
                 { name: 'Worksheet', field: 'worksheet', id: 'worksheet' },
                 { name: 'Place ID', field: 'gazetteer_uri', id: 'gazetteer_uri' , formatter: pleiadesFormatter },
                 { name: 'Corrected', field: 'gazetteer_uri_fixed', id: 'gazetteer_uri_fixed', formatter: pleiadesFormatter },
                 { name: 'Comment', field: 'comment', id: 'comment' }];

  var options = { enableCellNavigation: true, enableColumnReorder: false, forceFitColumns: true, autoEdit: false };
    
  var grid = new Slick.Grid('#table', {}, columns, options);
  grid.setSelectionModel(new Slick.RowSelectionModel());
  grid.onDblClick.subscribe(function(e, args) {
    var popup = new pelagios.georesolution.DetailsPopup(grid.getDataItem(args.row));
  });

  // Redraw grid in case of window resize
  $(window).resize(function() { grid.resizeCanvas(); })
  
  return grid;
}

/**
 * Initializes the Leaflet map.
 * @param {Element} mapDiv the DIV to hold the map
 * @private
 */
pelagios.georesolution.CorrectionTool.prototype._initMap = function(mapDiv) {
  var baseLayer = L.tileLayer('http://pelagios.org/tilesets/imperium//{z}/{x}/{y}.png', {
    attribution: 'Tiles: <a href="http://pelagios.org/maps/greco-roman/about.html">Pelagios</a>, 2012; Data: NASA, OSM, Pleiades, DARMC'
  });
        
  var map = new L.Map(mapDiv, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 5,
    layers: [baseLayer],
    minZoom: 3,
    maxZoom: 11
  });
  
  return map;
}

/*
        var findRowsWithGID = function(grid, gazetteerID) {
          // TODO we could optimize this using an index, but individual EGDs should be small enough
          var size = grid.getDataLength();
          var rows = [];
          for (var i = 0; i < size; i++) {
            var row = grid.getDataItem(i);
            if (row.gazetteer_uri == gazetteerID)
              rows.push(i);
          }
          return rows;
        };
        
        var getNextN = function(places, idx, n) {
          if (!n)
            n = 2;
            
          var nextN = [];
          var ctr = 1;
          while (nextN.length < n) {
            if (places.length <= idx + ctr)
              break;
              
            if (places[idx + ctr].marker)
              nextN.push(places[idx + ctr]);
              
            ctr++;
          }
          
          return nextN;
        };
        
        var getPrevN = function(places, idx, n) {
          if (!n)
            n = 2;
            
          var prevN = [];
          var ctr = 1;
          while (prevN.length < n) {
            if (idx - ctr < 0)
              break;
              
            if (places[idx - ctr].marker)
              prevN.push(places[idx - ctr]);
              
            ctr++;
          }
          
          return prevN;
        };
        
        var markerStyle = { radius:5, stroke: false, fillOpacity: 0.8 }
        var markerStyleCorrected = { radius:5, stroke: false, fillOpacity: 0.8, color: 'yellow' } 
        
        /** Sequence polylines **
        var sequence = [];
        
        /** Connecting line between a place and a manual fix **
        var lastFixConnection = undefined;
        
        $.getJSON('../api/0Avh0MASZtid1dFQyUFF4TDVmeHVBQVNRUHQ2ZFdIdXc', function(data) {
        
          $.each(places, function(idx, place) {
            if (place.coordinate) {
              });
            }
            
            // TODO eliminate code duplication
            if (place.fixedCoordinate) {
              place.marker = L.circleMarker(place.fixedCoordinate, markerStyleCorrected);
              place.marker.addTo(map); 
              place.marker.on('click', function(e) {
                place.marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup(); 

                if (lastFixConnection) {
                    map.removeLayer(lastFixConnection);
                    lastFixConnection = undefined;
                }

                if (place.coordinate) {
                  var connection = [ place.coordinate, place.fixedCoordinate ];
                  lastFixConnection = L.polyline(connection, { color: 'yellow', opacity: 1 });
                  lastFixConnection.addTo(map);
                }
              });
            }
          });
                              
          grid.onSelectedRowsChanged.subscribe(function(e, args) { 
            if (args.rows.length > 0) {
              var place = places[args.rows[0]]
              if (place.marker) {
                map.panTo(place.coordinate);
                place.marker.bindPopup(place.toponym + ' (<a href="' + place.source + '">Source</a>)').openPopup();
                
                // Clear sequence polylines
                for (idx in sequence) {
                  map.removeLayer(sequence[idx]);
                }
                sequence = [];
                
                var next2 = getNextN(places, args.rows[0], 2);
                var lineNext = [place.coordinate];
                for (idx in next2) {
                  var p = next2[idx];
                  lineNext.push(p.coordinate);
                }
                
                var pLineNext = L.polyline(lineNext, { color: '#00ff00', opacity: 1 });
                sequence.push(pLineNext);
                pLineNext.addTo(map);
                
                var prev2 = getPrevN(places, args.rows[0], 2);
                var linePrev = [place.coordinate];
                for (idx in prev2) {
                  var p = prev2[idx];
                  linePrev.push(p.coordinate);
                }
                
                var pLinePrev = L.polyline(linePrev, { color: '#ff0000', opacity: 1 });
                sequence.push(pLinePrev);
                pLinePrev.addTo(map);
                                                
              } else {
                alert(place.toponym + ' does not have coordinates.');
              }
            }    
          });
        });
      });
*/
