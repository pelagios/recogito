define(['georesolution/common', 'common/map'], function(common, MapBase) {
  
  var searchresults = {},
      searchresultsLayer,
      clearResultsButton = $('<div id="clear-results">Clear Results</div>');
  
  var DetailsMap = function(div, opt_active_basemap) {
    var self = this;
    
    MapBase.apply(this, arguments);
    
    searchresultsLayer = L.featureGroup();
    searchresultsLayer.addTo(this.map);
    
    $(div).on('click', '.gazetteer-id', function(e) {
      self.fireEvent('selectSearchresult', searchresults[e.target.href].result);
      return false;
    });
    
    clearResultsButton.hide();
    clearResultsButton.on('click', function() { self.clearSearchresults(); });
    $(div).append(clearResultsButton);
  }
  DetailsMap.prototype = Object.create(MapBase.prototype);
  
  DetailsMap.prototype.addSearchresult = function(result) {
    if (result.coordinate) {
      var marker = L.marker(result.coordinate);
      marker.bindPopup(
        '<strong>' + result.title + '</strong>' +
        '<br/>' +
        '<small>' + result.names.slice(0, 10).join(', ') + '</small>' +
        '<br/>' + 
        '<a href="' + result.uri + '" class="gazetteer-id" title="Click to confirm" onclick="return false;"><span class="icon">&#xf14a;</span> ' + common.Utils.formatGazetteerURI(result.uri)) + '</a>';
      
      searchresults[result.uri] = { result: result, marker: marker };
      searchresultsLayer.addLayer(marker);
      clearResultsButton.show();
    }
  };
  
  DetailsMap.prototype.selectSearchresult = function(uri) {
    var result = searchresults[uri];
    if (result)
      result.marker.openPopup();
  }
  
  DetailsMap.prototype.fitToSearchresults = function() {
    var bounds,
        searchBounds = searchresultsLayer.getBounds(),
        annotationBounds = this.getAnnotationBounds();
    
    bounds = (searchBounds.isValid()) ? searchBounds : annotationBounds;
    if (annotationBounds.isValid())
      bounds.extend(annotationBounds);
      
    if (bounds)
      this.map.fitBounds(bounds);
  };
  
  DetailsMap.prototype.clearSearchresults = function() {
    searchresultsLayer.clearLayers();
    clearResultsButton.hide();
    this.fitToAnnotations();
  };
  
  DetailsMap.prototype.destroy = function() {
    searchresults = {};
    MapBase.prototype.destroy.call(this);
  };
  
  return DetailsMap;
    
});
