/** 
 * A global Config object. Not the nicest way to handle this, but
 * we need it as a communication channel between the application and 
 * the host page.
 */
define([], function() { 

  /** Annotation marker colours **/
  window.config.MARKER_RED = '#aa0000';
  window.config.MARKER_GREY = '#323232';
  window.config.MARKER_YELLOW = '#bbb000';
  window.config.MARKER_GREEN = '#007700';
    
  /** Stroke colour for highlighted annotations **/
  window.config.MARKER_HI_COLOR = '#fff000';
  
  /** Opacity of the semi-transparent parts fo the annotation shape **/
  window.config.MARKER_OPACITY = 0.3;
    
  /** Annotation anchor point circle radius **/    
  window.config.MARKER_CIRCLE_RADIUS = 3;
    
  /** Annotation baseline width **/
  window.config.MARKER_LINE_WIDTH = 2;
    
  return window.config;
    
});
