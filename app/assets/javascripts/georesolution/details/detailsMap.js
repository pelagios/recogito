define(['georesolution/common', 'common/map', 'georesolution/details/searchresultsControl',  'georesolution/annotationContext'], function(common, MapBase, SearchresultsControl, AnnotationContext) {
  
  var locatedResults = {},
      resultLayers = {},
      resultsControl; 
                  
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
        },
        
        /** HTML template for the annotation popup **/
        popupTemplate = '<div>' +
                        '  <p class="quote">' +
                        '    <span class="pre"></span><em class="toponym"></em><span class="post"></span>' +
                        '  </p>' +
                        '  <p class="matched-to">' +
                        '    <strong class="label"></strong><br/>' +
                        '    <small class="names"></small><br/>' +
                        '    <a class="gazetteer-id" target="_blank"></a>' +
                        '  </p>' +
                        '  <div class="actions">' +
                        '    <div class="btn labeled verify"><span class="icon">&#xf14a;</span> OK</div>' +
                        '    <div class="btn labeled alternatives">Alternatives</div>' +
                        '    <div class="btn labeled skip-next">Next <span class="icon">&#xf105;</span></div>' +
                        '  </div>' +
                        '</div>',
        
        // Popup for the current annotation
        createPopup = function(place, annotationsWithContext) {          
          var annotation,
              status,
              element = jQuery(popupTemplate),
              quoteToponym = element.find('.toponym'),
              quotePre = element.find('.pre'),
              quotePost = element.find('.post'),
              matchedURI = element.find('.gazetteer-id'),
              matchedLabel = element.find('.label'),
              matchedNames = element.find('.names'),
              btnOk = element.find('.btn.verify'),
              btnAlternatives = element.find('.btn.alternatives'),
              btnSkip = element.find('.btn.skip-next');

          if (annotations.length > 0) { // Should always be the case
            annotation = annotationsWithContext[0][0];
            status = annotation.status.toLowerCase();
            context = annotationsWithContext[0][1];
            
            quoteToponym.html(annotation.toponym);
            quoteToponym.addClass(status);
            
            context.fetchContentPreview(function(preview) {
              var pre = AnnotationContext.truncateWordsRight(preview.pre, 4),
                  post = AnnotationContext.truncateWords(preview.post, 4);
                  
              quotePre.html('...' + pre);
              quotePost.html(post + '...');             
            });
            
            matchedLabel.html(place.title + common.Utils.categoryTag(place.category));
            matchedNames.html(place.names.slice(0, 8).join(', '));
            matchedURI.attr('href', place.uri);
            matchedURI.html(common.Utils.formatGazetteerURI(place.uri));
            
            if (status === 'verified')
              btnOk.hide();
            else
              btnOk.click(function() { self.fireEvent('verify'); });
              
            btnAlternatives.click(function() { 
              self.map.closePopup();
              self.fireEvent('findAlternatives'); 
            });
            btnSkip.click(function() { self.fireEvent('skipNext'); }); 

            return element[0];
          }
        };
        
    MapBase.apply(this, [ mapDiv, createPopup ]);
    
    resultsControl = new SearchresultsControl(jQuery(overlayDiv));
    resultsControl.on('hideGazetteer', function(gazetteer) {
      setLayerVisibility(gazetteer, false);
    });
    resultsControl.on('showGazetteer', function(gazetteer) {
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
            '<div class="search-result-popup">' + 
            '  <strong>' + result.title + '</strong>' + common.Utils.categoryTag(result.category) +
            '  <br/><small>' + result.names.slice(0, 8).join(', ') + '</small><br/>' +
            '  <p>' +
            '    <strong>Correct?</strong><br/>Assign to ' + 
            '    <a href="' + result.uri + '" class="gazetteer-id" title="Click to confirm" onclick="return false;">' +
            '      <span class="icon">&#xf14a;</span> ' + common.Utils.formatGazetteerURI(result.uri) + 
            '    </a>' +
            '  </p>' +
            '</div>');

          layer.addLayer(marker);
          locatedResults[result.uri] = { result: result, marker: marker };
        } 
      });
    });

    resultsControl.show(response.results.length, response.query, resultsByGazetteer);
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
    resultsControl.clear();
  };
  
  DetailsMap.prototype.destroy = function() {
    this.clearSearchresults();
    
    locatedResults = {};
    resultLayers = {};
    
    MapBase.prototype.destroy.call(this);
  };
  
  return DetailsMap;
    
});
