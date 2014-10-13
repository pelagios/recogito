define([], function() {
	
	var Events = {
		
		INITIALIZE : 'initialize',
    
    SWITCH_TO_NAVIGATE : 'switchToNavigate',
    
    SWITCH_TO_ANNOTATE : 'switchToAnnotate',
    
    EDIT_ANNOTATION : 'editAnnotation',

		ANNOTATION_CREATED : 'annotationCreated',
		
		ANNOTATION_UPDATED : 'annotationUpdated',
		
		ANNOTATION_DELETED : 'annotationDeleted',
    
    MOUSE_OVER_ANNOTATION : 'mouseOverAnnotation',
    
    MOUSE_LEAVE_ANNOTATION : 'mouseLeaveAnnotation',
		
		STORE_ANNOTATIONS_LOADED : 'annotationsLoaded',
		
		STORE_CREATE_ERROR : 'createError',
    
    STORE_UPDATE_ERROR : 'updateError',
    
    STORE_DELETE_ERROR : 'deleteError'
		
	};
	
	return Events;
	
});
