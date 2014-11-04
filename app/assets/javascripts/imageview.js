require(['common/eventbroker', 
         'imageannotation/events',
         'imageannotation/viewer/viewer', 
         'imageannotation/storage'], function(EventBroker, Events, Viewer, Storage) {
      
  var eventBroker = new EventBroker(),
  
      /** The viewer (based on OpenLayers 3) **/
      viewer = new Viewer('viewer', eventBroker),
      
      /** Takes care of AJAX-communication with the backend **/      
      storage = new Storage(eventBroker);

  eventBroker.fireEvent(Events.INITIALIZE);
  
});
