pelagios.georesolution.Footer = function(footerDiv) {
  this.element = $(footerDiv).find('#footer-info');
}

pelagios.georesolution.Footer.prototype.setData = function(data) {
  var count = function(status) {
    var list = $.grep(data, function(annotation, idx) { return annotation.status == status; });
    return list.length;
  }
  
  var total = data.length;
  var verified = count('VERIFIED');
  var not_identifyable = count('NOT_IDENTIFYABLE');
  var false_detection = count('FALSE_DETECTION');
  var ignore = count('IGNORE');
  var complete = verified / (total - not_identifyable - false_detection - ignore);
  
  $(this.element).html(
    data.length + ' Annotations &nbsp; ' + 
    '<span class="icon">&#xf14a;</span> ' + verified + ' &nbsp; ' + 
    '<span class="icon">&#xf024;</span> ' + not_identifyable + ' &nbsp; ' + 
    '<span class="icon">&#xf057;</span> ' + false_detection + ' &nbsp;' +
    '<span class="icon">&#xf05e;</span> ' + ignore + ' &nbsp; - &nbsp; ' + 

    (complete * 100).toFixed(1) + '% Complete');
}
