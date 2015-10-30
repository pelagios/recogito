define(['georesolution/common'], function(common) {

  return {

    /** Formatter for the toponym column **/
    ToponymFormatter : function(row, cell, value, columnDef, dataContext) {
      if (value) {
        var escapedVal = value.replace(/</g, '&lt;').replace(/>/g, '&gt;');
        if (dataContext.comment) {
          return '<span title="' + dataContext.comment + '" class="icon has-comment">&#xf075;</span>' + escapedVal;
        } else {
          return '<span class="icon empty"></span>' + escapedVal;
        }
      }
    },

    /** A cell formatter for gazetteer URIs **/
    GazeeteerURIFormatter: function(row, cell, value, columnDef, dataContext) {
      if (value) {
        var html =
          '<a href="' + value.uri +
          '" target="_blank" title="' + value.title + common.Utils.formatCategory(value.category, ' ({{category}})') +
          '">' + common.Utils.formatGazetteerURI(value.uri) + '</a>';

        if (value.coordinate)
          return '<span class="icon empty"></span>' + html;
        else
          return '<span title="Place has no coordinates" class="icon no-coords">&#xf041;</span>' + html;
      }
    },

    /** A cell formatter for tags **/
    TagFormatter: function (row, cell, value, columnDef, dataContext) {
      if (value)
        return value.join(", ");
    },

    /** A  cell formatter for the status column **/
    StatusFormatter : function (row, cell, value, columnDef, dataContext) {
      var icons = {
            VERIFIED:         '&#xf14a;',
            NOT_VERIFIED:     '&#xf059;',
            FALSE_DETECTION:  '&#xf057;',
            IGNORE:           '&#xf05e;',
            NOT_IDENTIFYABLE: '&#xf024;'
          },

          screenNames = {
            VERIFIED:         'Verified',
            NOT_VERIFIED:     'Not Verified',
            FALSE_DETECTION:  'False Detection',
            IGNORE:           'Ignore',
            NOT_IDENTIFYABLE: 'Not Identfiable'
          },

          template =
            '<div class="table-status">' +
              '<span class="icon {{current-status-css}}" title="{{current-status-title}}">{{current-status-icon}}</span>' +
                '<span class="table-status-selectors">' +
                  '<span class="icon status-btn {{status-1-css}}" title="{{status-1-title}}" data-row="{{row}}" data-status="{{status-1-value}}">{{status-1-icon}}</span>' +
                  '<span class="icon status-btn {{status-2-css}}" title="{{status-2-title}}" data-row="{{row}}" data-status="{{status-2-value}}">{{status-2-icon}}</span>' +
                  '<span class="icon edit" title="More..." data-row="{{row}}">&#xf040;</span>' +
                '<span>' +
              '</span>' +
            '</div>',

          toHTML = function(current, status1, status2) {
            var html = template;
            html = html.replace('{{current-status-css}}', current.toLowerCase().replace('_','-'));
            html = html.replace('{{current-status-title}}', screenNames[current]);
            html = html.replace('{{current-status-icon}}', icons[current]);
            html = html.replace('{{status-1-css}}', status1.toLowerCase().replace('_','-'));
            html = html.replace('{{status-1-title}}', 'Set to ' + screenNames[status1]);
            html = html.replace('{{status-1-value}}', status1);
            html = html.replace('{{status-1-icon}}', icons[status1]);
            html = html.replace('{{status-2-css}}', status2.toLowerCase().replace('_','-'));
            html = html.replace('{{status-2-title}}', 'Set to ' + screenNames[status2]);
            html = html.replace('{{status-2-value}}', status2);
            html = html.replace('{{status-2-icon}}', icons[status2]);
            html = html.replace(/{{row}}/g, row);
            return html;
          };

      if (value == 'VERIFIED')
        return toHTML('VERIFIED', 'NOT_VERIFIED', 'FALSE_DETECTION');
      else if (value == 'FALSE_DETECTION')
        return toHTML('FALSE_DETECTION', 'VERIFIED', 'NOT_VERIFIED');
      else if (value == 'IGNORE')
        return toHTML('IGNORE', 'NOT_VERIFIED', 'VERIFIED');
      else if (value == 'NO_SUITABLE_MATCH' || value == 'AMBIGUOUS' || value == 'MULTIPLE' || value == 'NOT_IDENTIFYABLE')
        return toHTML('NOT_IDENTIFYABLE', 'NOT_VERIFIED', 'FALSE_DETECTION');
      else
        return toHTML('NOT_VERIFIED', 'VERIFIED', 'FALSE_DETECTION');
    }

  };

});
