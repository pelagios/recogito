/**
 * The table component of the UI.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @constructor
 */
pelagios.georesolution.TableView = function(tableDiv, opt_edit_callback) {  
  var self = this;
    
  // A custom formatter for Pleiades URIs
  var pleiadesFormatter = function (row, cell, value, columnDef, dataContext) {
    if (value) {
      if (value.uri.indexOf('http://pleiades.stoa.org') == 0) {
        var id =  value.uri.substring(32);
        if (id.indexOf('#') > -1)
          id = id.substring(0, id.indexOf('#'));
        
        var normalizedURI = pelagios.georesolution.Utils.normalizePleiadesURI(value.uri);
        var formatted = '<a href="http://pelagios.org/api/places/' + encodeURIComponent(normalizedURI) + 
                        '" target="_blank" title="' + value.title + '">pleiades:' + id + '</a>';
        
        if (value.coordinate) 
          return formatted;
        else
          return '<a title="Place has no coordinates"><span class="table-no-coords">!</span></a> ' + formatted;
      } else {
        return value;
      }
    }
  }

  var columns = [{ name: '#', field: 'idx', id: 'idx' },
                 { name: 'Toponym', field: 'toponym', id: 'toponym' },
                 { name: 'Worksheet', field: 'worksheet', id: 'worksheet' },
                 { name: 'Place ID', field: 'place', id: 'place' , formatter: pleiadesFormatter },
                 { name: 'Corrected', field: 'place_fixed', id: 'place_fixed', formatter: pleiadesFormatter },
                 { name: 'Comment', field: 'comment', id: 'comment' }];

  var options = { enableCellNavigation: true, enableColumnReorder: false, forceFitColumns: true, autoEdit: false };
    
  this._grid = new Slick.Grid('#table', {}, columns, options);
  this._grid.setSelectionModel(new Slick.RowSelectionModel());
  
  var openCorrectionDialog = function(idx) {
    var prev2 = self.getPrevN(idx, 2);
    var next2 = self.getNextN(idx, 2);
    
    new pelagios.georesolution.DetailsPopup(self._grid.getDataItem(idx), function() {
      self._grid.invalidate();
      if (opt_edit_callback)
        opt_edit_callback();
    }, prev2, next2);
  };
  
  // Double-click brings up modal correction dialog...
  this._grid.onDblClick.subscribe(function(e, args) {
    openCorrectionDialog(args.row);
  });
  
  // ...so does enter
  this._grid.onKeyDown.subscribe(function(e, args) {
    if (e.which == 13) {
      openCorrectionDialog(args.row);
    }
  });

  // Selection in the table selects on the map, too
  this._grid.onSelectedRowsChanged.subscribe(function(e, args) { 
    if (args.rows.length > 0) {
      if (self.onSelectionChanged) {
        var place = self._grid.getDataItem(args.rows[0]);
        self.onSelectionChanged(args, place);
      }
    }
  });

  // Redraw grid in case of window resize
  $(window).resize(function() { self._grid.resizeCanvas(); })
}

/**
 * Removes a specific row from the table.
 * @param {Number} idx the index of the row to remove
 */
pelagios.georesolution.TableView.prototype.removeRow = function(idx) {
  var data = this._grid.getData();
  data.splice(idx, 1);
  this._grid.invalidate();
  this._grid.updateRowCount();
}

/**
 * Selects table rows for a specific gazetteer URI.
 * @param {string} uri the gazetteer URI
 */
pelagios.georesolution.TableView.prototype.selectByPlaceURI = function(uri) {
  // Note: we could optimize with an index, but individual EGDs should be small enough
  var size = this._grid.getDataLength();
  var rows = [];
  for (var i = 0; i < size; i++) {
    var row = this._grid.getDataItem(i);
    if (row.place && row.place.uri == uri)
      rows.push(i);
  }
 
  this._grid.setSelectedRows(rows);
  
  if (rows.length > 0)
    this._grid.scrollRowIntoView(rows[0], true);
}

/**
 * Sets data on the backing SlickGrid.
 * @param {Object} data the data
 */
pelagios.georesolution.TableView.prototype.setData = function(data) {
  this._grid.setData(data);
}

/**
 * Refreshes the backing SlickGrid.
 */
pelagios.georesolution.TableView.prototype.render = function() {
  this._grid.render();
}

pelagios.georesolution.TableView.prototype._getNeighbours = function(idx, n, step) {
  var length = this._grid.getData().length;
  
  if (!n)
    n = 2;
    
  if (!step)
    step = 1;
            
  var neighbours = [];
  var ctr = 1;
  while (neighbours.length < n) {   
    if (idx + ctr * step >= length)
      break;
      
    if (idx + ctr * step < 0)
      break;
             
    var dataItem = this._grid.getDataItem(idx + ctr * step);
    if (dataItem.marker)
      neighbours.push(dataItem);
      
    ctr++;
  }
      
  return neighbours;
}

pelagios.georesolution.TableView.prototype.getNextN = function(idx, n)  {
  return this._getNeighbours(idx, n, 1);
}

pelagios.georesolution.TableView.prototype.getPrevN = function(idx, n)  {
  return this._getNeighbours(idx, n, -1);
}
