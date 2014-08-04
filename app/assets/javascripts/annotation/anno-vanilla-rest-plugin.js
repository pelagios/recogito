/**
 * A basic plugin to store annotations on a REST-style HTTP/JSON endpoint. The plugin
 * also augments the annotation popup in case the JSON returned from the endpoint 
 * includes 'username', 'user_avatar' and 'timestamp' fields. A reference 
 * implementation illustrating how your endpoint should behave is in the available
 * at [INSERT HTTP URL].
 */
annotorious.plugin.VanillaREST = function(opt_config_options) {
  /** @private **/
  this._STORE_URI = opt_config_options['endpoint_url'];

  /** @private **/
  this._annotations = [];
  
  /** @private **/
  this._loadIndicators = [];
}

annotorious.plugin.VanillaREST.prototype.initPlugin = function(anno) {  
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

annotorious.plugin.VanillaREST.prototype.onInitAnnotator = function(annotator) {
  var spinner = this._newLoadIndicator();
  annotator.element.appendChild(spinner);
  this._loadIndicators.push(spinner);
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._newLoadIndicator = function() { 
  var outerDIV = document.createElement('div');
  outerDIV.className = 'annotorious-rest-plugin-load-outer';
  
  var innerDIV = document.createElement('div');
  innerDIV.className = 'annotorious-rest-plugin-load-inner';
  
  outerDIV.appendChild(innerDIV);
  return outerDIV;
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._showError = function(error) {
  // TODO proper error handling
  window.alert('ERROR');
  console.log(error);
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._loadAnnotations = function(anno) {
  // TODO need to restrict search to the URL of the annotated
  var self = this;
  jQuery.getJSON(this._STORE_URI + '?contextURL=' + encodeURIComponent(window.location.href), function(data) {
	console.log(data);
	jQuery.each(data, function(idx, annotation) {
	  // if (jQuery.inArray(annotation.id, self._annotations) < 0) {
	    self._annotations.push(annotation.id);
	    anno.addAnnotation(annotation);
	  // }
	});
    
    // Remove all load indicators
    jQuery.each(self._loadIndicators, function(idx, spinner) {
      jQuery(spinner).remove();
    });
  });
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._create = function(annotation) {
  var self = this;
  
  jQuery.ajax({
    url: this._STORE_URI,
    type: 'POST',
    data: JSON.stringify(annotation),
    contentType: 'application/json',
    success: function(response) {
    // TODO error handling if response status != 201 (CREATED)
      annotation.id = response.id;
      annotation.user = response.user;
      annotation.created = response.created;
      annotation.lastModified = response.lastModified;
      console.log(annotation);
    }
  });
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._update = function(annotation) {
  var self = this;
  jQuery.ajax({
    url: this._STORE_URI + '/' + annotation.id,
    type: 'PUT',
    data: JSON.stringify(annotation),
    contentType: 'application/json'
  }); 
}

/**
 * @private
 */
annotorious.plugin.VanillaREST.prototype._delete = function(annotation) {
  jQuery.ajax({
    url: this._STORE_URI + '/' + annotation.id,
    type: 'DELETE'
  });
}
