define([], function() {
    
  var AnnotationContext = function(_annotation) {
    this.annotation = _annotation;
    this.textPreviewHandlers = [];
    this.cachedTextPreview = false;
  };
  
  AnnotationContext.prototype.fetchContentPreview = function(callback) {   
    var self = this;
     
    if (this.cachedTextPreview) {
      callback(this.cachedTextPreview);
    } else {
      this.textPreviewHandlers.push(callback);
      if (this.textPreviewHandlers.length == 1) {
        jQuery.getJSON('/recogito/api/annotations/' + this.annotation.id, function(a) {
          var startIdx, endIdx, pre, post;

          if (a.context) {
            startIdx = a.context.indexOf(a.toponym);
            endIdx = startIdx + a.toponym.length;
              
            if (startIdx > -1 && endIdx <= a.context.length) {
              pre = a.context.substring(0, startIdx);
              post = a.context.substring(endIdx);
              self.cachedTextPreview = { pre: pre, toponym: self.annotation.toponym, post: post };
            }
          } else {
            self.cachedTextPreview = { toponym: self.annotation.toponym };
          }
          
          jQuery.each(self.textPreviewHandlers, function(idx, handler) {
            handler(self.cachedTextPreview);
          });
        });  
      }
    }
  };
  
  /** Helper to get the first N 'words' from a string **/
  AnnotationContext.truncateWords = function(str, numberOfWords) {
    var trimmed = str.trim(), idx = 0, i;
    for (i = numberOfWords; i >= 0; i--) {
      idx = trimmed.indexOf(' ', idx) + 1;
    }
    return trimmed.substring(0, idx);
  };
  
  /** Helper to get the last N 'words' from a string **/
  AnnotationContext.truncateWordsRight = function(str, numberOfWords) {
    var trimmed = str.trim(), idx = trimmed.length, i;
    for (i = numberOfWords; i >= 0; i--) {
      idx = str.lastIndexOf(' ', idx) - 1;
    }
    return trimmed.substring(idx + 1) + ' ';
  };
  
  return AnnotationContext;
  
});
