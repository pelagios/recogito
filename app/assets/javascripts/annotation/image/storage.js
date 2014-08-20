define(['config'], function(config) {
  
  var STORE_URI = '/recogito/api/annotations',
      GDOC_ID = config.gdoc_id,
      GDOC_PART_ID = config.gdoc_part_id;
  
  var Storage = function() {
  };
  
  Storage.prototype.create = function(annotation) {
    annotation.gdoc_id = GDOC_ID;
    
    if (GDOC_PART_ID)
      annotation.gdoc_part_id = GDOC_PART_ID;
    
    $.ajax({
      url: STORE_URI,
      type: 'POST',
      data: JSON.stringify(annotation),
      contentType: 'application/json',
      success: function(response) {
        annotation.id = response.id;
        annotation.last_edit = response.last_edit;
      }
    });
  }
  
  Storage.prototype.loadAll = function(callback) {
	  $.getJSON(STORE_URI + '?gdoc=' + GDOC_ID, function(data) {
      callback(data);
    });     
  }
  
  return Storage;
  
});
