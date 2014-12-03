require(['common/eventbroker', 
         'imageannotation/events',
         'imageannotation/viewer/viewer', 
         'imageannotation/drawingCanvas', 
         'imageannotation/storage',
         'imageannotation/helpWindow'], function(EventBroker, Events, Viewer, DrawingCanvas, Storage, HelpWindow) {
      
  var eventBroker = new EventBroker(),
  
      /** The viewer (based on OpenLayers 3) **/
      viewer = new Viewer('viewer', eventBroker),
      
      /** The drawing canvas that sits in front of the viewer **/
      drawingCanvas = new DrawingCanvas('drawing-canvas', viewer, eventBroker),
      
      /** Takes care of AJAX-communication with the backend **/      
      storage = new Storage(eventBroker),
      
      /** The help popup window **/
      helpWindow = new HelpWindow(),

      /** Toolbar component shorthands **/
      btnNavigate   = $('.navigate'),
      btnAnnotate   = $('.annotate'),
      btnHelp       = $('.help'),
    
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
      },
      
      toggleHelp = function() {
        if (helpWindow.isVisible()) {
          helpWindow.hide();
        } else {
          helpWindow.show();
        }
      };
  
  // Set up toolbar events
  btnNavigate.click(switchToNavigate);
  btnAnnotate.click(switchToAnnotate);
  btnHelp.click(toggleHelp);
  
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
