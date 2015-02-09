define(function() {
  
  var SidePanel = function(element) {
    var overlay = jQuery(element),
        btnSlide = jQuery('#btn-slide-toggle');
        
    btnSlide.click(function() {
      var right = overlay.offset().left + overlay.outerWidth();
      if (right > 0) {
        overlay.animate({ left: -right }, 300);
      } else {
        overlay.animate({ left: 0 }, 300);
      }
    });
  };
  
  return SidePanel;
  
});
