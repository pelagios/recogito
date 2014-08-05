annotorious.plugin.Recogito = function(config_opts) {
  /** @private **/
  this.STORE_URI = '/recogito/api/annotations';
  
  this.GDOC_ID = config_opts['gdoc_id'];
  this.GDOC_PART_ID = config_opts['gdoc_part_id'];

  /** @private **/
  this._annotations = [];
}

annotorious.plugin.Recogito.prototype.initPlugin = function(anno) {  
  var self = this;
  anno.addHandler('onAnnotationCreated', function(annotation) {
    self._create(annotation);
  });

  anno.addHandler('onAnnotationUpdated', function(annotation) {
    self._update(annotation);
  });

  anno.addHandler('onAnnotationRemoved', function(annotation) {
    self._delete(annotation);
  });
  
  self._loadAnnotations(anno);
}

/** @private **/
annotorious.plugin.Recogito.prototype._showError = function(error) {
  // TODO proper error handling
  window.alert('ERROR');
  console.log(error);
}

/** @private **/
annotorious.plugin.Recogito.prototype._loadAnnotations = function(anno) {
  var self = this;
	jQuery.getJSON(this.STORE_URI + '?gdoc=' + this.GDOC_ID, function(data) {
	  jQuery.each(data, function(idx, annotation) {
      annotation.src = 'map://openlayers/something';
      if (annotation.comment) {
        annotation.text =  annotation.comment;
        delete annotation.comment;
      }
	    anno.addAnnotation(annotation);
	  });
  });
}

/** @private **/
annotorious.plugin.Recogito.prototype._create = function(annotation) {
  var self = this;
  
  annotation.gdocId = this.GDOC_ID;
  annotation.gdocPartId = this.GDOC_PART_ID;
  
  console.log(annotation);
  
  jQuery.ajax({
    url: this.STORE_URI,
    type: 'POST',
    data: JSON.stringify(annotation),
    contentType: 'application/json',
    success: function(response) {
      annotation.id = response.id;
      console.log(annotation);
    }
  });
}

/**
 * @private
 */
annotorious.plugin.Recogito.prototype._update = function(annotation) {
  var self = this;
  jQuery.ajax({
    url: this.STORE_URI + '/' + annotation.id,
    type: 'PUT',
    data: JSON.stringify(annotation),
    contentType: 'application/json'
  }); 
}

/**
 * @private
 */
annotorious.plugin.Recogito.prototype._delete = function(annotation) {
  jQuery.ajax({
    url: this.STORE_URI + '/' + annotation.id,
    type: 'DELETE'
  });
}
