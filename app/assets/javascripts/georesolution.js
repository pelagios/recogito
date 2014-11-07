/**
 * The application entry point.
 * @param {Element} tableDiv the DIV to hold the SlickGrid table
 * @param {Element} mapDiv the DIV to hold the Leaflet map
 * @param {String} dataURL the URL from where to retrieve the JSON data
 * @constructor
 */
require(['georesolution/map', 'georesolution/table/table', 'georesolution/footer', 'common/eventbroker', 'georesolution/events'], function(Map, Table, Footer, EventBroker, events) {
   
  var self = this,
      eventBroker = new EventBroker(events),
      relatedLinks = $.grep($('link'), function(element) { return $(element).attr('rel') == 'related'; }),
      dataURL = $(relatedLinks[0]).attr('href'),
      map = new Map(document.getElementById('map')),
      table = new Table(document.getElementById('table'), eventBroker),
      footer = new Footer(document.getElementById('footer'));
        
  // Set up inter-component eventing
  map.on('select', function(annotation) { 
    var place = (annotation.place_fixed) ? annotation.place_fixed : annotation.place;
    table.selectByPlaceURI(place.uri); 
  });
  
  table.on('selectionChanged', function(args, annotation) { 
    var prev3 = table.getPrevN(args.rows[0], 3);
    var next2 = table.getNextN(args.rows[0], 2);
    map.selectPlace(annotation, prev3, next2); 
  });
  
  table.on('update', function(annotations) {
    storeToDB(annotations);
          
    if (!$.isArray(annotations))
      annotations = [ annotations ];
      
    $.each(annotations, function(idx, annotation) {    
  	  if (annotation.marker)
	      map.removePlaceMarker(annotation);
    
      map.addPlaceMarker(annotation);
 	  });
    
    if (self.annotations)
      footer.setData(self.annotations);
  });
  
  table.on('mouseover', function(annotation) { map.emphasizePlace(annotation); });
  table.on('mouseout', function(annotation) { map.deemphasizePlace(annotation); });

  // Fetch JSON data
  $.getJSON(dataURL, function(data) {
    // Flatten & repackage response
    var annotations = [];
    var runningIdx = 0;
        
    if (data.annotations) {
      $.each(data.annotations, function(idx, annotation) {
        annotation.idx = runningIdx;
        annotation.part = "";
        annotations.push(annotation);
        runningIdx++;
      });
    } else if (data.parts) {
      $.each(data.parts, function(idx, part) {
        $.each(part.annotations, function(idx, annotation) {
          annotation.idx = runningIdx;
          annotation.part = part.title;
          annotations.push(annotation);
          runningIdx++;
        });
      });
    }
    
    // Set data on table
    table.setData(annotations, true);
    table.render();
  
    // Set data on map
    $.each(annotations, function(idx, annotation) { map.addPlaceMarker(annotation) });
    
    // Set data on Footer
    footer.setData(annotations);
    
    self.annotations = annotations;
  });

  /**
   * Stores an updated annotation to the database.
   * @private
   */
  function storeToDB(annotations) {
    // Creates the JSON request payload for a single annotation 
    var createPayload = function(annotation)  {
      var payload = {
        'id': annotation.id,
        'status': annotation.status
      };
  
      if (annotation.tags)
        payload.tags = annotation.tags.join(",");
  
      if (annotation.place_fixed)
        payload.corrected_uri = annotation.place_fixed.uri;
    
      if (annotation.comment)
        payload.comment = annotation.comment;      
        
      return payload;
    }
    
    var id, payload;
    if ($.isArray(annotations)) {
      id = '';
      payload = [];
      $.each(annotations, function(idx, annotation) {
        payload.push(createPayload(annotation));
      });
    } else {
      id = annotations.id;
      payload = createPayload(annotations);
    }    
    
    $.ajax({
      type: 'PUT',
      url: 'api/annotations/' + id,
      contentType: 'application/json',
      data: JSON.stringify(payload) 
    });
  };

});
