define(['georesolution/common', 'georesolution/details', 'georesolution/batch'], function(common, DetailsPopup, BatchPopup) {

  /**
   * The table component of the UI.
   *   
   * Emits the following events:
   * 'update' ............. when an annotation was updated in the details popup
   * 'mouseover' .......... when the mouse is over an annotation
   * 'mouseout' ........... when the mouse exits an annotation
   * 
   * @param {Element} tableDiv the DIV to hold the SlickGrid table
   * @constructor 
   */
  var TableView = function(tableDiv) {  
    // Inheritance - not the nicest pattern but works for our case
    common.HasEvents.call(this);
  
    var self = this,
        rightClickMenu = new RightClickMenu(),
        contextTooltip = new ContextTooltip(),
        statusValues = [ false, ['VERIFIED'], ['NOT_VERIFIED'], ['IGNORE'], ['FALSE_DETECTION'], ['NO_SUITABLE_MATCH', 'AMBIGUOUS', 'MULTIPLE', 'NOT_IDENTIFYABLE'] ],
        statusIcons = [ '', '&#xf14a;', '&#xf059;', '&#xf05e;', '&#xf057;', '&#xf024;'],
        currentStatusFilterVal = 0,
        options = { enableCellNavigation: true, enableColumnReorder: false, forceFitColumns: true, autoEdit: false },
        columns = [{ name: '#', field: 'idx', id: 'idx', width:34, sortable:true },
                   { name: 'Toponym', field: 'toponym', id: 'toponym', sortable:true, formatter: Formatters.ToponymFormatter },
                   { name: 'EGD Part', field: 'part', id: 'part' },
                   { name: 'Tags', field: 'tags', id: 'tags', formatter: Formatters.TagsFormatter },
                   { name: 'Auto Match', field: 'place', id: 'place' , formatter: Formatters.GazeeteerURIFormatter },
                   { name: 'Corrected', field: 'place_fixed', id: 'place_fixed', formatter: Formatters.GazeeteerURIFormatter },
                   { name: 'Status', field: 'status', id: 'status', headerCssClass: 'table-status-header', width:80, formatter: Formatters.StatusFormatter }];
   
    // Initialize dataView and grid
    this._dataView = new Slick.Data.DataView();
    this._grid = new Slick.Grid('#table', this._dataView, columns, options);
    this._grid.setSelectionModel(new Slick.RowSelectionModel());
    this._dataView.onRowsChanged.subscribe(function(e, args) {
      self._grid.invalidateRows(args.rows);
      self._grid.render();
    });
    $(window).resize(function() { self._grid.resizeCanvas(); });
    
    // Right-click context menu
    tableDiv.oncontextmenu = function(e) {
      if (currentSelection.length > 1) {
        if (rightClickMenu.isShown()) {
          rightClickMenu.hide();
        } else {
          rightClickMenu.show(e.clientX, e.clientY, function() {
            rightClickMenu.hide();
            self._openBatchPopup(currentSelection);
          });
        }
        return false;
      }
    };
    
    // Escape key hides right-click menu
    $(document.body).keydown(function(e) {
      if (e.keyCode == 27)
        rightClickMenu.hide();
    });
    
    // Selection    
    var currentSelection = false;
    this._grid.onSelectedRowsChanged.subscribe(function(e, args) { 
      rightClickMenu.hide();
      if (args.rows.length === 0) {
        currentSelection = false;
      } else {
        currentSelection = args.rows;
        if (args.rows.length == 1) {
          var place = self._grid.getDataItem(args.rows[0]);
          self.fireEvent('selectionChanged', args, place);
        }
      }
    });
  
    // Double click -> Details popup
    this._grid.onDblClick.subscribe(function(e, args) { 
      self._openDetailsPopup(args.row); 
    });  
  
    // Enter key -> Details popup
    this._grid.onKeyDown.subscribe(function(e, args) {
      if (e.which == 13)
        self._openDetailsPopup(args.row);
    });
  
    // Sorting
    this._grid.onSort.subscribe(function(e, args) {    
      var comparator = function(a, b) { 
        var x = a[args.sortCol.field], y = b[args.sortCol.field];
        return (x == y ? 0 : (x > y ? 1 : -1));
      };
      self._dataView.sort(comparator, args.sortAsc);
    });
  
    // Filtering based on status
    var headerDiv = $('.table-status-header');
    headerDiv.append('<span class="icon"></span>');
    this._grid.onHeaderClick.subscribe(function(e, args) {
      if (args.column.field == 'status') {
        currentStatusFilterVal = (currentStatusFilterVal + 1) % statusValues.length;  
        self._dataView.beginUpdate();
        self._dataView.setFilterArgs({ status: statusValues[currentStatusFilterVal] });
        self._dataView.endUpdate();
        self._grid.invalidate();
      
        headerDiv.find(':last-child').replaceWith('<span class="icon">' + statusIcons[currentStatusFilterVal] + '</span>');
      }
    });
  
    // Mouseover, mouseout and select -> forward to event listeners
    this._grid.onMouseEnter.subscribe(function(e, args) {
      var cell = args.grid.getCellFromEvent(e);
      var dataItem = args.grid.getDataItem(cell.row);
      if (cell.cell == 1)
        contextTooltip.show(dataItem.id, e.clientX, e.clientY);
        
      self.fireEvent('mouseover', dataItem);
    });
  
    this._grid.onMouseLeave.subscribe(function(e, args) {
      var row = args.grid.getCellFromEvent(e).row;
      var dataItem = args.grid.getDataItem(row);
      contextTooltip.hide(dataItem.id);
      self.fireEvent('mouseout', dataItem);
    });
  
    // Delegated event handler for status column buttons
    $(document).on('click', '.status-btn', function(e) { 
      var row = parseInt(e.target.getAttribute('data-row'));
      var status = e.target.getAttribute('data-status');    
      var annotation = self._grid.getDataItem(row);
    
      annotation.status = status;
      self._grid.invalidate();
      self.fireEvent('update', annotation);  
    });
    
    // Delegated event handler for 'edit' icon -> Details popup
    $(document).on('click', '.edit', function(e) {
      var idx = parseInt(e.target.getAttribute('data-row'));
      self._openDetailsPopup(idx);
    });
  };

  // Inheritance - not the nicest pattern but works for our case
  TableView.prototype = new common.HasEvents();

  /**
   * Opens the details popup.
   * @param idx the table row index
   * @private
   */
  TableView.prototype._openDetailsPopup = function(idx) {
    var self = this,
        prev2 = this.getPrevN(idx, 2),
        next2 = this.getNextN(idx, 2);
    
    var popup = new DetailsPopup(this._grid.getDataItem(idx), prev2, next2);
    popup.on('update', function(annotation) {
      self._grid.invalidate();
      self.fireEvent('update', annotation);
    });
    
    popup.on('skip-prev', function() {
      if (idx > 0) {
        popup.destroy();
        self._openDetailsPopup(idx - 1); 
      }
    });
    
    popup.on('skip-next', function() {
      if (idx < self._grid.getDataLength() - 1) {
        popup.destroy();
        self._openDetailsPopup(idx + 1);
      }
    });
  };

  /**
   * Opens the batch operations popup.
   * @param {Array.<Number>} indexes the table row indexes
   * @private
   */
  TableView.prototype._openBatchPopup = function(indexes) {
    var self = this,
        annotations = $.map(indexes, function(idx) { return self._grid.getDataItem(idx); }),  
        popup = new BatchPopup(annotations);
      
    popup.on('update', function(annotations) {
      self._grid.invalidate();
      self.fireEvent('update', annotations);
    });
  };

  /**
   * Removes a specific row from the table.
   * @param {Number} idx the index of the row to remove
   */
  TableView.prototype.removeRow = function(idx) {
    var data = this._grid.getData();
    data.splice(idx, 1);
    this._grid.invalidate();
    this._grid.updateRowCount();
  };

  /**
   * Selects table rows for a specific gazetteer URI.
   * @param {string} uri the gazetteer URI
   */
  TableView.prototype.selectByPlaceURI = function(uri) {
    // Note: we could optimize with an index, but individual EGDs should be small enough
    var size = this._grid.getDataLength();
    var rows = [];
    for (var i = 0; i < size; i++) {
      var row = this._grid.getDataItem(i);
    
      var place = (row.place_fixed) ? row.place_fixed: row.place;
      if (place && place.uri == uri) {
        rows.push(i);
      }
    }
 
    this._grid.setSelectedRows(rows);
  
    if (rows.length > 0)
      this._grid.scrollRowIntoView(rows[0], true);
  };

  /**
   * Sets data on the backing SlickGrid DataView.  
   * @param {Object} data the data
   */
  TableView.prototype.setData = function(data) {
    this._dataView.beginUpdate();
    this._dataView.setItems(data);
    this._dataView.setFilter(Filters.StatusFilter);
    this._dataView.endUpdate();
    this._grid.resizeCanvas();
  
    // Check if there's a '#{rownumber}' URL fragment - and open the popup if so
    if (window.location.hash) {
      var idx = parseInt(window.location.hash.substring(1));
      this._openDetailsPopup(idx);
    } 
  };

  /**
   * Refreshes the backing SlickGrid.
   */
  TableView.prototype.render = function() {
    this._grid.render();
  };
  
  /** 
   * Returns the next N annotations in the list from the specified index.
   * @param {Number} idx the index
   * @param {Number} n the number of neighbours to return
   * @return the next N annotations in the list
   */
  TableView.prototype.getNextN = function(idx, n)  {
    return getNeighbours(this._grid, idx, n, 1);
  };

  /**
   * Returns the previous N annotations in the list from the specified index.
   * @param {Number} idx the index
   * @param {Number} n the number of neighbours to return
   * @return the previous N annotations in the list
   */
  TableView.prototype.getPrevN = function(idx, n)  {
    return getNeighbours(this._grid, idx, n, -1); 
  };

  /**
   * Returns N neighbours of the annotation with the specified index, based
   * on a (positive or  negative) step value.
   * @param {Number} idx the index of the annotation
   * @param {Number] n the number of neighbours to return 
   * @param {step} the step value
   * @return the neighbours
   * @private
   */
  var getNeighbours = function(grid, idx, n, step) {
    var length = grid.getData().length;
  
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
             
      var dataItem = grid.getDataItem(idx + ctr * step);
      if (!dataItem)
        break;
      
      if (dataItem.marker) {
        if (step > 0)
          neighbours.push(dataItem);
        else 
          neighbours.unshift(dataItem);
      }
      
      ctr++;
    }
      
    return neighbours;
  };
  
  var RightClickMenu = function() {
    var self = this;
    
    this.template = $('<div class="table-rightclickmenu"><a>Batch-Edit Selected Toponyms</a></div>');  
    this.callback = false;
    this.template.find('a').click(function() { 
      if (self.callback)
        self.callback();
    });
        
    $(document.body).append(this.template);
  };
  
  RightClickMenu.prototype.show = function(x, y, callback) {
    this.template.addClass('visible');
    this.template.css({left: x, top: y }); 
    this.callback = callback;
  };
  
  RightClickMenu.prototype.hide = function() {
    this.template.removeClass('visible');
  };
  
  RightClickMenu.prototype.isShown = function() {
    return this.template.hasClass('visible');
  };
  
  var ContextTooltip = function() {
    this.template = $('<div class="table-context-tooltip"></div>');
    $(document.body).append(this.template);
    this.INTERVAL_MILLIS = 500;
    this.timer = false;
    this.currentId = false;
  };
  
  ContextTooltip.prototype.show = function(id, x, y) {
    var template = this.template;
    this.currentId = id;
    this.timer = setTimeout(function() {
      $.getJSON('api/annotations/' + id, function(a) {
        if (a.context) {
          var startIdx = a.context.indexOf(a.toponym);
          var endIdx = startIdx + a.toponym.length;
          if (startIdx > -1 && endIdx <= a.context.length) {
            var pre = a.context.substring(0, startIdx);
            var post = a.context.substring(endIdx);
            template.html('...' + pre + '<em>' + a.toponym + '</em>' + post + '...');
            template.addClass('visible');
            template.css({left: x, top: y });
          }
        }    
      });
    }, this.INTERVAL_MILLIS);
  };

  ContextTooltip.prototype.hide = function(id) {
    if (id == this.currentId && this.timer) {
      this.template.removeClass('visible');
      clearTimeout(this.timer);
      this.timer = false;
    }
  };

  var Filters = {     
    StatusFilter : function(item, args) {
      if (!args)
        return true;
   
      if (!args.status)
        return true;
    
      return (args.status.indexOf(item.status) > -1);
    }
  };


  var Formatters = {
    /** Custom table cell formatter for Pleiades URIs **/
    ToponymFormatter : function(row, cell, value, columnDef, dataContext) {
      if (value) {
        if (dataContext.comment) {
          return '<span title="' + dataContext.comment + '" class="icon has-comment">&#xf075;</span>' + value;
        } else {
          return '<span class="icon empty"></span>' + value;
        }
      }
    },
    
    GazeeteerURIFormatter : function(row, cell, value, columnDef, dataContext) {  
      if (value) {
        if (value.uri.indexOf('http://pleiades.stoa.org') === 0) {
          var id =  value.uri.substring(32);
          if (id.indexOf('#') > -1)
            id = id.substring(0, id.indexOf('#'));
      
          var normalizedURI = common.Utils.normalizePleiadesURI(value.uri);
          var formatted = '<a href="http://pelagios.org/api/places/' + encodeURIComponent(normalizedURI) + 
                          '" target="_blank" title="' + value.title + 
                          common.Utils.formatCategory(value.category, ' ({{category}})') +
                          '">pleiades:' + id + '</a>';
        
          if (value.coordinate) 
            return '<span class="icon empty"></span>' + formatted;
          else
            return '<span title="Place has no coordinates" class="icon no-coords">&#xf041;</span>' + formatted;
        } else if (value.uri.indexOf('http://www.alraqmiyyat.org') === 0) {
          return '<span class="icon empty"></span><a href="' + value.uri + '" target="_blank" title="' + value.title + '">' +
            'thurayya:' + value.uri.substring(39) + '</a>';
        } else {
          return value.title;
        }
      }
    },

    /** Custom table cell formatter for the tags colum **/
    TagsFormatter : function (row, cell, value, columnDef, dataContext) {
      if (value)
        return value.join(", ");
    },
  
    /** Custom table cell formatter for the status column **/
    StatusFormatter : function (row, cell, value, columnDef, dataContext) {
      var statusTemplate = 
            '<div class="table-status">' + 
              '<span class="icon {{current-status-css}}" title="{{current-status-title}}">{{current-status-icon}}</span>' +
                '<span class="table-status-selectors">' +
                  '<span class="icon status-btn {{status-1-css}}" title="{{status-1-title}}" data-row="{{row}}" data-status="{{status-1-value}}">{{status-1-icon}}</span>' +
                  '<span class="icon status-btn {{status-2-css}}" title="{{status-2-title}}" data-row="{{row}}" data-status="{{status-2-value}}">{{status-2-icon}}</span>' +
                  '<span class="icon edit" title="More..." data-row="{{row}}">&#xf040;</span>' +
                '<span>' +
              '</span>' +
            '</div>';
        
      if (value) {
        var html = statusTemplate;
      
        if (value == 'VERIFIED') {
          html = html.replace('{{current-status-css}}', 'verified');
          html = html.replace('{{current-status-title}}', 'Verified');
          html = html.replace('{{current-status-icon}}', '&#xf14a;');
          html = html.replace('{{status-1-css}}', 'not-verified');
          html = html.replace('{{status-1-title}}', 'Set to Not Verified');
          html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
          html = html.replace('{{status-1-icon}}', '&#xf059;');
          html = html.replace('{{status-2-css}}', 'false-detection');
          html = html.replace('{{status-2-title}}', 'Set to False Detection');
          html = html.replace('{{status-2-value}}', 'FALSE_DETECTION');
          html = html.replace('{{status-2-icon}}', '&#xf057;');
          html = html.replace(/{{row}}/g, row);
        } else if (value == 'FALSE_DETECTION') { 
          html = html.replace('{{current-status-css}}', 'false-detection');
          html = html.replace('{{current-status-title}}', 'False Detection');
          html = html.replace('{{current-status-icon}}', '&#xf057;');
          html = html.replace('{{status-1-css}}', 'verified');
          html = html.replace('{{status-1-title}}', 'Set to Verified');
          html = html.replace('{{status-1-value}}', 'VERIFIED');
          html = html.replace('{{status-1-icon}}', '&#xf14a;');
          html = html.replace('{{status-2-css}}', 'not-verified');
          html = html.replace('{{status-2-title}}', 'Set to Not Verified');
          html = html.replace('{{status-2-value}}', 'NOT_VERIFIED');
          html = html.replace('{{status-2-icon}}', '&#xf059;');
          html = html.replace(/{{row}}/g, row);
        } else if (value == 'IGNORE') { 
          html = html.replace('{{current-status-css}}', 'ignore');
          html = html.replace('{{current-status-title}}', 'Ignore');
          html = html.replace('{{current-status-icon}}', '&#xf05e;');
          html = html.replace('{{status-1-css}}', 'not-verified');
          html = html.replace('{{status-1-title}}', 'Set to Not Verified');
          html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
          html = html.replace('{{status-1-icon}}', '&#xf059;');
          html = html.replace('{{status-2-css}}', 'verified');
          html = html.replace('{{status-2-title}}', 'Set to Verified');
          html = html.replace('{{status-2-value}}', 'VERIFIED');
          html = html.replace('{{status-2-icon}}', '&#xf14a;');
          html = html.replace(/{{row}}/g, row);
        } else if (value == 'NO_SUITABLE_MATCH' || value == 'AMBIGUOUS' || value == 'MULTIPLE' || value == 'NOT_IDENTIFYABLE') {
          html = html.replace('{{current-status-css}}', 'not-identifyable');
          html = html.replace('{{current-status-title}}', 'Not Identifyable');
          html = html.replace('{{current-status-icon}}', '&#xf024;');
          html = html.replace('{{status-1-css}}', 'not-verified');
          html = html.replace('{{status-1-title}}', 'Set to Not Verified');
          html = html.replace('{{status-1-value}}', 'NOT_VERIFIED');
          html = html.replace('{{status-1-icon}}', '&#xf059;'); 
          html = html.replace('{{status-2-css}}', 'false-detection');
          html = html.replace('{{status-2-title}}', 'Set to False Detection');
          html = html.replace('{{status-2-value}}', 'FALSE_DETECTION');
          html = html.replace('{{status-2-icon}}', '&#xf057;');
          html = html.replace(/{{row}}/g, row);
        } else {
          // 'NOT_VERIFIED'
          html = html.replace('{{current-status-css}}', 'not-verified');
          html = html.replace('{{current-status-title}}', 'Not Verified');
          html = html.replace('{{current-status-icon}}', '&#xf059;');
          html = html.replace('{{status-1-css}}', 'verified');
          html = html.replace('{{status-1-title}}', 'Set to Verified');
          html = html.replace('{{status-1-value}}', 'VERIFIED');
          html = html.replace('{{status-1-icon}}', '&#xf14a;');
          html = html.replace('{{status-2-css}}', 'false-detection');
          html = html.replace('{{status-2-title}}', 'Set to False Detection');
          html = html.replace('{{status-2-value}}', 'FALSE_DETECTION');
          html = html.replace('{{status-2-icon}}', '&#xf057;');
          html = html.replace(/{{row}}/g, row);
        }
      
        return html;
      }
    }

  };
  
  return TableView;

});
