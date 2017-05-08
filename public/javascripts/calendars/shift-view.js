// crea un calendario per i turni
var shiftCalendar = new sistCalendar("shift", "view");

var shiftConfGroup = new sistCalendarGroup("shift", "view");

var calendarType = '';
var types = [];
var userAllowed = false;

jQuery(document).ready(function() {

	var json = $.getJSON('renderPersonShiftIds', function(data) {
		
		$.each(data, function(index, element) {
	        types.push(element);
	    });
		console.log("types: "+types );
	    if (types.length > 1) {
	    		userAllowed = true;
				shiftCalendar.selectPopup(userAllowed, types, 'shiftView', shiftCalendar, shiftConfGroup);
		} else {
			if (types.length == 1) {
				calendarType = types;
				shiftConfGroup.calendarChoise = calendarType;
				userAllowed = true;
				createCalendarShiftView(userAllowed, calendarType, shiftCalendar, shiftConfGroup);
				jQuery('h1.title').append(" "+shiftConfGroup.getActivityDescription(types));
			} else {
				userAllowed = false;
				createCalendarShiftView(userAllowed, calendarType, shiftCalendar, shiftConfGroup);			
			}	
		}


	});
	
	// Codice per bottone visuale successiva
		// ----------------------------------------
	jQuery('#next').click(function() {
	  jQuery('#calendar').fullCalendar('next');
	  jQuery('#calendar').fullCalendar('rerenderEvents');
	});
	
	// Codice per bottone visuale precedente
		// ----------------------------------------
	jQuery('#prev').click(function() {
	  jQuery('#calendar').fullCalendar('prev');
	  jQuery('#calendar').fullCalendar('rerenderEvents');
	});
	
	// Codice per bottone visuale giorno odierno
		// ----------------------------------------
	jQuery('#oggi').click(function() {
	  jQuery('#calendar').fullCalendar('today');
	  jQuery('#calendar').fullCalendar('rerenderEvents');
	});
});
