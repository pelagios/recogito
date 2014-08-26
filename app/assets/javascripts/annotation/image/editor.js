define(['config', 'annotation/image/utils'], function(config, Utils) {
  
  var map, element,
      top, middle, left, right, bottom,
      window, controls,
      currentAnnotation = false;
  
  var Editor = function(olMap) {    
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
          '    <input type="text">' +
          '    <div class="buttons">' +
          '      <button class="button ok">OK</button>' +
          '      <button class="button red delete">DELETE</button>' +
          '      <button class="button cancel">CANCEL</button>' +
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

    controls.find('.ok').click(function() {
      var transcription = controls.find('input').val(); 
            
      var data = (config.gdoc_part_id) ? 
        '{ "gdoc_part_d": ' + config.gdoc_part_id + ', "corrected_toponym": "' + transcription + '" }' :
        '{ "gdoc_id": ' + config.gdoc_id + ', "corrected_toponym": "' + transcription + '" }';
    
      $.ajax({
        url: '/recogito/api/annotations/' + currentAnnotation.id,
        type: 'PUT',
        data: data,
        contentType : 'application/json',
        error: function(result) {
          console.log('ERROR updating annotation!');
        }
      });  
    });

    controls.find('.delete').click(function() {
      $.ajax({
        url: '/recogito/api/annotations/' + currentAnnotation.id,
        type: 'DELETE',
        success: function(result) {
          map.removeAnnotation(currentAnnotation.id);
          self.hide();
        },
        error: function(result) {
          console.log('ERROR deleting annotation!');
        }
      }); 
    });

    controls.find('.cancel').click(function() { self.hide(); });
        
    window = element.find('.window');
        
    $('#annotation-area').append(element);
  };
  
  Editor.prototype.show = function(annotation) {
    currentAnnotation = annotation;
    setTimeout(function() {
      var bounds = map.toViewportCoordinates(Utils.getBounds(annotation), 50);   
      
      top.height(bounds.top);
      middle.height(bounds.height);
      left.width(bounds.left);
    
      window.width(bounds.width);
    
      right.css('left', (bounds.left + bounds.width) + 'px');
      bottom.css('top', (bounds.top + bounds.height) + 'px');

      controls.css({ left: bounds.left + 'px', top: (bounds.top + bounds.height) + 'px' });
  
      element.show();   
    }, 1);
  };
  
  Editor.prototype.hide = function() {
    currentAnnotation = false;
    element.hide();
  };
  
  return Editor;
  
});
