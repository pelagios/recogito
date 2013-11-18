/**
 * The table component of the UI.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @constructor
 */
pelagios.georesolution.TableView = function(tableDiv) {  
  var self = this;
    
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
    
  this._grid = new Slick.Grid('#table', {}, columns, options);
  this._grid.setSelectionModel(new Slick.RowSelectionModel());
  
  // Double-click brings up modal correction dialog
  this._grid.onDblClick.subscribe(function(e, args) {
    var popup = new pelagios.georesolution.DetailsPopup(self._grid.getDataItem(args.row));
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
 * Selects table rows for a specific gazetteer URI.
 * @param {string} uri the gazetteer URI
 */
pelagios.georesolution.TableView.prototype.selectByPlaceURI = function(uri) {
  // Note: we could optimize with an index, but individual EGDs should be small enough
  var size = this._grid.getDataLength();
  var rows = [];
  for (var i = 0; i < size; i++) {
    var row = this._grid.getDataItem(i);
    if (row.gazetteer_uri == uri)
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
