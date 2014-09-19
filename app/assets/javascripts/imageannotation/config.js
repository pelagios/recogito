/** 
 * A global Config object. Not the nicest way to handle this, but
 * we need it as a communication channel between the application and 
 * the host page.
 */
define([], function() { 

  /** Annotation markers stroke colour **/
  window.config.MARKER_COLOR = '#0000aa';
    
  /** Stroke colour for highlighted annotations **/
  window.config.MARKER_HI_COLOR = '#fff000';
  
  /** Opacity of the semi-transparent parts fo the annotation shape **/
  window.config.MARKER_OPACITY = 0.3;
    
  /** Annotation anchor point circle radius **/    
  window.config.MARKER_CIRCLE_RADIUS = 4;
    
  /** Annotation baseline width **/
  window.config.MARKER_LINE_WIDTH = 3;
    
  return window.config;
    
});
