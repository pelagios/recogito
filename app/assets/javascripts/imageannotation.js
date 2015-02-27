require(['common/eventBroker', 
         'imageannotation/events',
         'imageannotation/viewer/viewer', 
         'imageannotation/drawingCanvas', 
         'imageannotation/storage',
         'imageannotation/helpWindow'], function(EventBroker, Events, Viewer, DrawingCanvas, Storage, HelpWindow) {
      
  jQuery(document).ready(function() {
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
        btnBrightness = $('.brightness'),
        btnContrast   = $('.contrast'),
        btnSignOff    = $('.signoff'),
        btnHelp       = $('.help'),
        
        brightness = $('.tool.brightness input'),
        brightnessConfig = { vertical: true, hideRange: true, min: -100, max: 100, start: 0 },
        brightnessSlider = new Powerange(brightness[0], brightnessConfig);
        brightnessPanel = $('.tool.brightness .panel'),

        contrast = $('.tool.contrast input'),
        contrastConfig = { vertical: true, hideRange: true, min: 0, max: 500, start: 100 },
        contrastSlider = new Powerange(contrast[0], contrastConfig),
        contrastPanel = $('.tool.contrast .panel'),
                    
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
      
        /** Shows hides the help popup **/
        toggleHelp = function() {
          if (helpWindow.isVisible()) {
            helpWindow.hide();
          } else {
            helpWindow.show();
          }
        };
    
    brightnessPanel.hide();
    // Unfortnately, Powerange requires the input to be displayed on init
    // Therefore we're setting the panel to opacity 0 to prevent it from showing on load
    // We're changing this back now
    brightnessPanel.css({ opacity: 1 });
    brightness.change(function(e) {
      var value = jQuery(e.target).val();
      eventBroker.fireEvent(Events.SET_BRIGHTNESS, value);
    });
 
    contrastPanel.hide();
    contrastPanel.css({ opacity: 1 });
    contrast.change(function(e) {
      var value = jQuery(e.target).val();
      eventBroker.fireEvent(Events.SET_CONTRAST, value);
    });
        
    // Set up toolbar events
    btnNavigate.click(switchToNavigate);
    btnAnnotate.click(switchToAnnotate);
    
    btnBrightness.mouseover(function() { brightnessPanel.show(); });
    btnBrightness.mouseout(function() { brightnessPanel.hide(); });
    
    btnContrast.mouseover(function() { contrastPanel.show(); });
    btnContrast.mouseout(function() { contrastPanel.hide(); });    
    
    btnSignOff.click(function(e) { eventBroker.fireEvent(Events.TOGGLE_SIGNOFF); });  
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

    // Update UI on successful signoff-status changes
    eventBroker.addHandler(Events.SIGNOFF_CALLBACK, function(response) {
      var signedOff = response.signed_off,
          icon = btnSignOff.find('.icon'),
          counter = btnSignOff.find('.signoff-count'),
          count = parseInt(counter.text());
        
      if (response.success) {
        if (signedOff) {
          icon.addClass('signed');
          icon.attr('title', 'You signed off this image');
          counter.addClass('signed');
          counter.html(count + 1);
          counter.attr('title', (count + 1) + ' people have signed off this image');
        } else {
          icon.removeClass('signed');
          icon.attr('title', 'Do you think this image is complete? Click to sign it off!');
          if (count < 2)
            counter.removeClass('signed');
          counter.html(count - 1);
          counter.attr('title', (count - 1) + ' people have signed off this image');          
        }      
      }
    });

    // Ready to start!
    eventBroker.fireEvent(Events.INITIALIZE);
  });
  
});
