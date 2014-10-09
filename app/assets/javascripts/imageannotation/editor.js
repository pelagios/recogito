define(['imageannotation/config'], function(config) {
  
  var map, element,
      top, middle, left, right, bottom,
      window, controls,
      currentAnnotation = false;
  
  var Editor = function(olMap, eventBroker) {    
    map = olMap;
    
    var self = this,
        template = 
          '<div class="editor">' +
          '  <div class="mask top"></div>' +
          '  <div class="middle">' +
          '    <div class="mask left"></div><div class="window"></div><div class="mask right"></div>' +
          '  </div>' +
          '  <div class="mask bottom"></div>' +
          '  <div class="editor-controls">' +
          '    <span class="label">Transcription:</span>' +
          '    <input type="text">' +
          '    <div class="buttons">' +
          '      <button class="button ok"><span class="icon">&#xf00c;</span> OK</button>' +
          '      <button class="button cancel"><span class="icon">&#xf05e;</span> Cancel</button>' +
          '      <button class="button red delete"><span class="icon">&#xf00d;</span> Delete Annotation</button>' +
          '    </div>' +
          '  </div>' +
          '</div>';
      
    element = $(template);
    element.hide();
    
    top = element.find('.top');
    middle = element.find('.middle');
    left = element.find('.left');
    right = element.find('.right');
    bottom = element.find('.bottom');
    
    controls = element.find('.editor-controls');
    input = controls.find('input');
    
    var saveTranscription = function() {
      var transcription = input.val(); 
      currentAnnotation.corrected_toponym = transcription;     
      
      var data = (config.gdoc_part_id) ? 
        '{ "gdoc_part_d": ' + config.gdoc_part_id + ', "corrected_toponym": "' + transcription + '" }' :
        '{ "gdoc_id": ' + config.gdoc_id + ', "corrected_toponym": "' + transcription + '" }';
    
      $.ajax({
        url: '/recogito/api/annotations/' + currentAnnotation.id,
        type: 'PUT',
        data: data,
        contentType : 'application/json',
        success: function(result) {
          hide();
        },
        error: function(result) {
          console.log('ERROR updating annotation!');
        }
      });  
    };
    
    input.keypress(function(e) {
      if (e.which == 13)
        saveTranscription();
    });
    
    controls.find('.ok').click(function() {
      saveTranscription();
    });

    controls.find('.delete').click(function() {
      $.ajax({
        url: '/recogito/api/annotations/' + currentAnnotation.id,
        type: 'DELETE',
        success: function(result) {
          map.removeAnnotation(currentAnnotation.id);
          hide();
        },
        error: function(result) {
          console.log('ERROR deleting annotation!');
        }
      }); 
    });

    controls.find('.cancel').click(function() { hide(); });
        
    window = element.find('.window');
        
    map.on('moveend', function(e) {
      if (currentAnnotation)
        show(currentAnnotation);
    });
        
    $('#annotation-area').append(element);
    eventBroker.addHandler('onEditAnnotation', show);
  };
  
  var show = function(annotation) {
    currentAnnotation = annotation;
    var transcription = (annotation.corrected_toponym) ? annotation.corrected_toponym : annotation.toponym;
    if (transcription)
      controls.find('input').val(transcription);
    else 
      controls.find('input').val('');
 
    setTimeout(function() {
      var bounds = map.toViewportCoordinates(annotation, 50);   
      
      top.height(bounds.top);
      middle.height(bounds.height);
      left.width(bounds.left);
    
      window.width(bounds.width);
    
      right.css('left', (bounds.left + bounds.width) + 'px');
      bottom.css('top', (bounds.top + bounds.height) + 'px');

      controls.css({
        left: (bounds.left + 1) + 'px',
        top: (bounds.top + bounds.height) + 'px',
        minWidth: bounds.width + 'px'
      });
  
      element.show();   
      element.find('.editor-controls input').focus();
    }, 1);
  }
  
  var hide = function() {
    currentAnnotation = false;
    controls.find('input').val(''); 
    element.hide();
  }
  
  return Editor;
  
});
