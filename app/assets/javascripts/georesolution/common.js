/** Various helpers and common components **/
define(['common/hasEvents'], function(HasEvents) {

  /**
   * A tag list element to be used in the Details & Batch popups.
   * @param {Element} parent the parent element to attach the list to
   */
  var TagList = function(parent, tags) {
    HasEvents.call(this);
    
    var self = this,
        ul = $('<ul></ul>'),
        tagEditor = false,
        addButton =  $('<button class="button dark">Add Tag</button>');

    // Helper function to add a tag to the list
    var addTag = function(idx, tag) {
      ul.append('<li class="popup-tag">' + tag + '<a title="Remove Tag" data-index="' + idx + '" class="popup-tag-remove icon">&#xf00d;</a></li>'); 
    };
      
    // Helper function to remove a tag from the list
    var removeTag = function(idx) {
      if (tags) {
        tags.splice(idx, 1);
        ul.empty();
        $.each(tags, function(idx, tag) {
        if (tag.length > 0)
          addTag(idx, tag);        
        });
      }
    };
      
    // Initialize the tag list
    if (tags) {
      $.each(tags, function(idx, tag) {
        if (tag.length > 0)
          addTag(idx, tag);        
      });
    }
    parent.append(ul);
    parent.append(addButton);
    
    // Event hanlding: 'Add tag' button
    addButton.click(function(e) {
      if (tagEditor) {
        tagEditor.destroy();
        tagEditor = false;
      } else {
        var offset = addButton.offset();
      
        var onEnter = function(newTags) {
          if (newTags.length > 0) {
            if (!tags)
              tags = [];
         
            $.each(newTags, function(idx, newTag) { 
              addTag(tags.length, newTag); 
              tags.push(newTag);
            });
      
            self.fireEvent('update', tags);
          }
        };
      
        var onEscape = function(editor) { 
          editor.destroy(); 
          tagEditor = false; 
        };
      
        tagEditor = new TagEditor(e.target.offsetParent, e.target.offsetTop - 5, e.target.offsetLeft + e.target.offsetWidth + 5, onEnter, onEscape);
      }
    });
    
    // Event handling: 'Remove tag' icons
    $(parent).on('click', '.popup-tag-remove', function(e) {
      var idx = parseInt($(e.target).data('index'));
      removeTag(idx);
      self.fireEvent('update', tags);
    });
  }
  
  TagList.prototype = new HasEvents();

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
  
    this.element = $('<div class="popup-tag-editor"><input type="text" placeholder="Tags, separated by comma..."></input></div>');
    this.element.css('top', (offsetTop +  3) + 'px');
    this.element.css('left', offsetLeft + 'px');
    this.element.appendTo(parent); 
    this.element.click(function(e) { e.stopPropagation(); });
  
    var textField = this.element.find('input')[0];    
    textField.focus()
    this.element.keydown(function(e) {
      if (e.keyCode == 13) {
        // Enter
        if (onEnter) {
          var tags = textField.value.toLowerCase().split(",");
          onEnter($.map(tags, function(str) { return $.trim(str); }));
        }
        
        self.destroy();
      } else if (e.keyCode == 27) {
        // Escape
        if (onEscape)
          onEscape(self);
        
        self.destroy();
      }    
    }); 
  }

  TagEditor.prototype.destroy = function() {
    $(this.element).remove(); 
  }

  /**
   * Helpers and utility methods.
   */
  var Utils = {
  
    /** Normalizes Pleiades URIs by stripping the trailing '#this' (if any) **/
    normalizePleiadesURI: function(uri) {
      if (uri.indexOf('#this') < 0) {
        return uri;
      } else {
        return uri.substring(0, uri.indexOf('#this'));
      }
    },
    
    formatGazetteerURI: function(uri) {
      // Shorthand
      var format = function(uri, prefix, offset) {
        var id = uri.substring(offset);
        if (id.indexOf('#') > -1)
          id = id.substring(0, id.indexOf('#'));
        return prefix + ':' + id;        
      };
    
      if (uri.indexOf('http://pleiades.stoa.org') == 0)
        return format(uri, 'pleiades', 32);
      else if (uri.indexOf('http://data.pastplace.org/') == 0)
        return format(uri, 'pastplace', 35);
      else if (uri.indexOf('http://www.imperium.ahlfeldt.se') == 0)
        return format(uri, 'dare', 39);
      else
        return uri;
    },
  
    formatCategory: function(category, opt_template) {
      if (!category)
        return '';
      
      var screenName;
    
      if (category == 'SETTLEMENT')
        screenName = 'Settlement';
      else if (category == 'REGION')
        screenName = 'Region';
      else if (category == 'NATURAL_FEATURE')
        screenName = 'Natural Feature'
      else if (category == 'ETHNOS')
        screenName = 'Ethnos';
      else if (category == 'MAN_MADE_STRUCTURE')
        screenName = 'Built Structure';
      else
        screenName = category;
      
      if (opt_template)
        return opt_template.replace('{{category}}', screenName);
      else
        return screenName
    },
    
    categoryTag: function(category)  {
      var longName = Utils.formatCategory(category);
      var shortName = longName;
    
      if (category == 'NATURAL_FEATURE')
        shortName = 'Feature';
      else if (category == 'MAN_MADE_STRUCTURE')
        shortName = 'Structure';
        
      return '<span class="categorytag ' + shortName.toLowerCase() + '" title="' + longName + '">' + shortName + '</span>';
    }
  
  }
  
  return {
    
    HasEvents: HasEvents,
    
    TagList: TagList,
    
    Utils: Utils
    
  };

});
