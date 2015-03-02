/** 
 * A global Config object. Not the nicest way to handle this, but
 * we need it as a communication channel between the application and 
 * the host page.
 */
define([], function() { 
  
  var testWebGLSupport = function() {
    if (window.WebGLRenderingContext) {
      // Browser claims to support WebGL, but this check alone is not reliable
      var canvas = document.createElement('canvas');
      var context = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
      return context;
    } else {
      // No luck
      return false;
    }
  }

  /** Annotation marker colours **/
  window.config.MARKER_RED = '#aa0000';
  window.config.MARKER_GREY = '#323232';
  window.config.MARKER_YELLOW = '#bbb000';
  window.config.MARKER_GREEN = '#007700';
    
  /** Stroke colour for highlighted annotations **/
  window.config.MARKER_HI_COLOR = '#777777';
  
  /** Opacity of the semi-transparent parts fo the annotation shape **/
  window.config.MARKER_OPACITY = 0.3;
    
  /** Annotation anchor point circle radius **/    
  window.config.MARKER_CIRCLE_RADIUS = 3;
    
  /** Annotation baseline width **/
  window.config.MARKER_LINE_WIDTH = 2;
  
  /** Shorthand for a (reasonably comprehensive) WebGL support check **/
  window.config.WEBGL_ENABLED = testWebGLSupport();
    
  return window.config;
    
});
