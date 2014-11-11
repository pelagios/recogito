/** Various helpers and common components **/
define(['common/hasEvents'], function(HasEvents) {

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
    
    Utils: Utils
    
  };

});
