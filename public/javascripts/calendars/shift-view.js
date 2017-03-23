// crea un calendario per i turni
var shiftCalendar = new sistCalendar("shift", "view");

var shiftConfGroup = new sistCalendarGroup("shift", "view");

var calendarType = '';
var types = [];
var userAllowed = false;

jQuery(document).ready(function() {

    // read the Group Ids of the user and all roles for the groups          
//    var gids = Drupal.settings.sistCalendar.gids['node'];
//    for (var gid in gids) {
//      groupTypes = shiftConfGroup.getTypes4Group(gid);
//      for(one in groupTypes){
//        if($.inArray(groupTypes[one], types) < 0){
//          types.push(groupTypes[one]);
//        }
//      }
//    }
	var json = $.getJSON('http://localhost:9000/shift/renderIds', function(data) {
		console.log("data = " + data);
		$.each(data, function(index, element) {
	        types.push(element);
	    });
	    if (types.length > 1) {
	    	console
				userAllowed = true;
				shiftCalendar.selectPopup(userAllowed, types, 'shiftView', shiftCalendar, shiftConfGroup);
			} else {
				if (types.length == 1) {
					calendarType = types;
	        shiftConfGroup.calendarChoise = calendarType;
					userAllowed = true;
					createCalendarShiftView(userAllowed, calendarType, shiftCalendar, shiftConfGroup);
	        jQuery('h1.title').append(" "+shiftConfGroup.getShiftDescription(types));
				} else {
					userAllowed = false;
					createCalendarShiftView(userAllowed, calendarType, shiftCalendar, shiftConfGroup);			
				}	
			}
		console.log("types =" + types);
		console.log("types.length = " + types.length);

	});
	
	
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

