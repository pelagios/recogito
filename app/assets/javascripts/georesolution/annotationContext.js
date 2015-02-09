define([], function() {
  
  var annotation,
      textPreviewHandlers,
      cachedTextPreview;
  
  var AnnotationContext = function(_annotation) {
    annotation = _annotation;
    textPreviewHandlers = [];
    cachedTextPreview = false;
  };
  
  AnnotationContext.prototype.fetchContentPreview = function(callback) {    
    if (cachedTextPreview) {
      callback(cachedTextPreview);
    } else {
      textPreviewHandlers.push(callback);
      if (textPreviewHandlers.length == 1) {
        jQuery.getJSON('/recogito/api/annotations/' + annotation.id, function(a) {
          var startIdx, endIdx, pre, post;

          if (a.context) {
            startIdx = a.context.indexOf(a.toponym);
            endIdx = startIdx + a.toponym.length;
              
            if (startIdx > -1 && endIdx <= a.context.length) {
              pre = a.context.substring(0, startIdx);
              post = a.context.substring(endIdx);
              
              cachedTextPreview = { pre: pre, toponym: annotation.toponym, post: post };
              jQuery.each(textPreviewHandlers, function(idx, handler) {
                handler(cachedTextPreview);
              });
            }
          }
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
