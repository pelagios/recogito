/** Various helpers and common components **/
define(['common/hasEvents'], function(HasEvents) {

  var ul,
      currentTags = [],
  
      addTag = function(index, tag) {
        ul.append('<li class="tag">' + tag +
                  '  <a title="Remove Tag" data-index="' + index + '" class="remove-tag icon">&#xf00d;</a>' +
                  '</li>'); 
      },
               
      removeTag = function(index) {
        currentTags.splice(index, 1);
        ul.empty();
        jQuery.each(currentTags, function(idx, tag) {
          if (tag.length > 0)
            addTag(idx, tag);        
        });
      };

  /**
   * A tag list element to be used in the Details & Batch popups.
   * @param {Element} parent the parent element to attach the list to
   */
  var TagList = function(parent) {    
    var self = this,
        tagEditor = false,

        btnToggleEditor =  jQuery('<button class="button dark">Add Tag</button>'),
        
        closeEditor = function() {
          if (tagEditor) {
            tagEditor.destroy(); 
            tagEditor = false; 
          }
        },
        
        /** Handles ENTER key on tag editor **/
        onEnter = function(newTags) {
          if (newTags.length > 0) {
            // TODO filter out existing tags
            jQuery.each(newTags, function(idx, newTag) { 
              addTag(currentTags.length, newTag); 
              currentTags.push(newTag);
            });
            self.fireEvent('update', currentTags);
            closeEditor();
          }
        };

    ul = jQuery('<ul></ul>');
    parent.append(ul);
    parent.append(btnToggleEditor);
    
    btnToggleEditor.click(function(e) {
      var offset = btnToggleEditor.offset(); 
      if (tagEditor) {
        var tags = tagEditor.getTags();
        if (tags.length > 0) {
          onEnter(tags);
        }
      } else {
        tagEditor = new TagEditor(e.target.offsetParent, e.target.offsetTop - 5, e.target.offsetLeft + e.target.offsetWidth + 5, onEnter, closeEditor);
      }
    });
    
    jQuery(parent).on('click', '.remove-tag', function(e) {
      var idx = parseInt(jQuery(e.target).data('index'));
      removeTag(idx);
      self.fireEvent('update', currentTags);
    });
    
    HasEvents.apply(this);
  }
  TagList.prototype = Object.create(HasEvents.prototype);
  
  /** Opens the editor with a new set of tags **/
  TagList.prototype.show = function(tags) {
    this.clear();
    if (tags) {
      currentTags = tags;
      jQuery.each(tags, function(idx, tag) {
          addTag(idx, tag);        
      });   
    }
  };
  
  TagList.prototype.clear = function() {
    currentTags = [];
    ul.empty();
  }

  /**
   * A tag editor component to be used in the Details & Batch popups.
   * @param {Element} parent the DOM element to attach the editor to 
   * @param {Number} offsetTop the top offset for placing the editor
   * @param {Number} offsetLeft the left offset for placing the editor
   * @param {Function} onEnter a callback for catching the 'ENTER' keypress
   * @param {Function} onEscape a callback for catching the 'Escape' keypress
   */
  var TagEditor = function(parent, offsetTop, offsetLeft, onEnter, onEscape) {
    var self = this;
  
    this.element = $('<div class="tag-editor"><input type="text" placeholder="Tags, separated by comma..."></input></div>');
    this.element.css('top', (offsetTop +  3) + 'px');
    this.element.css('left', offsetLeft + 'px');
    this.element.appendTo(parent); 
    this.element.click(function(e) { e.stopPropagation(); });
  
    var textField = this.element.find('input')[0];    
    textField.focus();
    
    this.getTags =  function() {
      var tags = textField.value.toLowerCase().split(",");
      return jQuery.map(tags, function(str) { return $.trim(str); });    
    };
    
    this.element.keydown(function(e) {
      if (e.keyCode == 13) {
        // Enter
        if (onEnter) {
          onEnter(self.getTags());
        }
        
        self.destroy();
      } else if (e.keyCode == 27) {
        // Escape
        if (onEscape)
          onEscape(self);
        
        self.destroy();
      }    
    }); 
  };

  TagEditor.prototype.destroy = function() {
    $(this.element).remove(); 
  };

  return TagList;

});
