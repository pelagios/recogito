define(function() {
  
  var SidePanel = function(element) {
    var overlay = jQuery(element),
        btnSlide = jQuery('#btn-slide-toggle'),
        
        slideIn = function(delta) {
          overlay.animate({ left: -delta }, 300);
          btnSlide.html('&#xf054;');
        },
        
        slideOut = function() {
          overlay.animate({ left: 0 }, 300);
          btnSlide.html('&#xf053;');
        };
        
    btnSlide.click(function() {
      var right = overlay.offset().left + overlay.outerWidth();
      if (right > 0)
        slideIn(right);
      else
        slideOut();
    });
  };
  
  return SidePanel;
  
});
