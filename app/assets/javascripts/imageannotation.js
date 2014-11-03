require(['common/eventbroker', 
         'imageannotation/events',
         'imageannotation/viewer/viewer', 
         'imageannotation/drawingCanvas', 
         'imageannotation/storage'], function(EventBroker, Events, Viewer, DrawingCanvas, Storage) {
      
  var eventBroker = new EventBroker(),
  
      /** The viewer (based on OpenLayers 3) **/
      viewer = new Viewer('viewer', eventBroker),
      
      /** The drawing canvas that sits in front of the viewer **/
      drawingCanvas = new DrawingCanvas('drawing-canvas', viewer, eventBroker),
      
      /** Takes care of AJAX-communication with the backend **/      
      storage = new Storage(eventBroker),

      /** Toolbar component shorthands **/
      btnNavigate   = $('.navigate'),
      btnAnnotate   = $('.annotate'),
    
      /** Switches the GUI to navigation mode **/
      switchToNavigate = function() {
        eventBroker.fireEvent(Events.SWITCH_TO_NAVIGATE);
        btnNavigate.addClass('selected');
        btnAnnotate.removeClass('selected');
      },
      
      /** Switches the GUI to annotation mode **/
      switchToAnnotate = function() {
        eventBroker.fireEvent(Events.SWITCH_TO_ANNOTATE);
        btnNavigate.removeClass('selected');
        btnAnnotate.addClass('selected');
      };
  
  // Set up toolbar events
  btnNavigate.click(function(e) { switchToNavigate() });
  btnAnnotate.click(function(e) { switchToAnnotate() });
  
  // Spacebar - mode toggle
  $(document).keyup(function(e) {
    if (e.which == 27)
      eventBroker.fireEvent(Events.ESCAPE);
      
    if (e.target.tagName !== 'INPUT' && e.which == 32) {
      if (btnAnnotate.hasClass('selected'))
        switchToNavigate();
      else
        switchToAnnotate();
    }
  });

  // Ready to start!
  eventBroker.fireEvent(Events.INITIALIZE);
  
});
