define(['georesolution/common', 'common/map', 'georesolution/details/searchresultsFilter'], function(common, MapBase, FilterOverlay) {
  
  var locatedResults = {},
      resultLayers = {},
      filterOverlay; 
                  
      /** List of supported gazetteers **/
      KnownGazetteers = {
        'http://pleiades.stoa.org' : 'Pleiades',
        'http://data.pastplace.org' : 'PastPlace',
        'http://www.imperium.ahlfeldt.se': 'DARE'
      },
      
      /** Helper function that groups search results by gazetteers **/
      groupByGazetteer = function(results) {
        var allGrouped = {};
        
        jQuery.each(results, function(idx, result) {
          var gazetteer = KnownGazetteers[result.uri.substr(0, result.uri.indexOf('/', 7))],
              key = (gazetteer) ? gazetteer : 'Other', 
              group = allGrouped[key];
          
          if (group)
            group.push(result);
          else
            allGrouped[key] = [result];
        });     
        
        return allGrouped
      };
  
  var DetailsMap = function(mapDiv, overlayDiv) {
    var self = this,
        setLayerVisibility = function(name, visible) {
          var layer = resultLayers[name];
          if (layer) {
            if (visible)
              self.map.addLayer(layer);
            else
              self.map.removeLayer(layer);
          }
        };
        
    MapBase.apply(this, [ mapDiv ]);
    
    filterOverlay = new FilterOverlay(jQuery(overlayDiv));
    filterOverlay.on('hideGazetteer', function(gazetteer) {
      setLayerVisibility(gazetteer, false);
    });
    filterOverlay.on('showGazetteer', function(gazetteer) {
      setLayerVisibility(gazetteer, true);
    });
    
    $(mapDiv).on('click', '.gazetteer-id', function(e) {
      self.fireEvent('selectSearchresult', locatedResults[e.target.href].result);
      return false;
    });
  }
  DetailsMap.prototype = Object.create(MapBase.prototype);
  
  DetailsMap.prototype.showSearchresults = function(response) {
    var self = this,
        resultsByGazetteer = groupByGazetteer(response.results);
    
    jQuery.each(resultsByGazetteer, function(gazetteer, results) {
      // Create new layer for each gazetteer
      var layer = L.featureGroup();
      layer.addTo(self.map);
      resultLayers[gazetteer] = layer;
    
      // Now add the result markers
      jQuery.each(results, function(idx, result) {
        var marker;
      
        if (result.coordinate) {
          marker = L.marker(result.coordinate);
          marker.bindPopup(
            '<strong>' + result.title + '</strong>' +
            '<br/>' +
            '<small>' + result.names.slice(0, 8).join(', ') + '</small>' +
            '<br/>' + 
            '<strong>Correct mapping to</strong> <a href="' + result.uri + '" class="gazetteer-id" title="Click to confirm" onclick="return false;"><span class="icon">&#xf14a;</span> ' + common.Utils.formatGazetteerURI(result.uri)) + '</a>';

          layer.addLayer(marker);
          locatedResults[result.uri] = { result: result, marker: marker };
        } 
      });
    });

    filterOverlay.show(response.results.length, response.query, resultsByGazetteer);
  };
  
  DetailsMap.prototype.selectSearchresult = function(uri) {
    var result = locatedResults[uri];
    if (result)
      result.marker.openPopup();
  }
  
  DetailsMap.prototype.fitToSearchresults = function() {
    var self = this, 
        searchBounds,
        annotationBounds = this.getAnnotationBounds();
    
    jQuery.each(resultLayers, function(name, layer) {
      var bounds = layer.getBounds();
      if (bounds.isValid()) {
        if (searchBounds)
          searchBounds.extend(bounds);
        else
          searchBounds = bounds;
      }
    });
    
    if (searchBounds.isValid()) {
      if (annotationBounds.isValid())
        searchBounds.extend(annotationBounds);
      
      this.map.fitBounds(searchBounds, { minZoom: self.getCurrentMinZoom() });
    }
  };
  
  DetailsMap.prototype.clearSearchresults = function() {
    jQuery.each(resultLayers, function(gazetteer, layer) {
      layer.clearLayers();
    });
    filterOverlay.clear();
  };
  
  DetailsMap.prototype.destroy = function() {
    this.clearSearchresults();
    
    locatedResults = {};
    resultLayers = {};
    
    MapBase.prototype.destroy.call(this);
  };
  
  return DetailsMap;
    
});
