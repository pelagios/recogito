/**
 * The table component of the UI.
 * 
 * Emits the following events:
 * 'selectionChanged' ... when the selection was changed in the table
 * 'update' ............. when an annotation was updated in the details popup
 * 'markedAsFalse' ...... when an annotation was marked as false detection in the details popup
 * 
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @constructor
 */
pelagios.georesolution.TableView = function(tableDiv) {  
  // Inheritance - not the nicest pattern but works for our case
  pelagios.georesolution.HasEvents.call(this);
  
  var self = this,
      statusTemplate = 
        '<div class="table-status">' + 
          '<span class="icon {{current-status-css}}" title="{{current-status-title}}">{{current-status-icon}}</span>' +
            '<span class="table-status-selectors">' +
              '<span class="icon status-btn {{status-1-css}}" title="{{status-1-title}}" data-row="{{row}}" data-status="{{status-1-value}}">{{status-1-icon}}</span>' +
              '<span class="icon edit" title="More..." data-row="{{row}}">&#xf040;</span>' +
            '<span>' +
          '</span>' +
        '</div>';
    
  // A custom table cell formatter for Pleiades URIs
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
          return '<span class="icon empty"></span>' + formatted;
        else
          return '<span title="Place has no coordinates" class="icon no-coords">&#xf041;</span>' + formatted;
      } else {
        return value;
      }
    }
  };
  
  // The table cell formatter for the status column
  var statusFormatter = function (row, cell, value, columnDef, dataContext) {
    if (value) {
      var html = statusTemplate;
      
      if (value == 'VERIFIED') {
        html = html.replace('{{current-status-css}}', 'verified');
        html = html.replace('{{current-status-title}}', 'Verified');
        html = html.replace('{{current-status-icon}}', '&#xf14a;');
        html = html.replace('{{status-1-css}}', 'not-verified');
        html = html.replace('{{status-1-title}}', 'Not Verified');
        html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
        html = html.replace('{{status-1-icon}}', '&#xf059;');
        html = html.replace(/{{row}}/g, row);
      } else if (value == 'NOT_IDENTIFYABLE') {
        html = html.replace('{{current-status-css}}', 'not-identifyable');
        html = html.replace('{{current-status-title}}', 'Not Identifyable');
        html = html.replace('{{current-status-icon}}', '&#xf024;');
        html = html.replace('{{status-1-css}}', 'not-verified');
        html = html.replace('{{status-1-title}}', 'Not Verified');
        html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
        html = html.replace('{{status-1-icon}}', '&#xf059;');        
      } else if (value == 'FALSE_DETECTION') { 
        html = html.replace('{{current-status-css}}', 'false-detection');
        html = html.replace('{{current-status-title}}', 'False Detection');
        html = html.replace('{{current-status-icon}}', '&#xf05e;');
        html = html.replace('{{status-1-css}}', 'not-verified');
        html = html.replace('{{status-1-title}}', 'Not Verified');
        html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
        html = html.replace('{{status-1-icon}}', '&#xf059;');
      } else {
        // 'NOT_VERIFIED'
        html = html.replace('{{current-status-css}}', 'not-verified');
        html = html.replace('{{current-status-title}}', 'Not Verified');
        html = html.replace('{{current-status-icon}}', '&#xf059;');
        html = html.replace('{{status-1-css}}', 'verified');
        html = html.replace('{{status-1-title}}', 'Verified');
        html = html.replace('{{status-1-value}}', 'VERIFIED');
        html = html.replace('{{status-1-icon}}', '&#xf14a;');
        html = html.replace(/{{row}}/g, row);
      }
      
      return html;
    }
  };

  var columns = [{ name: '#', field: 'idx', id: 'idx', width:25 },
                 { name: 'Toponym', field: 'toponym', id: 'toponym' },
                 { name: 'EGD Part', field: 'part', id: 'part' },
                 { name: 'Auto Match', field: 'place', id: 'place' , formatter: pleiadesFormatter },
                 { name: 'Corrected', field: 'place_fixed', id: 'place_fixed', formatter: pleiadesFormatter },
                 { name: 'Status', field: 'status', id: 'status', width:55, formatter: statusFormatter }];

  var options = { enableCellNavigation: true, enableColumnReorder: false, forceFitColumns: true, autoEdit: false };
    
  this._grid = new Slick.Grid('#table', {}, columns, options);
  this._grid.setSelectionModel(new Slick.RowSelectionModel());
  this._grid.onDblClick.subscribe(function(e, args) { self._openDetailsPopup(args.row); });  
  this._grid.onKeyDown.subscribe(function(e, args) {
    if (e.which == 13) {
      self._openDetailsPopup(args.row);
    }
  });

  // Selection in the table is forwarded to event listener
  this._grid.onSelectedRowsChanged.subscribe(function(e, args) { 
    if (args.rows.length > 0) {
      var place = self._grid.getDataItem(args.rows[0]);
      self.fireEvent('selectionChanged', args, place);
    }
  });

  // Redraw grid in case of window resize
  $(window).resize(function() { self._grid.resizeCanvas(); })
  
  // Add status column button handlers
  $(document).on('click', '.status-btn', function(e) { 
    var row = parseInt(e.target.getAttribute('data-row'));
    var status = e.target.getAttribute('data-status');
    
    var annotation = self._grid.getDataItem(row);
    annotation.status = status;
    self._grid.invalidate();
    self.fireEvent('update', annotation);
  });
    
  $(document).on('click', '.edit', function(e) {
    var idx = parseInt(e.target.getAttribute('data-row'));
    self._openDetailsPopup(idx);
  });
}

// Inheritance - not the nicest pattern but works for our case
pelagios.georesolution.TableView.prototype = new pelagios.georesolution.HasEvents();

/**
 * Opens the details popup
 * @private
 */
pelagios.georesolution.TableView.prototype._openDetailsPopup = function(idx) {
  var self = this,
      prev2 = this.getPrevN(idx, 2),
      next2 = this.getNextN(idx, 2);
    
  var popup = new pelagios.georesolution.DetailsPopup(this._grid.getDataItem(idx), prev2, next2);
  popup.on('save', function(annotation) {
    self._grid.invalidate();
    self.fireEvent('update', annotation);
  });
  popup.on('markedAsFalse', function(annotation) {
    self.removeRow(idx);
    self.fireEvent('markedAsFalse', annotation);
  });
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
  
  // Check if there's a '#{rownumber}' URL fragment - and open the popup if so
  if (window.location.hash) {
    var idx = parseInt(window.location.hash.substring(1));
    this._openDetailsPopup(idx);
  }
}

/**
 * Refreshes the backing SlickGrid.
 */
pelagios.georesolution.TableView.prototype.render = function() {
  this._grid.render();
}

/**
 * Returns N neighbours of the annotation with the specified index, based
 * on a (positive or  negative) step value.
 * @param {Number} idx the index of the annotation
 * @param {Number] n the number of neighbours to return
 * @param {step} the step value
 * @return the neighbours
 * @private
 */
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

/**
 * Returns the next N annotations in the list from the specified index.
 * @param {Number} idx the index
 * @param {Number} n the number of neighbours to return
 * @return the next N annotations in the list
 */
pelagios.georesolution.TableView.prototype.getNextN = function(idx, n)  {
  return this._getNeighbours(idx, n, 1);
}

/**
 * Returns the previous N annotations in the list from the specified index.
 * @param {Number} idx the index
 * @param {Number} n the number of neighbours to return
 * @return the previous N annotations in the list
 */
pelagios.georesolution.TableView.prototype.getPrevN = function(idx, n)  {
  return this._getNeighbours(idx, n, -1);
}
