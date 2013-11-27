pelagios.georesolution.Footer = function(footerDiv) {
  this.element = footerDiv;
}

pelagios.georesolution.Footer.prototype.setData = function(data) {
  var verified = $.grep(data, function(annotation, idx) { 
    return annotation.status == 'VERIFIED';
  });
  
  this.element.innerHTML = data.length + ' Annotations. ' + verified.length + ' Verified. (<a href="logout">Logout</a>).';
}
