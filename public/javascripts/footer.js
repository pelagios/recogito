pelagios.georesolution.Footer = function(footerDiv) {
  this.element = footerDiv;
}

pelagios.georesolution.Footer.prototype.setData = function(data) {
  var verified = $.grep(data, function(annotation, idx) { return annotation.status == 'VERIFIED'; })
  var verifiedPercent = verified.length / data.length;
  this.element.innerHTML = 
    data.length + ' Annotations. ' + 
    verified.length + ' Verified - ' + 
    (verifiedPercent * 100).toFixed(1) + '% (<a href="logout">Logout</a>).';
}
