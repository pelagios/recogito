pelagios.georesolution.Footer = function(footerDiv) {
  this.element = $(footerDiv).find('#footer-info');
}

pelagios.georesolution.Footer.prototype.setData = function(data) {
  var verified = $.grep(data, function(annotation, idx) { return annotation.status == 'VERIFIED'; })
  var verifiedPercent = verified.length / data.length;
  $(this.element).html(
    data.length + ' Annotations - ' + 
    verified.length + ' Verified (' + 
    (verifiedPercent * 100).toFixed(1) + '%)');
}
