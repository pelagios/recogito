/** Namespaces **/
var recogito = (window.recogito) ? window.recogito : { };

/**
 * Image annotation interface logic.
 * @param {String} olDiv the DIV holding the OpenLayers viewer
 * @param {Number} gdocId the DB id of the document the image belongs to
 */
recogito.ImageAnnotationUI = function(olDiv, imgUrl, gdocId, width, height) { 
  var map = new OpenLayers.Map(olDiv);
  
  var imageLayer = new OpenLayers.Layer.Image(
    'Image', imgUrl,
    new OpenLayers.Bounds(0, 0, width, height),
    new OpenLayers.Size(width, height), {
      minResolution:0.2,
      maxResolution:2
    });
  
  map.addLayer(imageLayer);
  map.zoomToMaxExtent();
  
  anno.makeAnnotatable(map);
  
  // Toolbar
  var toolMove = $('.move-image'),
      toolAnnotate = $('.annotate-image');
      
  toolMove.click(function(e) {
    toolMove.addClass('selected');
	toolAnnotate.removeClass('selected');
  });
  
  toolAnnotate.click(function(e) {
	toolMove.removeClass('selected');
	toolAnnotate.addClass('selected');
	
	anno.activateSelector(function() {
	  toolMove.addClass('selected');
	  toolAnnotate.removeClass('selected');	  
	});
  });
}