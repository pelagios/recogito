define(['georesolution/common', 'common/map'], function(common, MapBase) {
  
  var parts = {};
  
  /**
   * The map component of the UI.
   * 
   * Emits the following events:
   * 'select' ... when a place was selected on a map
   * 
   * @param {Element} mapDiv the DIV to hold the Leaflet map
   * @constructor
   */
  var OverviewMap = function(mapDiv) {


    // List of current EGD parts
    // [{ name: { visible: true | false, tags: [ { name: ..., visible: true | false }] }]
    // this._parts = {};
  
    /* Part/tag visibility selector widget
    this._selector = $(selector_template);
    $(this._selector).appendTo(mapDiv);
    $(this._selector).click(function() { self._currentPart += 1; self.redraw(); });
    $(this._selector).on('click', '.map-selector-part', function(e) {
      var part = $(e.target).data('part');
      self._parts[part].visible = !self._parts[part].visible;
      self.redraw(); 
    });
  
    this._currentSequence = [];
  
    this._currentSelection;
  
    this._allAnnotations = [];
    */
    
    MapBase.apply(this, [ mapDiv ]);
  }
  OverviewMap.prototype = Object.create(MapBase.prototype);
  
  OverviewMap.prototype.emphasizePlace = function(annotation, prevN, nextN) {
    /*
    if (annotation.marker && this._isVisible(annotation)) {      
      var style = annotation.marker.options;
      style.radius = style.radius * 2;
      annotation.marker.setStyle(style);
      annotation.marker.bringToFront();
    }
    */
  }

  OverviewMap.prototype.deemphasizePlace = function(annotation, prevN, nextN) {
    /*
    if (annotation.marker && this._isVisible(annotation)) {
      var style = annotation.marker.options;
      style.radius = style.radius * 0.5;
      annotation.marker.setStyle(style);
    }
    */
  }

  /*
  OverviewMap.prototype._isVisible = function(annotation) {
    var part_settings = this._parts[annotation.part];
    if (part_settings) {
      // TODO take visibility of tags into account
      return part_settings.visible;
    } 
  }

  OverviewMap.prototype.removePlaceMarker = function(annotation) {
    if (annotation.marker) {
      this._map.removeLayer(annotation.marker);
      annotation.marker = false;
    }
  }




  */
  
  return OverviewMap;

});
