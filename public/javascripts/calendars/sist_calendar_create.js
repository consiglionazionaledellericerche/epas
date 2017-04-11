/*************************************************************************
* Effettua una chiamata REST
*
* - uri: uri a cui risponde il servizio rest
* - type: GET o POST
* - asyncMode: se modalità asincrona (true o false)
* - dataJson: json da inviare
*
*************************************************************************/
function _RestJsonCall (uri, type, asyncMode, dataJson) {

	var result;
	
	$.ajax({
		headers: {
			Accept: "application/json",
    	},
	    dataType: "json",
		contentType: "application/json",
		url: uri,
		type: type,
		data: dataJson,
		async: false,
		success: function(data) {
			result = data;
			}
		})
		.fail(function (jqXHR, textStatus, errorThrown) {
          console.log("error during proxy call= " + textStatus);
          console.log("incoming reperebility Text= " + jqXHR.responseText);
		});
		
	return result;

}

/**************************************************************************
*
* Evita il duplicamento delle emails. Utilizzata nella richiesta
* delle assenze dei turnisti.
*
* - objects: json originale con duplicati
*
/**************************************************************************/
function noDuplicate(objects){
  var objectsNoDup = new Array();
  var objectsEmails = new Array();

  var absentEmails = '';

  var so = JSON.parse(objects);
  //console.log(so);

  for(i=0; i<so['emails'].length; i++){
    if(jQuery.inArray(so['emails'][i]['email'], objectsNoDup) < 0){
      objectsNoDup.push(so['emails'][i]['email']);
    }
  }

  for(i=0; i<objectsNoDup.length; i++){
    objtmp = new Object({"email": objectsNoDup[i]});
    objectsEmails.push(objtmp);
  }
  absentEmails = "{emails:" + JSON.stringify(objectsEmails) + "}";

  //console.log("DupEmails: "+objects);
  //console.log("NoDupEmails: "+absentEmails);
  return absentEmails;
}

/************************************************************************************************/
/****************** CREAZIONE CALENDARIO REPERIBILITA' MODALITA' VIEW ***************************/

/************************************************************************************************/
function createCalendarRepView(allowed, repType, repCalObj, repGrpObj) {

	// defines date variables
	var today = new Date();
	var mToday = today.getMonth();
	var yToday = today.getFullYear();

	// used to be appended to the reperibility  events
	var indexRep = 0;

	// decodifica id -> nome reperibile
	// decodifica id -> colore reperibile
	var idToName  = new Array();
	var idToColor = new Array();
	var idToEmail = new Array();
	var idToMobile = new Array();


	// array of reperibility  events to send
	var reperibilita = new Array();

	var loadedYears = new Array();

	var div = '#external-events';

	// get the reperibility person if the user is allowed
	//----------------------------------------------------
	//console.log("calendarType="+repType);
	if (allowed) {

		//uri to get reperibility persons
		var uriGetRepPersons = repCalObj.getUriRestToGetPersons(repType);

		//console.log('uriGetRepPersons='+uriGetRepPersons);

		var data = new Array();
		data.push('GET');
		data.push(uriGetRepPersons);

		var dataJson = JSON.stringify(data);

		// exec the URI call
		var repPerson = _RestJsonCall (repCalObj.uriProxy, 'POST', false, dataJson);

		// inizialize parameters and create the reperibility person
		// to drop in the calendar
		jQuery.each(repPerson, function (i, event) {
			var color = personColor.shift();
			var name = event.name + " " + event.surname;
			repCalendar.createElement(event.id, name, color).appendTo(div);
			//console.log("person: " + event.email);
			_RestJsonCall
			// get the email and the mobile phone
			idToEmail[event.id] = event.email;
			idToMobile[event.id] = event.mobile;
			idToColor[event.id] = color;
			idToName[event.id] = name;
		});
	} else {

    /*$('<div>', {
      text: "Nessuna persona reperibile associata al Gruppo '" + repConfGroup.getGroupDescription(gid) + "'"
  	}).appendTo('#external-events');*/
	}


    /* initialize the calendar
    -----------------------------------------------------------------*/
    jQuery('#calendar').fullCalendar({
    	loading: function(bool){
    		setTimeout(function(){
    			if(bool){
    				jQuery('#loading').hide();
    			}
    		}, 2000);
    	},

    	// renames months in italian
    	monthNames: repCalObj.monthNames,
    	monthNamesShort: repCalObj.monthNamesShort,

    	//renames days in italian
    	dayNames: repCalObj.dayNames,
    	dayNamesShort: repCalObj.dayNamesShort,

    	// defines the header
    	header: repCalObj.header,

    	// properties
    	selectable: repCalObj.selectable,      // highlight multiple days
    	editable: repCalObj.editable,
    	droppable: repCalObj.droppable, // this allows things to be dropped onto the calendar !!!
    	firstDay:  repCalObj.firstDay,    // start from monday
    	weekMode: repCalObj.weekMode,
    	timeFormat: repCalObj.timeFormat,   // 24- hours clock


    	eventClick: function(calEvent, jsEvent, view) {
      		alert(calEvent.title +'\nTelefono: '+calEvent.mobile+'\nEmail: '+calEvent.eMail);
    	},


    	eventRender: function(event, element) {
        	var inizio = jQuery.fullCalendar(event.start).format("MM");
        	var fine = jQuery.fullCalendar(event.end).format("MM");
        	var currentView = jQuery('#calendar').fullCalendar('getDate').stripTime().format();
        	var currentViewMonth = jQuery.fullCalendar(currentView).format("MM");

        	if( (inizio != currentViewMonth) && (fine != currentViewMonth) && (!event.id.match(/@google.com/g)) ){
          		element.addClass('oldEvent');
        	}
    	},
    	googleCalendarApiKey: 'AIzaSyAEoRhKv77jIyoqHb0VDNWbPdD_BDuEnFk',

    	// define events to be included
    	eventSources: [
        	{
        		events: function (start, end, callback) {
        			// build the URI for the json file
        			var currDate = $('#calendar').fullCalendar('getDate').stripTime().format();
        			var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
        			firstYear = currentYear;

        			// uri to get reperibility
        			var uriParam = currentYear.concat('/01/01/').concat(currentYear.concat('/12/31'));
        			uriRepToGet = repCalObj.getUriRestToGetEntity(repType, uriParam);
	        	    //console.log('uriRepToGet='+uriRepToGet);

        			/* get the reperibility if the user is allowed
        			-----------------------------------------------------------------*/
        			if ((loadedYears.indexOf(currentYear) <  0) && allowed) {
          				loadedYears.push(currentYear);

	       				var data = new Array();
	      				data.push('GET');
	      				data.push(uriRepToGet);
	
						var dataJson = JSON.stringify(data);
	
						var repPeriods = _RestJsonCall (repCalObj.uriProxy, 'POST', false, dataJson);
	
						// create calendar reperibility periods and complete their parameters
	          			jQuery.each(repPeriods, function (i, event) {
	             			event['color'] = idToColor[event.id];
	         				event['personId'] = event.id;
	         				event['title'] = idToName[event.id];
	         				event['eMail'] = idToEmail[event.id];
	         				event['mobile'] = idToMobile[event.id];
	         				event['id'] = 'rep-' + event.id + '-' + indexRep;
	         				indexRep++;
	
	         				jQuery('#calendar').fullCalendar('renderEvent', event, true);
	
	         				// set data.end = data.start for check problem
	         				if (event.end == null) {
	         					event.end = event.start;
	         				}
	        			});
            		}
          		},
        	},
      	],
    });

    /* initialize the external events
    -----------------------------------------------------------------*/
    jQuery('#external-events div.external-event').each(function() {
        // create an Event Object (http://arshaw.com/fullcalendar/docs/event_data/Event_Object/)
        // it doesn't need to have a start or end
        var eventObject = {
                id: 'rep-' + jQuery(this).attr("id"),
                title: jQuery.trim(jQuery(this).text()),  // use the element's text as the event title
                color: jQuery(this).attr("class").split(" ").pop(),
                personId: jQuery(this).attr("id"),
                eMail: idToEmail[jQuery(this).attr("id")],
                mobile: idToMobile[jQuery(this).attr("id")],
        };

        // store the Event Object in the DOM element so we can get to it later
        jQuery(this).data('eventObject', eventObject);
    });
} // FINE createCalendarRepView();





/************************************************************************************************/
/****************** CREAZIONE CALENDARIO REPERIBILITA' MODALITA' ADMIN **************************/
/*
/* allowed: se l'utente ha il permesso di visualizzare il calendario
/*
/************************************************************************************************/

function createCalendarRepAdmin(allowed, repType, repCalObj, repGrpObj){


	// uri to put reperibility in the DB
	var uriProxy = repCalObj.uriProxy;

	//console.log("uriProxy="+uriProxy);

	// uri to get reperibility persons
	var uriGetRepPersons    = repCalObj.getUriRestToGetPersons(repType);

	// defines date variables
	var today = new Date();
	var mToday = today.getMonth();
	var yToday = today.getFullYear();


	// used to be appended to the reperibility  events
	var indexRep = 0;

	// decodifica id -> nome reperibile
	// decodifica id -> colore reperibile
	var idToName  = new Array();
	var idToColor = new Array();
	var idToEmail = new Array();
	var idToMobile = new Array();

	// array of reperibility  events to send
	var reperibilita = new Array();

	var firstYear = 0;
	var loadedYears = new Array();

	var div = '#external-events';

	// legge le persone reperibili che può gestire da EPAS solo se ci sono
	// reperibili associati a quel gruppo
	if (allowed) {
  		//console.log('uriGetRepPersons='+uriGetRepPersons);
  		var data = new Array();
   		data.push('GET');
   		data.push(uriGetRepPersons);

   		var dataJson = JSON.stringify(data);
   		//console.log(dataJson);

	// exec the URI call
       	var repPerson = _RestJsonCall (uriProxy, 'POST', false, dataJson);

       	jQuery.each(repPerson, function (i, event) {

        	var color = personColor.shift();
   	    	var name = event.name + " " + event.surname;
       		repCalendar.createElement(event.id, name, color).appendTo(div);
       		//console.log("person: " + event.email);

      		// get the email and the mobile phone
       		idToEmail[event.id] = event.email;
       		idToMobile[event.id] = event.mobile;
       		idToColor[event.id] = color;
       		idToName[event.id] = name;
      	});

	} else {
      		/*$('<div>', {
       	     text: "Nessuna persona reperibile associata al Gruppo '" + repConfGroup.getGroupDescription(gid) + "'"
       		}).appendTo('#external-events');*/
    }



	/* initialize the external events
	-----------------------------------------------------------------*/
	jQuery('#external-events div.external-event').each(function() {
		// create an Event Object (http://arshaw.com/fullcalendar/docs/event_data/Event_Object/)
		// it doesn't need to have a start or end
		var eventObject = {
			id: 'rep-' + jQuery(this).attr("id"),
			title: jQuery.trim(jQuery(this).text()),  // use the element's text as the event title
			color: jQuery(this).attr("class").split(" ").pop(),
			personId: jQuery(this).attr("id"),
			eMail: idToEmail[jQuery(this).attr("id")],
			mobile: idToMobile[jQuery(this).attr("id")],
		};

		// store the Event Object in the DOM element so we can get to it later
		jQuery(this).data('eventObject', eventObject);

		// make the event draggable using jQuery UI
		jQuery(this).draggable({
			zIndex: 999,
			revert: true,      // will cause the event to go back to its
			revertDuration: 0  //  original position after the drag
		});

	});

	  /* initialize the calendar
	  -----------------------------------------------------------------*/
	jQuery('#calendar').fullCalendar({
		loading: function(bool) {
			setTimeout(function() {
				if(bool) {
					jQuery('#loading').hide();
      			}
			}, 2000);
  	   	},

	// renames months in italian
	monthNames: repCalObj.monthNames,
	monthNamesShort: repCalObj.monthNamesShort,

	//renames days in italian
	dayNames: repCalObj.dayNames,
	dayNamesShort: repCalObj.dayNamesShort,

	// defines the header
	header: repCalObj.header,

	// define the text of the buttton
	buttonText: repCalObj.buttonText,

	// properties
	selectable: repCalObj.selectable,	// highlight multiple days
	editable: repCalObj.editable,
	droppable: repCalObj.droppable, // this allows things to be dropped onto the calendar !!!
	firstDay: repCalObj.firstDay,	// start from monday
	weekMode: repCalObj.weekMode,
	timeFormat: repCalObj.timeFormat,	// 24- hours clock

	// define events to be included
	eventSources: [
		{
			events: function (start, end, callback) {

				var currDate = $('#calendar').fullCalendar('getDate').stripTime().format();
            	var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
				firstYear = currentYear;

				var uriParam = currentYear.concat('/01/01/').concat(currentYear.concat('/12/31'));

				uriRepToGet = repCalendar.getUriRestToGetEntity(repType, uriParam);
				uriFerieToGet = repCalendar.getUriRestToGetAbsence(repType, uriParam);

            	/* get the reperibility
            	-----------------------------------------------------------------*/
				if(loadedYears.indexOf(currentYear) <  0) {
				    loadedYears.push(currentYear);
					//console.log('uriRepToGet='+uriRepToGet);

				    var data = new Array();
					data.push('GET');
					data.push(uriRepToGet);
					//console.log("leggo reperibilità da "+uriRepToGet);

					var dataJson = JSON.stringify(data);

					// exec the URI call
       				var repPeriods = _RestJsonCall (uriProxy, 'POST', false, dataJson);

					// create the reperibility periods event on the calendar
       				jQuery.each(repPeriods, function(i, event) {
					   	var startEv = event.start;
        				var endEv = event.end;
        				var tempStartMonth = startEv.split("-");
        				var tempEndMonth = endEv.split("-");

        				var startMonth = tempStartMonth[1];
        				var endMonth = tempEndMonth[1];

							// divides the periods if it covers 2 months
        				if (startMonth != endMonth) {
          					var lastDayPreMonth = new Date(currentYear, startMonth, 0).getDate();
          					var event1 = {
                  				'color': idToColor[event.id],
              					'personId': event.id,
              					'title': idToName[event.id],
              					'eMail': idToEmail[event.id],
              					'mobile': idToMobile[event.id],
              					'id': 'rep-'+event.id+'-'+indexRep+"-1",
              					'start': event.start,
              					'end': currentYear+"/"+startMonth+"/"+lastDayPreMonth,
          					}
          					if (event1.end == null) {
                  				event1.end = event1.start;
                  				event1['allDay'] = 'true';
          					} else {
                  				event1['allDay'] = 'false';
          					}
          					indexRep++;

          					var event2 = {
          						'color': idToColor[event.id],
          						'personId': event.id,
              					'title': idToName[event.id],
              					'eMail': idToEmail[event.id],
              					'mobile': idToMobile[event.id],
              					'id': 'rep-'+event.id+'-'+indexRep+"-2",
              					'start': currentYear+"/"+endMonth+"/01",
              					'end': event.end,
          					}
          					if (event2.end == null) {
                  				event2.end = event2.start;
                  				event2['allDay'] = 'true';
          					} else {
                  				event2['allDay'] = 'false';
          					}

          					indexRep++;
          					jQuery('#calendar').fullCalendar('renderEvent', event1, true);
          					jQuery('#calendar').fullCalendar('renderEvent', event2, true);
        				} else {
          					event['color'] = idToColor[event.id];
          					event['personId'] = event.id;
          					event['title'] = idToName[event.id];
          					event['eMail'] = idToEmail[event.id];
          					event['mobile'] = idToMobile[event.id];
          					event['id'] = 'rep-'+event.id+'-'+indexRep;

         					jQuery('#calendar').fullCalendar('renderEvent', event, true);
          					indexRep++;

          					// set data.end = data.start for check problem
          					if (event.end == null) {event.end = event.start;}
        				}
       				});


				    /* get the holidays
				    ---------------------------------------------------------------------*/
				    //console.log('uriFerieToGet='+uriFerieToGet);
				    var data = new Array();
  					data.push('GET');
  					data.push(uriFerieToGet);
  					//console.log("leggo assenze da "+uriFerieToGet);

  					var dataJson = JSON.stringify(data);
					// exec the URI call
    				var absentPerson = _RestJsonCall (uriProxy, 'POST', false, dataJson);

      				var indexVac = 0;
      				jQuery.each(absentPerson, function (i, event) {
		      			event['color'] = 'LEMONCHIFFON';
		      			event['textColor'] = 'red';
		      			event['borderColor'] = 'red';
		      			event['personId'] = event.id;
		      			event['title'] = 'Assenza ' + idToName[event.id];
		      			event['id'] = 'assenza-' + event.id + '-' + indexVac;
		
		      			jQuery('#calendar').fullCalendar('renderEvent', event, true);
		      			indexVac++;
      				});
      			}
    		}
        },// end events
        		/*{
        			url: 'https://www.google.com/calendar/feeds/it.italian%23holiday%40group.v.calendar.google.com/public/basic',
        			color: 'red',
        			textColor: 'white'
        		}*/
    ],

	drop: function(date, allDay) { // this function is called when something is dropped
	// retrieve the dropped element's stored Event Object
	var originalEventObject = jQuery(this).data('eventObject');
	
	// we need to copy it, so that multiple events don't have a reference to the same object
	var copiedEventObject = jQuery.extend({}, originalEventObject);
	
	// assign it the date that was reported
	// and adds a progressive number i order to avoid repeated events
	copiedEventObject.start = date;
	copiedEventObject.allDay = allDay;
	copiedEventObject.id = copiedEventObject.id.concat('-').concat(indexRep);
	indexRep++;
	
	// render the event on the calendar
	// the last `true` argument determines if the event "sticks" (http://arshaw.com/fullcalendar/docs/event_rendering/renderEvent/)
	jQuery('#calendar').fullCalendar('renderEvent', copiedEventObject, true);

    // set data.end = data.start for check problem
    if (copiedEventObject.end == null) {copiedEventObject.end = copiedEventObject.start;}

    	var cEOstart = copiedEventObject.start;
		var ymCEO = jQuery.fullCalendar.formatDate(cEOstart, "yyyy").toString().concat(copiedEventObject.start.getMonth());
		var ymT = yToday.toString().concat(mToday);

		if (jQuery.fullCalendar.formatDate(cEOstart, "yyyy").toString().concat(copiedEventObject.start.getMonth()) < yToday.toString().concat(mToday)) {
			// reperibility of the previous month cannot be modified from this screen
			/*alert ('ERRORE!\nNon e possibile modificare la reperibilita del mese precedente');
          	jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);*/
		} else {
			// check if the 'reperibilty ' overlap a ferie o others reperibility
			var obj = jQuery('#calendar').fullCalendar('clientEvents');
			jQuery.each(obj, function(i, val) {
				if(val.id.toString().match(/assenza/g) && val.personId.toString() == copiedEventObject.personId.toString()) {
					if ((val.start.toString() == date.toString()) || (date >= val.start && date <= val.end)) {
						alert("Il reperibile e' assente!!!");
						jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
						return false;
					}
				}
				else if(val.id.toString().match(/rep/g)  && (val.personId.toString() != copiedEventObject.personId.toString())) {
					if ((val.start.toString() == date.toString()) || (date >= val.start && date <= val.end)) {
						alert("La reperobilita di "+copiedEventObject.title+" si sovrappone con quella di "+val.title.toString())
						jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
						return false;
					}
				}
			});
		}
	},

	eventClick: function(calEvent, jsEvent, view) {
		if (calEvent.id.toString().match(/rep/g)) {
			alert(calEvent.title +'\nTelefono: '+calEvent.mobile+'\nEmail: '+calEvent.eMail);
		} else {
			alert(calEvent.title +' \ninizio ' +jQuery.fullCalendar.formatDate(calEvent.start, "dd-MM-yyyy")+ '\nfine '+jQuery.fullCalendar.formatDate(calEvent.end, "dd-MM-yyyy"));
		}

		// change the border color just for fun
		jQuery(this).css('border-color', 'red');

	},

	eventMouseover: function(event, jsEvent, view){
		if(event.id.indexOf('rep-') != '-1'){
			// Add remove-event button
			$(this).find(".fc-event-title").append($("<a>", {
				'href': '#',
				'class': 'remove-event',
				'html': '<img src=\"'+'/public/images/remove-button-cross.png\"></img>',
			}));


			// On click remove-event
			$(".remove-event").click(function(e) {
				e.stopPropagation();
				var conf = confirm('Sei sicuro di voler cancellare il turno di '+event.title+'?');
				if(conf){
					$('#calendar').fullCalendar('removeEvents', event.id);
				}
			});
		}
	}, // end eventMouseover


	eventMouseout: function(event, jsEvent, view){
		$(this).find(".remove-event").remove();
	}, // end eventMouseout
	

	eventDrop: function(event, dayDelta, minuteDelta, allDay, revertFunc) {
		var eventStart = event.start;
		var eventStartM = (eventStart.getMonth() == 0) ? eventStart.getMonth().toString().concat('0') : eventStart.getMonth();
		var todayM = (mToday == 0) ? mToday.toString().concat('0') : mToday;


		var ymEvent = jQuery.fullCalendar.formatDate(eventStart, "yyyy").toString().concat(eventStartM);
		var ymT = yToday.toString().concat(todayM);


		// set data.end = data.start for check problem
		if (event.end == null) {event.end = event.start;}
			
		// holiday events cannot be modified
		if (event.id.toString().match(/assenza/g)) {
			alert ('Da questa interfaccia non e possibile modificare le assenze!');
			revertFunc();
		} else if (jQuery.fullCalendar.formatDate(eventStart, "yyyy").toString().concat(eventStartM) < yToday.toString().concat(todayM)) {
			// reperibility of the previous month cannot be modified from this screen
			alert ('ERRORE!\nNon e possibile modificare la reperibilita del mese precedente');
			revertFunc();
			return false;
		} else {
			// check if ferie and reperibility  of a certain person overlap
			var obj = jQuery('#calendar').fullCalendar('clientEvents');
			jQuery.each(obj, function(i, val) {
				
				if (((event.end >= val.start) && (event.start <= val.start))  ||
						((event.end >= val.start) && (event.end <= val.end)) ||
						((event.start <= val.end) && (event.end >= val.end)) ||
						((event.end == null) && (event.start >=val.start) && (event.start <= val.end))) {
					// check for the ferie
					if (val.id.toString().match(/assenza/g) && val.personId.toString() == event.personId.toString()) {
						alert("Impossibile spostare la reperibilita.\n Il reperibile e assente!");
						revertFunc();
						return false;
					}
					else if(val.id.toString().match(/rep/g)  && (val.personId.toString() != event.personId.toString())) {
						alert("Impossibile spostare la reperibilita.\nLa reperibilita di "+event.title+" si sovrappone con quella di "+val.title)
						revertFunc();
						return false;
					}
				}
			});
		}
	},

	eventResize: function(event, dayDelta, minuteDelta, revertFunc) {
		// set data.end = data.start for check problem
		if (event.end == null) {event.end = event.start;}
		
		// holidays events cannot be modified
		if (event.id.toString().match(/assenza/g)) {
			alert ('Da questa interfaccia non e possibile modificare le assenze!');
			revertFunc();
		} else if (event.end.getMonth() != mToday) {
			// reperibility of the previous month cannot be modified from this screen
			//alert ('ERRORE!\nE possibile modificare la reperibilita solo del mese corrente');
			//revertFunc();
		} else {
			// check if ferie and reperibility  of a certain person overlap
			var obj = jQuery('#calendar').fullCalendar('clientEvents');
			jQuery.each(obj, function(i, val) {
				if ((event.end >= val.start) && (event.start < val.start)) {
					if (val.id.toString().match(/rep/g)) {
						alert("La reperibilita di "+event.title+" si sovrappone con quella di "+val.title.toString());
						revertFunc();
					}
					if (val.id.toString().match(/assenza/g) && val.personId.toString() == event.personId.toString()) {
						alert("La reperibilita di "+event.title+" si sovrappone con una sua assenza");
						revertFunc();
					}
				}
			});
		}
	},

	eventRender: function(event, element) {
		var inizio = jQuery.fullCalendar(event.start).format("MM");
		var fine = jQuery.fullCalendar(event.end).format("MM");
		var currentView = jQuery('#calendar').fullCalendar('getDate').stripTime().format();
		var currentViewMonth = jQuery.fullCalendar(currentView).format("MM");

		if( (inizio != currentViewMonth) && (fine != currentViewMonth) && (!event.id.match(/@google.com/g)) ){
			element.addClass('oldEvent');
		}
	},
	
	});
} // FINE createCalendarRepAdmin();





  /************************************************************************************************/
  /****************** CREAZIONE CALENDARIO TURNI MODALITA' VIEW **************************/
  /************************************************************************************************/
function createCalendarShiftView(allowed, shiftType, shiftCalObj, shiftGrpObj){


	// defines date variables
	var today = new Date();
	var dToday = today.getDate();
	var mToday = today.getMonth();
	var yToday = today.getFullYear();

	// used to be appended to the reperibility  events
	var indexRep = 0;

	// decodifica id -> nome reperibile
	// decodifica id -> colore reperibile
	var idToName  = new Array();
	var shiftToColor = new Array();
	var idToMobile = new Array();
	var idToEmail = new Array();
	var idToShift = new Array();

	// array containing jolly persons with their shift
	var jollyPersons = new Array();

	var tipi = new Array();
    var turni = new Array();

	// prende i colori per visualizzare le persone nel calendario
	var shiftColor = shiftCalObj.getPersonColor(false);
	var jollyColor = shiftCalObj.getPersonColor(true);
	console.log("shiftType: "+shiftType);
	// tipologia di turni da leggere dal DB
	var tipoTurni = shiftGrpObj.getShiftFromType(shiftType);
	console.log("tipoTurni: "+tipoTurni);

	var firstYear = 0;
    var loadedYears = new Array();

	// per ogni turno legge i turnisti dal DB EPAS
    	// e crea gli elementi droppabili nel calendario

	// div contenente le persone in turno
    var divContainer = '#external-events';

	// Per ogni turno legge le persone di quel turno
	//-----------------------------------------------
	j = 0;
	while (j < tipoTurni.length) {
   		var tipoTurno = tipoTurni[j];
   		var color = shiftColor.shift();
   		var div = '#' + tipoTurno;

   		$('<span>', {
  			class: "titolo-external",
   			text: 'Turno ' + tipoTurno
  		}).appendTo(divContainer);
   		$('<div>', {
   			id: tipoTurno,
   		}).appendTo(divContainer);

   		// uri REST per leggere le persone del turno 'tipoTurno'
   		uriGetShiftPersons = shiftCalObj.getUriRestToGetPersons(tipoTurno);
   		//console.log('uriGetShiftPersons='+uriGetShiftPersons);

   		// exec the URI call
       	var shiftPerson = _RestJsonCall (uriProxy + uriGetShiftPersons, 'GET', false, {});
       	//console.log("shiftPerson="+shiftPerson);

        jQuery.each(shiftPerson, function (i, event) {
       		var name = event.name + " " + event.surname;
       		if (!event.jolly) {
      			 shiftCalObj.createShiftElement(event.id, name, tipoTurno, color).appendTo(div);
      			 idToShift[event.id] = tipoTurno;
      			 shiftToColor[tipoTurno] = color;
       		} else {
               		if (jollyPersons.hasOwnProperty(event.id)) {
             			var turni = jollyPersons[event.id];
              			turni.push(tipoTurno);
               			jollyPersons[event.id] = turni;
              		} else {
              			jollyPersons[event.id] = new Array(tipoTurno);
              		}
               		idToShift[event.id] = "J";
               		shiftToColor["J"] = jollyColor;
       		}

       		// get the email and the mobile phone
       		idToEmail[event.id] = event.email;
       		idToMobile[event.id] = event.mobile;
       		idToName[event.id] = name;
  		})

   		j++;
   		tipoTurno = '';
	}

	$('<br />').appendTo(divContainer);
	$('<hr />').appendTo(divContainer);
	$('<span>', { class: 'titolo-external', text: 'Personale sostituto' }).appendTo(divContainer);


	//ATTENZIONE!! si da comunque per scontato che il reperibile sia 1 solo!!!! POI CAMBIARE!!!!!
	// non torna id='J'
	for (var id in jollyPersons) {
    	$('<div>', { id: 'J' }).appendTo(divContainer);
    	shiftCalendar.createShiftElement(id, idToName[id], "J", jollyColor).appendTo("#J");
	}



	/* initialize the external events
	   -----------------------------------------------------------------*/
	jQuery(' div.external-event').each(function() {
		// create an Event Object (http://arshaw.com/fullcalendar/docs/event_data/Event_Object/)
		// it doesn't need to have a start or end

		var classAttr = new Array();
  		classAttr = jQuery(this).attr("class").split(" ");

  		var eventObject = {
   			id: 'turno-' + jQuery(this).attr("id"),
   			title: jQuery.trim(jQuery(this).text()),  // use the element's text as the event title
   			personId: jQuery(this).attr("id"),
   			shiftType: classAttr[2],
   			eMail: idToEmail[jQuery(this).attr("id")],
   			mobile: idToMobile[jQuery(this).attr("id")],
   			color: classAttr[3],
  		};
  		idToName[jQuery(this).attr("id")] = jQuery.trim(jQuery(this).text());
  		shiftToColor[eventObject.shiftType] = eventObject.color;

		// store the Event Object in the DOM element so we can get to it later
		jQuery(this).data('eventObject', eventObject);
	});

	/* initialize the calendar
	-----------------------------------------------------------------*/
	jQuery('#calendar').fullCalendar({
		loading: function(bool){
       		setTimeout(function(){
       			if(bool){
       				jQuery('#loading').hide();
       			}
       		}, 2000);
      	},
		// renames months in italian
		monthNames: shiftCalObj.monthNames,
		monthNamesShort: shiftCalObj.monthNamesShort,

		//renames days in italian
		dayNames: shiftCalObj.dayNames,
		dayNamesShort: shiftCalObj.dayNamesShort,

		// defines the header
		header: shiftCalObj.header,

		// define the text of the buttton
		buttonText: shiftCalObj.buttonText,

		// properties
		selectable: 	shiftCalObj.selectable,	// highlight multiple days
		editable: 	shiftCalObj.editable,
		droppable: 	shiftCalObj.droppable, 	// this allows things to be dropped onto the calendar !!!
		firstDay: 	shiftCalObj.firstDay,		// start from monday
		weekMode: 	shiftCalObj.weekMode,
		timeFormat: 	shiftCalObj.timeFormat,

		// define events to be included
      		eventSources: [
        	{
          		events: function (start, end, callback) {
            			var currDate = $('#calendar').fullCalendar('getDate').stripTime().format();
            			var currentYear = jQuery.fullCalendar(currDate).format("YYYY");
            			firstYear = currentYear.concat(jQuery.fullCalendar(currDate).format("MM"));

				  	// build the URI for the json file
				  	var uriParam = currentYear.concat('/01/01/').concat(currentYear.concat('/12/31'));
	
	        		/* get the work shift
	        		----------------------------------------------------------------- */
				  	var datas = new Array();
				  	if (loadedYears.indexOf(currentYear) <  0) {
				  		loadedYears.push(currentYear);
				  		j = 0;
				  		while (j < tipoTurni.length) {
				  			var tipoTurno = tipoTurni[j];
	
				  			uriShiftToGet = shiftCalObj.getUriRestToGetEntity(tipoTurno, uriParam);
				  			//console.log('+++uriShiftToGet='+uriShiftToGet);
	
				  			var data = new Array();
				  			data.push('GET');
	            			data.push(uriShiftToGet);
	
	            			var dataJson = JSON.stringify(data);
	
	            			// exec the URI call
	            			var shiftPerson = _RestJsonCall (shiftCalObj.uriProxy, 'POST', false, dataJson);
	            			console.log("colore="+shiftToColor[tipoTurno]);
	            			
	                    	jQuery.each(shiftPerson, function (i, event) {
	                    		event['color'] = shiftToColor[tipoTurno];
	                    		event['shiftType'] = tipoTurno;
	                    		event['eMail'] = idToEmail[event.id];
	                    		event['mobile'] = idToMobile[event.id];
	                    		if (event.cancelled == 'false') {
	                    			event['start'] = jQuery.fullCalendar.moment(event.start.concat(' ').concat(event.ttStart));
	                    			event['end'] = jQuery.fullCalendar.moment(event.end.concat(' ').concat(event.ttEnd));
	                    			event['title'] = event.shiftSlot + ' -- ' + idToName[event.id];
	
	                    			//event['allDay'] = false; // If true, shows event time in the event title
	                    			event['cancelled'] = false;
	                    			event['personId'] = event.id;
	                    			event['shiftHour'] = event.ttStart;
	                    		} else {
	                    			event['title'] = 'Turno ' + tipoTurno + '\n\rANNULLATO';
	                    			event['start'] = jQuery.fullCalendar.moment(event.start);
	                    			event['end'] = jQuery.fullCalendar.moment(event.end);
	                    			event['allDay'] = true;
	                    			event['className'] = "del-event";
	                    			event['cancelled'] = true;
	                    		}
	                    		event['id'] = 'turno-' + event.id + '-' + indexRep;
	                    		indexRep++;
	                    		
							jQuery('#calendar').fullCalendar('renderEvent', event, true);
	
							// set data.end = data.start for check problem
							if (event.end == null) {
								event.end = event.start;
							}
                    	});

                    	j++;
                    	tipoTurno = '';
			  		}
				  }
			    }
         	},

         	/*{
              		url: 'https://www.google.com/calendar/feeds/it.italian%23holiday%40group.v.calendar.google.com/public/basic',
             		color: 'red',
              		textColor: 'white'
         	}*/
      		],

      	eventClick: function(calEvent, jsEvent, view) {
			alert('\ncolor: '+calEvent.color+ '\n '+calEvent.title +' \ninizio ' +jQuery.fullCalendar.formatDate(calEvent.start, "dd-MM-yyyy")+ '\nfine '+jQuery.fullCalendar.formatDate(calEvent.end, "dd-MM-yyyy"));
		}, // end eventClick

		eventRender: function(event, element) {
			var inizio = jQuery.fullCalendar(event.start).format("MM");
			var fine = jQuery.fullCalendar(event.end).format("MM");
			var currentView = jQuery('#calendar').fullCalendar('getDate').stripTime().format();
			var currentViewMonth = jQuery.fullCalendar(currentView).format("MM");

			if( (inizio != currentViewMonth) && (fine != currentViewMonth) && (!event.id.match(/@google.com/g)) ){
          			element.addClass('oldEvent');
			}
		} // end eventRender
		});
} // FINE createCalendarShiftView();





/************************************************************************************************/
/****************** CREAZIONE CALENDARIO TURNI MODALITA' ADMIN **************************/
/************************************************************************************************/
function createCalendarShiftAdmin(allowed, shiftType, shiftCalObj, shiftGrpObj) {


	// read the list of the shift associated to the activity
	tipoTurni = shiftGrpObj.getShiftFromType(shiftType);

	// defines date variables
	var today = new Date();
	var dToday = today.getDate();
	var mToday = today.getMonth();
	var yToday = today.getFullYear();

  	// Build relative path of proxy
    var uriProxy = shiftCalObj.uriProxy;
    // used to be appended to the reperibility  events
	var indexRep = 0;

	// decodifica id -> nome reperibile
	// decodifica id -> colore reperibile
	var idToName  = new Array();
	var shiftToColor = new Array();
	var idToMobile = new Array();
    var idToEmail = new Array();
	var idToShift = new Array();

	// contiene le mail dei turnisti
	var personEmails = new Array()

	// array containing jolly persons with their shift
	var jollyPersons = new Array();

	var tipi = new Array();
    var turni = new Array();

	// tipologia di turni da leggere dal DB
	var tipoTurni = shiftGrpObj.getShiftFromType(shiftType);

    var loadedPeriod = new Array();
	var period = '';

	// for each shift read the persons involved from the DB
	// and adds them to the interface to be drugged

	// div contenente le persone in turno
	var divContainer = '#external-events';

	// Ora usata per il caricamento dei turni annullati
	var cancelledHourS = '07:00:00.000';
	var cancelledHourE = '12:00:00.000';

	j = 0;
	while (j < tipoTurni.length) {
    	var tipoTurno = tipoTurni[j];
    	//console.log("tipoTurno:"+tipoTurno);
    	var color = shiftColor.shift();
    	var div = '#' + tipoTurno;

    	$('<span>', { class: "titolo-external", text: 'Turno ' + tipoTurno }).appendTo(divContainer);
    	$('<div>', { id: tipoTurno, }).appendTo(divContainer);
    	//console.log("chiamata rest a: "+shiftCalendar.category);
    	// uri REST per leggere le persone in turno
    	uriGetShiftPersons = shiftCalendar.getUriRestToGetPersons(tipoTurno);
    	//console.log("uriGetShiftPersons="+uriGetShiftPersons);

    	var data = new Array();
    	//data.push('GET');
    	data.push(uriGetShiftPersons);

    	var dataJson = JSON.stringify(uriGetShiftPersons);

    	// exec the URI call
    	var shiftPerson = _RestJsonCall (uriGetShiftPersons, 'GET', false, {});

    	jQuery.each(shiftPerson, function (i, event) {
    		var name = event.name + " " + event.surname;
    		if (!event.jolly) {
    			shiftCalendar.createShiftElement(event.id, name, tipoTurno, color).appendTo(div);
    			idToShift[event.id] = tipoTurno;
    			shiftToColor[tipoTurno] = color;

              //console.log("creo elemento id: " + event.id+ "name="+event.name+ " tipoTurno=="+tipoTurno+ " color="+color);
    		} else {
    			if (jollyPersons.hasOwnProperty(event.id)) {
    				var turni = jollyPersons[event.id];
    				turni.push(tipoTurno);
    				jollyPersons[event.id] = turni;
    			} else {
    				jollyPersons[event.id] = new Array(tipoTurno);
    			}
    			idToShift[event.id] = "J";
    			shiftToColor["J"] = jollyColor;
    		}

    		// get the email and the mobile phone
    		idToEmail[event.id] = event.email;
    		idToMobile[event.id] = event.mobile;
    		idToName[event.id] = name;
    		var m = event.email;
    		var temp = {email: event.email};
    		personEmails.push(temp);
        });

    	j++;
    	tipoTurno = '';
	}


	// Read the shift time tables
	// and print them on on the left side of the calendar
	// N.B. we assume that each shift has the same time table
	//---------------------------------------------

	// uri REST per leggere la time table di un turno
    uriGetShiftTimeTable = shiftCalendar.getUriRestToGetShiftTimeTable(tipoTurni[0]);
    //console.log("uriGetShiftTimeTable="+uriGetShiftTimeTable);

	var data = new Array();
	data.push('GET');
	data.push(uriGetShiftTimeTable);

	var dataJson = JSON.stringify(data);

	var shiftTimeTable = _RestJsonCall (uriGetShiftTimeTable, 'GET', false, {});

	// fieldset che contiene l'orario
    $('<fieldset>', { id: 'orario' }).appendTo(divContainer);
    $('<legend>', { text: 'Orario' }).appendTo('#orario');
	$('<input>', { type: 'radio', id: 'mattina', name: 'hour', value: shiftTimeTable[0].startMorning + '-' + shiftTimeTable[0].endMorning, checked: 'checked' }).appendTo('#orario');
    $('<span>', { text: ' ' }).appendTo(divContainer);
	$('<label>', { for: 'shift', text: ' Mattina' }).appendTo('#orario');
   	$('<br />').appendTo('#orario');
	$('<input>', { type: 'radio', id: 'pomeriggio', name: 'hour', value: shiftTimeTable[0].startAfternoon + '-' + shiftTimeTable[0].endAfternoon }).appendTo('#orario');
	$('<span>', { text: ' ' }).appendTo(divContainer);
	$('<label>', { for: 'shift', text: ' Pomeriggio' }).appendTo('#orario');
	$('<br />').appendTo(divContainer);
	$('<hr />').appendTo(divContainer);
	$('<span>', { class: 'titolo-external', text: 'Personale sostituto' }).appendTo(divContainer);

	// adds the jolly persons
	//ATTENZIONE!! si da comunque per scontato che il reperibile sia 1 solo!!!! POI CAMBIARE!!!!!
	// non torna id='J'
	for (var id in jollyPersons) {
    	var turniJ = jollyPersons[id];

    	$('<span>', { text: 'Turno: ' }).appendTo(divContainer);

    	for (var i = 0; i < turniJ.length; i++) {
        		$('<label>', { for: 'shift', text: turniJ[i] }).appendTo(divContainer);
        		$('<span>', { text: ' ' }).appendTo(divContainer);
        		$('<input>', { type: 'radio', id: 'shift', name: 'shift', value: turniJ[i], checked: 'checked' }).appendTo(divContainer);
        		$('<span>', { text: ' ' }).appendTo(divContainer);
    	}

    	$('<div>', { id: 'J' }).appendTo(divContainer);
    	//console.log( "id=" + id+ "turni="+jollyPersons[ id ] );
    	//console.log("creo elemento id: " + id+ "name="+idToName[id]+ " tipoTurno=J  color="+jollyColor);
    	shiftCalendar.createShiftElement(id, idToName[id], "J", jollyColor).appendTo("#J");
	}

	$('<span>', { class: 'titolo-external', text: 'Annullato' }).appendTo(divContainer);
	$('<div>', { id: 'X' }).appendTo(divContainer);
	shiftCalendar.createShiftElement('', 'Turno ANNULLATO', "X", "CORNFLOWERBLUE").appendTo("#X");

	/* initialize the external events
	-----------------------------------------------------------------*/
	jQuery(' div.external-event').each(function() {
		// create an Event Object (http://arshaw.com/fullcalendar/docs/event_data/Event_Object/)
		// it doesn't need to have a start or end
		var type = idToShift[jQuery(this).attr("id")];
		var eventObject = {
			id: 'turno-' + jQuery(this).attr("id"),
			title: jQuery.trim(jQuery(this).text()),  // use the element's text as the event title
			personId: jQuery(this).attr("id"),
			shiftType: type,
			eMail: idToEmail[jQuery(this).attr("id")],
			mobile: idToMobile[jQuery(this).attr("id")],
			color: shiftToColor[type],
		};

		// store the Event Object in the DOM element so we can get to it later
		jQuery(this).data('eventObject', eventObject);

		// make the event draggable using jQuery UI
	  	jQuery(this).draggable({
			zIndex: 999,
			revert: true,      // will cause the event to go back to its
			revertDuration: 0  //  original position after the drag
		});

	});


	/* initialize the calendar
	-----------------------------------------------------------------*/
	jQuery('#calendar').fullCalendar({
		// renames months in italian
		monthNames: shiftCalendar.monthNames,
		monthNamesShort: shiftCalendar.monthNamesShort,

		//renames days in italian
		dayNames: shiftCalendar.dayNames,
		dayNamesShort: shiftCalendar.dayNamesShort,

		// defines the header
		header: shiftCalendar.header,

		// define the text of the buttton
		buttonText: shiftCalendar.buttonText,

		// properties
		selectable: shiftCalendar.selectable,	// highlight multiple days
		editable: 	shiftCalendar.editable,
		droppable: 	shiftCalendar.droppable, // this allows things to be dropped onto the calendar !!!
		firstDay: 	shiftCalendar.firstDay,	// start from monday
		weekMode: 	shiftCalendar.weekMode,
		timeFormat: shiftCalendar.timeFormat,
		displayEventEnd: shiftCalendar.displayEventEnd,

		// define events to be included
 		eventSources: [
        {
      		events: function (start, end, timezone, callback) {
      			//Eventi da visualizzare nel calendario
      			var events = [];
      			
      			var currDate = $('#calendar').fullCalendar('getDate').stripTime().format();
            
      			//console.log("currDate: "+currDate);
      			var currentYear = currDate.substring(0,4);
      			//console.log("currentYear: "+currentYear);
	            //var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
	            //period = currentYear.concat(jQuery.fullCalendar.formatDate(currDate, "MM"));

      			// build the URI for the json file
      			var uriParam = currentYear.concat('/01/01/').concat(currentYear.concat('/12/31'));
      			//console.log("uriParam: "+uriParam);
        		
      			/* get the work shift
        		----------------------------------------------------------------- */
      			var datas = new Array();
    			if (loadedPeriod.indexOf(currentYear) <  0) {
    				loadedPeriod.push(currentYear);
    				j=0;

    				while (j < tipoTurni.length) {
    					var tipoTurno = tipoTurni[j];
    					
    					//console.log("uriParam: "+uriParam);
    					uriShiftToGet = shiftCalendar.getUriRestToGetEntity(tipoTurno, uriParam);
                    	//console.log("++ uriShiftToGet="+uriShiftToGet);

	    				// exec the URI call
	    				var shiftPeriods = _RestJsonCall (uriShiftToGet, 'GET', false, {});
	
	
	    				// TURNI
	    				var data2 = shiftPeriods;
	    				jQuery.each(data2, function(i, event) {
		    				var startEv = event.start;
		      				var endEv = event.end;
		      				var tempStartMonth = startEv.split("-");
		      				var tempEndMonth = endEv.split("-");
		
		      				var startMonth = tempStartMonth[1];
		      				var endMonth = tempEndMonth[1];
	   						if (event.cancelled == 'false') {
	   							if (startMonth != endMonth) {
	   								var lastDayPreMonth = new Date(currentYear, startMonth, 0).getDate();
	   								var event1 = {
	   									'color': shiftToColor[tipoTurno],
	   									'personId': event.id,
	   									'title': ' -- ' + idToName[event.id],
	    								'shiftType': tipoTurno,
	    								'shiftHour': event.ttStart,
	    								'shiftSlot': event.shiftSlot,
	    								'cancelled': false,
	    								'allDay': false,
	    								'eMail': idToEmail[event.id],
	    								'mobile': idToMobile[event.id],
	    								'id': 'turno-'+event.id+'-'+indexRep+"-1",
	    								'start': jQuery.fullCalendar.moment(event.start + " " + event.ttStart),
	    								'end': jQuery.fullCalendar.moment(currentYear+"-"+startMonth+"-"+lastDayPreMonth + "T" +event.ttEnd + "Z"),
	   								}
	   								indexRep++;
	
	   								var event2 = {
	   									'color': shiftToColor[tipoTurno],
	    						    	'personId': event.id,
	       						   		'title': ' -- ' + idToName[event.id],
	       						   		'shiftType': tipoTurno,
	    								'shiftHour': event.ttStart,
	    								'shiftSlot': event.shiftSlot,
	    								'cancelled': false,
	    								'allDay': false,
	    								'eMail': idToEmail[event.id],
	    								'mobile': idToMobile[event.id],
	    								'id': 'turno-'+event.id+'-'+indexRep+"-2",
	    								'start': jQuery.fullCalendar.moment(currentYear+"-"+endMonth+"-01T" + event.ttStart + "Z"),
	    								'end': jQuery.fullCalendar.moment(event.end + " " + event.ttEnd),
	   								}
	           					indexRep++;
	   						    events.push(event1);
	   						    events.push(event2);
	   							jQuery('#calendar').fullCalendar('renderEvent', event1, true);
	           					jQuery('#calendar').fullCalendar('renderEvent', event2, true);
	   							} else {
		          					event['color'] = shiftToColor[tipoTurno];
		          					event['personId'] = event.id;
		          					event['title'] = ' -- ' + idToName[event.id];
		    						event['shiftType'] = tipoTurno;
		          					event['shiftHour'] = event.ttStart;
		          					event['cancelled'] = false;
		          					event['allDay'] = false;
		          					event['eMail'] = idToEmail[event.id];
		          					event['mobile'] = idToMobile[event.id];
		          					event['id'] = 'turno-'+event.id+'-'+indexRep;
									event['start'] = event.start + " " + event.ttStart;
									event['end'] = event.end + " " + event.ttEnd;
	
									jQuery('#calendar').fullCalendar('renderEvent', event, true);
									indexRep++;

									// set data.end = data.start for check problem
									if (event.end == null) {event.end = event.start;}
	   							}	
	   						} else {
								event['color'] = shiftToColor[tipoTurno];
								event['shiftType'] = tipoTurno;
								event['eMail'] = idToEmail[event.id];
								event['mobile'] = idToMobile[event.id];
								event['title'] = 'Turno '+tipoTurno+' ANNULLATO';
		          				event['start'] = jQuery.fullCalendar.moment(event.start+" "+cancelledHourS);
		          				event['end'] = jQuery.fullCalendar.moment(event.end+" "+cancelledHourE);
		          				event['allDay'] = true;
		         	 			event['className'] = "del-event";
		          				event['cancelled'] = true;
		    					event['editable'] = 'false';
		          				event['id'] = 'turno-annullato-'+indexRep;
		          				
		          				events.push(event);
		          				jQuery('#calendar').fullCalendar('renderEvent', event, true);
		          				indexRep++;
		
		          				// set data.end = data.start for check problem
		          				if (event.end == null) {event.end = event.start;}
    						}
	    				});

    					j++;
    					tipoTurno = '';
    				}

    				/* get the absenses
            		---------------------------------------------------------------------*/
    				uriFerieToGet = shiftCalendar.getUriRestToGetEntity('absence', uriParam);

    				// costruisce il json delle email dei turnisti per
	      			// prenderne le assenze
	      			var absParameter = {emails : personEmails};
	      			var jsonAbsParameter = JSON.stringify(absParameter);

		            var data = new Array();
		            //data.push('POST');
		            //data.push(uriFerieToGet);
		            jsonAbsParameter = noDuplicate(jsonAbsParameter);
		            data.push(jsonAbsParameter);
		            //console.log("jsonAbsParameter: "+jsonAbsParameter);

		            //var dataJson = JSON.stringify(data);
		            //console.log("uriFerieToGet"+uriFerieToGet);
		            //console.log('DATAJSON: '+dataJson);
		            // exec the URI call
		            var absentPerson = _RestJsonCall (uriFerieToGet, 'POST', false, jsonAbsParameter);
		
		            var indexVac = 0;
		            jQuery.each(absentPerson, function (i, event) {
			              event['color'] = 'LEMONCHIFFON';
			              event['textColor'] = 'red';
			              event['borderColor'] = 'red';
			              event['personId'] = event.personId;
			              event['title'] = 'Assenza ' + idToName[event.personId];
			    		//event['start'] = event.dateFrom;
    					//event['end'] = event.dateTo;
			              event['id'] = 'assenza-' + event.personId + '-' + indexVac;
			              //console.log("assenza di " + idToName[event.personId] + "event.personId="+event.personId);
			              
			             // event.color = 'yellow';
			              events.push(event);
			              jQuery('#calendar').fullCalendar('renderEvent', event, true);
			              indexVac++;

		            });
    			};
    			
    			callback(events);
      		}
    		
          },
          
          
	      /*{
	        url: 'https://www.google.com/calendar/feeds/it.italian%23holiday%40group.v.calendar.google.com/public/basic',
	        color: 'red',
	        textColor: 'white'
	      }*/

        ],

	// this function is called when something is dropped
	drop: function(date, allDay) {
		var date2 = jQuery.fullCalendar.formatDate(date, "YYYY-MM-DD");

		// retrieve the dropped element's stored Event Object
		var originalEventObject = jQuery(this).data('eventObject');

		// we need to copy it, so that multiple events don't have a reference to the same object
		var copiedEventObject = jQuery.extend({}, originalEventObject);

		// read the period of time set by the user
		var shiftHour = jQuery("input:radio[name=hour]:checked").val().split("-");
		var shiftSlot = jQuery("input:radio[name=hour]:checked").attr('id');

		var currentView = new Date();
		
		var currentMonth = currentView.getMonth() +1;
		//console.log("currentMonth: "+currentMonth);

		// assign it the date that was reported with the time period selected by the user
		// and adds a progressive number to the id in order to avoid repeated events
		copiedEventObject.allDay = false;
		copiedEventObject.start = jQuery.fullCalendar.moment(date2.concat(' ').concat(shiftHour[0]));
		copiedEventObject.end = jQuery.fullCalendar.moment(date2.concat(' ').concat(shiftHour[1]));
		copiedEventObject.shiftHour = shiftHour[0];
		copiedEventObject.title = ' -- '.concat(copiedEventObject.title);
		copiedEventObject.shiftSlot = shiftSlot;
		copiedEventObject.cancelled = false;

		copiedEventObject.id = copiedEventObject.id.concat('-').concat(indexRep);
		indexRep++;

		// change the color and the shift Type attribute for the sobstitute
		if (copiedEventObject.shiftType == 'J') {
			// read the assigned shif for the sobstitute
          		var shiftType = jQuery("input:radio[name=shift]:checked").val();
		  	copiedEventObject.shiftType = shiftType;
			copiedEventObject.color = shiftToColor[shiftType];
		}

		if(originalEventObject.title == 'Turno ANNULLATO'){
			var shiftType = jQuery("input:radio[name=shift]:checked").val();
			copiedEventObject.title = 'Turno '+ shiftType +' ANNULLATO';
			copiedEventObject.shiftType = shiftType;
			copiedEventObject.color = shiftToColor[shiftType];
			copiedEventObject.allDay = true;
			copiedEventObject.editable = false;
			copiedEventObject.className = 'del-event';
			copiedEventObject.cancelled = true;
			copiedEventObject.id = 'turno-annullato-'+indexRep;
			indexRep++;
	    }

	  	// render the event on the calendar
		// the last `true` argument determines if the event "sticks"
		jQuery('#calendar').fullCalendar('renderEvent', copiedEventObject, true);
		//console.log(copiedEventObject);
		// set data.end = data.start for check problem
		if (copiedEventObject.end == null) {copiedEventObject.end = copiedEventObject.start;}		
		var month = parseInt(copiedEventObject.start.stripTime().format().substring(5,7));
		//console.log("month: "+month);
		/* Qui va messo un controllo che prende da epas se per il mese in cui si vuole cambiare la configurazione dei turni
		 * sono già stati inviati gli attestati. La logica è che la modifica alla configurazione dei turni deve essere possibile
		 * solo se le info sui turni non sono già state inviate ad attestati.
		 */
		
		if (month  <  currentMonth) {
			// shift of the previous month cannot be modified from this screen
			alert ('ERRORE!\nNon e possibile modificare i turni del mese precedente a quello visualizzato.');
		  	jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
		} else {
			  // check if the shift overlap a ferie o others work shift
			  var obj = jQuery('#calendar').fullCalendar('clientEvents');
			  var cEOStart =  jQuery.fullCalendar.moment(copiedEventObject.start).format("YYYY-MM-DD");
			  var cEOEnd = jQuery.fullCalendar.moment(copiedEventObject.end).format("YYYY-MM-DD");
			  
			  jQuery.each(obj, function(i, val) {
				  var valStart = jQuery.fullCalendar.moment(val.start).format("YYYY-MM-DD");
				  var valEnd = jQuery.fullCalendar.moment(val.end).format("YYYY-MM-DD");
	
	
				  // check for the cancelled shift overlap
				  if (val.title.toString().match(/ANNULLATO/g) && (val.shiftType == copiedEventObject.shiftType) && (cEOStart >= valStart) && (cEOStart<=valEnd)&&(val.id.toString() != copiedEventObject.id.toString())){
	       				alert("Impossibile aggiungere persone per questo turno in questo giorno.\nTurno Annullato\n");
	       				jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
	       				return false;
				  }
	
					  // check for absences
				  if(val.id.toString().match(/assenza/g) && val.personId.toString() == copiedEventObject.personId.toString()) {
					  if ((valStart == cEOStart) || (cEOStart >= valStart && cEOStart <= valEnd)) {
						  alert("ERRORE! \nIl turnista e' in ferie!!!");
						  jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
					  }
					// check for shift
				  } else if(val.id.toString().match(/turno/g)  && (val.shiftType.toString() == copiedEventObject.shiftType.toString()) && (val.id.toString() != copiedEventObject.id.toString())) {
					  	// interval overlap
					  	if ( cEOStart >= valStart && cEOEnd <= valEnd) {
					  		if (val.shiftHour.toString() == copiedEventObject.shiftHour.toString()+":00.000") {
					  			alert("Il turno delle "+copiedEventObject.shiftHour.toString()+" e gia coperto");
					  			jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
					  		} else if (val.personId.toString() == copiedEventObject.personId.toString()) {
					  			alert(copiedEventObject.title+" e' gia' in turno");
					  			jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
							}
						}
					}
	
				  // Check that the jolly person doesn't cover two or more shifts
				  if ( (valStart <= cEOStart) && (cEOStart <= valEnd) && (cEOEnd <= valEnd) && (cEOEnd >= valStart) && (val.personId == copiedEventObject.personId) && (val.shiftType != copiedEventObject.shiftType)  && (!copiedEventObject.id.toString().match(/annullato/g))) {
					  alert(copiedEventObject.title+" e' gia' in turno ");
					  jQuery('#calendar').fullCalendar('removeEvents', copiedEventObject.id);
				  }
			});
		}
	},

	eventClick: function(calEvent, jsEvent, view) {
		if (calEvent.id.toString().match(/rep/g)) {
			alert(' \ninizio ' +jQuery.fullCalendar.moment(calEvent.start).format("DD-MM-YYYY")+ '\nfine '+jQuery.fullCalendar.moment(calEvent.end).format("DD-MM-YYYY")+ '\nid: '+calEvent.id+'\npersonId: '+calEvent.personId+'\ntipo turno: '+calEvent.shiftType+'\norario: '+calEvent.shiftHour);
		} else {
		    alert(calEvent.title +'\ncolor: '+calEvent.color+' \ntipo turno: '+calEvent.shiftType+'\nemail ' +calEvent.eMail +'\norario: '+calEvent.shiftHour+ '\ninizio ' +jQuery.fullCalendar.moment(calEvent.start).format("DD-MM-YYYY")+ '\nfine '+jQuery.fullCalendar.moment(calEvent.end).format("DD-MM-YYYY"));
		}
	}, // end eventClick


	eventMouseover: function(event, jsEvent, view){
			  // Add remove-event button
        $(this).find(".fc-event-time").append($("<a>", {
        	'href': '#',
        	'class': 'remove-event',
        	'title': 'Cancella questo evento',
        	'alt': 'Remove event',
        	'html': '<img src=\"' + '/public/images/remove-button-cross.png\"></img>',
        }));

        // Add remove-shift button
		if(event.cancelled != true){
        	$(this).find(".fc-event-time").append($("<a>", {
	          	'href': '#',
	          	'class': 'remove-shift',
	          	'title': 'Annulla l\'intero turno',
	          	'alt': 'Remove shift',
	          	'html': '<img src=\"'+ '/public/images/remove-button-sosta.png\"></img>',
        	}));
		}

        // Add remove-event button for deleted shift
        $(this).filter(".del-event").find(".fc-event-title").append($("<a>", {
        	'href': '#',
        	'class': 'remove-event',
        	'title': 'Cancella questo evento',
        	'alt': 'Remove event',
        	'html': '<img src=\"' + '/public/images/remove-button-cross.png\"></img>',
        }));

        // On click remove-event
        $(".remove-event").click(function(e) {
        	e.stopPropagation();
        	var conf = confirm("Sei sicuro di voler cancellare il turno?");
        	if(conf){
        		$('#calendar').fullCalendar('removeEvents', event.id);
        	}
        });

			  // On click remove-shift
        $(".remove-shift").click(function(e) {
        	startEvent = $.fullCalendar.moment(event.start).format("YYYY-MM-DD");
        	endEvent = $.fullCalendar.moment(event.end).format("YYYY-MM-DD");
        	//startEvent = $.fullCalendar.formatDate(event.start, "yyyy-MM-dd");
        	//endEvent = $.fullCalendar.formatDate(event.end, "YYYY-MM-dd");
        	e.stopPropagation();
        	var conf = confirm("In questo modo annullerai l'intero turno. Vuoi continuare?");
        	if(conf){
        		var events = $('#calendar').fullCalendar('clientEvents');
        		$.each(events, function(i,v){
        			if((event.id != v.id)&&(event.shiftType == v.shiftType) && (event.title != v.title) && (event.shiftHour != v.shiftHour)&&(!v.id.match(/@google.com/g))){
        				//start = $.fullCalendar.formatDate(v.start, "yyyy-MM-dd");
        				//end = $.fullCalendar.formatDate(v.end, "yyyy-MM-dd");
        				start = $.fullCalendar.moment(v.start).format("YYYY-MM-DD");
        				end = $.fullCalendar.moment(v.end).format("YYYY-MM-DD");

        				if(v.shiftHour == "07:00"){
        					startTime = "07:00:00";
        					endTime = "13:30:00";
        				} else {
        					startTime = "13:30:00";
        					endTime = "19:00:00";
        				}
        				if( startEvent <= start && start <= endEvent ){
        					if(end > endEvent){
        						var v1 = {
        							'title': "Turno " + event.shiftType + "\n\rANNULLATO",
        							'shiftType': event.shiftType,
        							'cancelled': true,
        							'id': "turno-X"+"-"+v.id,
        							'className': 'del-event',
        							'start': startEvent,
        							'end': endEvent,
        							'color': shiftToColor[v.shiftType],
        						};
        						$("#calendar").fullCalendar( 'renderEvent', v1, true);
        						// set data.end = data.start for check problem
        						if (v1.end == null) {v1.end = v1.start;}
        						var temp = event.end;
        						temp.setDate(temp.getDate()+1);
        						//temp = $.fullCalendar.formatDate(temp, "yyyy-MM-dd");
        						temp = $.fullCalendar.moment(temp).format("YYYY-MM-DD");
        						var v2 = {
        							'title': v.title,
        							'allDay': false,
        							'id': v.id + "-1",
        							'calcelled': false,
        							'shiftHour': v.shiftHour,
        							'shiftType': v.shiftType,
        							'color': shiftToColor[v.shiftType],
        							'personId': v.personId,
			                      	'start': temp + " " + startTime,
			                      	'end': end + " " + endTime,
			                    };
        						$("#calendar").fullCalendar( 'renderEvent', v2, true);
        						// set data.end = data.start for check problem
        						if (v2.end == null) {v2.end = v2.start;}
        						$('#calendar').fullCalendar('removeEvents', v.id);
        					} else {
        						var v1 = {
        							'title': "Turno " + event.shiftType + "\n\rANNULLATO",
        							'shiftType': event.shiftType,
        							'cancelled': true,
        							'id': "turno-X"+"-"+v.id,
        							'className': 'del-event',
        							'start': startEvent,
        							'end': endEvent,
        							'color': shiftToColor[v.shiftType],
        						};
        						$("#calendar").fullCalendar( 'renderEvent', v1, true);
        						// set data.end = data.start for check problem
        						if (v1.end == null) {v1.end = v1.start;}
        						$('#calendar').fullCalendar('removeEvents', v.id);
        					}

        				} else if( startEvent >= start && startEvent <= end){
        					var temp3 = event.start;
        					temp3.setDate(temp3.getDate() - 1);
        					temp3 = $.fullCalendar.moment(temp3).format("YYYY-MM-DD");
        					var v1 = {
        						'title': v.title,
        						'allDay': false,
        						'id': v.id + "-1",
        						'personId': v.personId,
        						'shiftHour': v.shiftHour,
        						'shiftType': v.shiftType,
        						'cancelled': false,
        						'color': shiftToColor[v.shiftType],
        						'personId': v.personId,
        						'start': start + " " + startTime,
        						'end': temp3 + " " + endTime,
        					};
        					$("#calendar").fullCalendar( 'renderEvent', v1, true);

        					// set data.end = data.start for check problem
        					if (v1.end == null) {v1.end = v1.start;}

        					if(endEvent < end){
        						var temp2 = event.end;
        						temp2.setDate(temp2.getDate() + 1);
        						temp2 = $.fullCalendar.moment(temp2).format("YYYY-MM-DD");
        						var v2 = {
        							'title': "Turno " + event.shiftType + "\n\rANNULLATO",
        							'id': "turno-X"+"-"+v.id,
        							'shiftType': v.shiftType,
        							'cancelled': true,
        							'className': 'del-event',
        							'color': shiftToColor[v.shiftType],
        							'start': startEvent,
        							'end': endEvent,
        						};
        						$("#calendar").fullCalendar( 'renderEvent', v2, true);
        						//	set data.end = data.start for check problem
        						if (v2.end == null) {v2.end = v2.start;}
        						var v3 = {
        							'title': v.title,
        							'allDay': false,
        							'id': v.id + "-2",
        							'shiftHour': v.shiftHour,
        							'shiftType': v.shiftType,
        							'cancelled': false,
        							'color': shiftToColor[v.shiftType],
        							'personId': v.personId,
        							'start': temp2 + " " + startTime,
        							'end': end + " " + endTime,
        						};
        						$("#calendar").fullCalendar( 'renderEvent', v3, true);
        						// set data.end = data.start for check problem
        						if (v3.end == null) {v3.end = v3.start;}
        						$('#calendar').fullCalendar('removeEvents', v.id);
        					} else if(endEvent >= end){
        						var v2 = {
        							'title': "Turno " + event.shiftType + "\n\rANNULLATO",
        							'id': "turno-X"+"-"+v.id,
        							'shiftType': v.shiftType,
        							'cancelled': true,
        							'className': 'del-event',
        							'start': startEvent,
        							'color': shiftToColor[v.shiftType],
        							'end': endEvent,
        						};

        						$("#calendar").fullCalendar( 'renderEvent', v2, true);
        						// set data.end = data.start for check problem
        						if (v2.end == null) {v2.end = v2.start;}
        						$('#calendar').fullCalendar('removeEvents', v.id);
        					}
        				}
        				$('#calendar').fullCalendar('removeEvents', event.id);
        			}
        		});
        	}
        });
	}, // end eventMouseover

	eventMouseout: function(event, jsEvent, view) {
		$(this).find(".remove-event").remove();
		$(this).find(".remove-shift").remove();
		$(".del-event").find(".remove-event").remove();
	}, // end eventMouseout

	eventDrop: function(event, dayDelta, minuteDelta, allDay, revertFunc) {
		var currentView = new Date();
		var currentMonth = currentView.getMonth()+1;

	  // set data.end = data.start for check problem
		if (event.end == null) {event.end = event.start;}
		var month = parseInt(event.start.stripTime().format().substring(5,7));
	  // holiday events cannot be modified
		if (event.id.toString().match(/ferie/g)) {
            alert ('Da questa interfaccia non e possibile modificare le ferie!');
            revertFunc();
		} else if ((parseInt(event.start.stripTime().format().substring(5,7)) < currentMonth) || (parseInt(event.end.stripTime().format().substring(5,7)) > currentMonth)) {
            // reperibility of the previous month cannot be modified from this screen
            alert ('ERRORE!\nNon e possibile modificare i turni del mese precedente o successivo a quello visualizzato.');
            revertFunc();
		} else {
				    // check if absences and shift of a certain person overlap
            var obj = $('#calendar').fullCalendar('clientEvents');
					  //var eventStart = $.fullCalendar.formatDate(event.start, "yyyy-MM-dd");
            var eventStart = $.fullCalendar.moment(event.start).format("YYYY-MM-DD");
            //var eventEnd = $.fullCalendar.formatDate(event.end, "yyyy-MM-dd");
            var eventEnd = $.fullCalendar.moment(event.end).format("YYYY-MM-DD");
            jQuery.each(obj, function(i, val) {
            	//var valStart = $.fullCalendar.formatDate(val.start, "yyyy-MM-dd");
            	//var valEnd = $.fullCalendar.formatDate(val.end, "yyyy-MM-dd");
            	var valStart = $.fullCalendar.moment(val.start).format("YYYY-MM-DD");
            	var valEnd = $.fullCalendar.moment(val.end).format("YYYY-MM-DD");

            	if (((valStart == eventStart) || (eventStart >= valStart && eventStart <= valEnd)) && (val.id.toString() != event.id.toString())) {
            		// check for the ferie
            		if (val.id.toString().match(/assenza/g) && val.personId.toString() == event.personId.toString()) {
          				alert("Impossibile spostare il turno.\n La persona e in ferie!");
          				revertFunc();
            		}
            		// check for the shift
            		else if (val.id.toString().match(/turno/g)  &&  (val.shiftType.toString() == event.shiftType.toString())) {
            			if (val.shiftHour.toString() === event.shiftHour.toString()+":00.000") {
            				alert("Il turno delle "+event.shiftHour.toString()+" e gia coperto");
            				revertFunc();
            			} else if (val.personId.toString() === event.personId.toString()) {
            				alert(event.title+" e' gia' in turno ");
            				revertFunc();
            			}
            		}
            	}
            	// Check that the jolly person doesn't cover two or more shifts
            	if ( (valStart >= eventStart) && (eventStart <= valEnd) && (eventEnd <= valEnd) && (eventEnd >= eventStart) && (val.personId.toString() == event.personId.toString()) && (val.shiftType != event.shiftType) && (!event.id.toString().match(/annullato/g)) ) {
            		alert(event.title+" e' gia' in turno ");
            		jQuery('#calendar').fullCalendar('removeEvents', event.id);
            	}
      		});
		}
	}, // end eventdrop

	eventResize: function(event, dayDelta, minuteDelta, revertFunc) {

		var currentView = new Date();
		var currentMonth = currentView.getMonth() +1;

		// set data.end = data.start for check problem
		if (event.end == null) {event.end = event.start;}

		// holidays events cannot be modified
		if (event.id.toString().match(/ferie/g)) {
			alert ('da questa interfaccia non e possibile modificare le ferie!');
			revertFunc();
		} else if (parseInt(event.end.stripTime().format().substring(5,7)) <  currentMonth) {
			// reperibility of the previous month cannot be modified from this screen
			alert ('ERRORE!\nNon e possibile modificare i turni del mese precedente a quello visualizzato.');
			revertFunc();
		} else {
			// check if absences and shift  of a certain person overlap
            var obj = $('#calendar').fullCalendar('clientEvents');
            //ar eventStart = $.fullCalendar.formatDate(event.start, "yyyy-MM-dd");
            //var eventEnd = $.fullCalendar.formatDate(event.end, "yyyy-MM-dd");
            var eventStart = $.fullCalendar.moment(event.start).format("YYYY-MM-DD");
			var eventEnd = $.fullCalendar.moment(event.end).format("YYYY-MM-DD");
            jQuery.each(obj, function(i, val) {
            	//var valStart = $.fullCalendar.formatDate(val.start, "yyyy-MM-dd");
            	//var valEnd = $.fullCalendar.formatDate(val.end, "yyyy-MM-dd");
            	var valStart = $.fullCalendar.moment(val.start).format("YYYY-MM-DD");
            	var valEnd = $.fullCalendar.moment(val.end).format("YYYY-MM-DD");
				if ((eventEnd >= valStart) && (eventStart < valStart) && (val.id.toString() != event.id.toString())) {
				    if (val.id.toString().match(/turno/g) && (val.shiftType.toString() == event.shiftType.toString())) {
				    	if (val.shiftHour.toString() === event.shiftHour.toString()) {
				    		alert("ATTENZIONE!\nIl turno delle "+event.shiftHour.toString()+" e gia coperto");
				    		revertFunc();
				    	} else if (val.personId.toString() === event.personId.toString()) {
				    		alert(event.title+" e' gia' in turno ")
				    		revertFunc();
				    	}
				    }
					if (val.id.toString().match(/assenza/g) && val.personId.toString() == event.personId.toString()) {
					    alert("Il turno di "+event.title+" si sovrappone con le sue ferie");
			        	revertFunc();
					}
				}
            });
			    }
      	}, // end eventResize

	eventRender: function(event, element) {

		//TODO: da riscrivere!
		/*var inizio = $.fullCalendar.formatDate(event.start, "MM");
		var fine = $.fullCalendar.formatDate(event.end, "MM");
		var currentView = $('#calendar').fullCalendar('getDate').stripTime().format();
		var currentViewMonth = $.fullCalendar.formatDate(currentView, "MM");

		if ( (inizio != currentViewMonth) && (fine != currentViewMonth) && (!event.id.match(/@google.com/g)) ) {
        		element.addClass('oldEvent');
   		}*/
	} // end eventRender
      	
	});
}// FINE createCalendarShiftAdmin();




/************************************************************************************************/
/****************** CREAZIONE CALENDARIO ASSENZE  MODALITA' VIEW **************************/
/*
/* allowed: se l'utente ha il permesso di visualizzare il calendario
/************************************************************************************************/
function createCalendarAbsenceView(allowed, persons, absenceCalObj, absenceGrpObj) {


	// defines date variables
	var today = new Date();
	var dToday = today.getDate();
	var mToday = today.getMonth();
	var yToday = today.getFullYear();

	// used to be appended to the reperibility  events
	var indexAbsence = 0;

	// decodifica id -> nome reperibile
	// decodifica id -> colore reperibile
	var idToDescription  = new Array();
	var absenceToColor = new Array();
	var idToMobile = new Array();
	var idToEmail = new Array();

	var firstYear = 0;
	var loadedYears = new Array();
	
	// div contenente le persone in turno
	var divContainer = '#external-events';

	// inserisce la lista delle tipologie di assenza
       	$('<span>', { class: "titolo-external", text: 'Tipologie di assenze' }).appendTo(divContainer);

	// get the current year
	var today = new Date();
	var yyyy = today.getFullYear();
	var param = yyyy + '/01/01/' + yyyy + '/12/31';

	// uri REST per leggere le persone del turno 'tipoTurno'
	uriGetAbsenceTypes = absenceCalObj.getUriRestToGetAbsenceTypes(param);
	//console.log('uriGetAbsenceTypes='+uriGetAbsenceTypes);

	var data = new Array();
	data.push('GET');
	data.push(uriGetAbsenceTypes);
	
	var dataJson = JSON.stringify(data);
	
	console.log(dataJson);

	// exec the URI call
	var absenceTypes = _RestJsonCall (absenceCalObj.uriProxy, 'POST', false, dataJson);

	//console.log(absenceTypes);

	jQuery.each(absenceTypes, function (i, event) {
		var color = absenceColor.shift();

		var name = event.description;
		absenceCalObj.createElement(event.code, name, color).appendTo(divContainer);
		
		var absCodes = event.code.split('-');
		jQuery.each(absCodes, function (i, code) {
			//console.log('code ' + code);
			absenceToColor[code] = color;
			idToDescription[code] = event.description;
		});
	});


	// initialize the external events
	//-----------------------------------------------------------------
	jQuery(' div.external-event').each(function() {

		var classAttr = new Array();
		classAttr = jQuery(this).attr("class").split(" ");

		var eventObject = {
			id: 'absence-' + jQuery(this).attr("id"),
			title: jQuery.trim(jQuery(this).text()),  // use the element's text as the event title
			absenceId: jQuery(this).attr("id"),
			absenceType: classAttr[2],
			color: classAttr[3],
		};

		// store the Event Object in the DOM element so we can get to it later
		jQuery(this).data('eventObject', eventObject);
	});

	// print the general tipe of absence
	var genericColor = absenceColor.shift();
	absenceCalObj.createElement('X', 'Assenza generica', genericColor).appendTo(divContainer);
	absenceToColor['X'] = genericColor;
	idToDescription['X'] = 'Assenza generica';

	// initialize the calendar
	//-----------------------------------------------------------------
	jQuery('#calendar').fullCalendar({
		loading: function(bool) {
			setTimeout(function() {
				if(bool) {
					jQuery('#loading').hide();
				}
        	}, 2000);
      	},

      	// renames months in italian
      	monthNames: absenceCalObj.monthNames,
      	monthNamesShort: absenceCalObj.monthNamesShort,

      	//renames days in italian
      	dayNames: absenceCalObj.dayNames,
      	dayNamesShort: absenceCalObj.dayNamesShort,

      	// defines the header
      	header: absenceCalObj.header,

      	// define the text of the buttton
      	buttonText: absenceCalObj.buttonText,

      	// properties
      	selectable:     absenceCalObj.selectable, // highlight multiple days
      	editable:       absenceCalObj.editable,
      	droppable:      absenceCalObj.droppable,  // this allows things to be dropped onto the calendar !!!
      	firstDay:       absenceCalObj.firstDay,           // start from monday
      	weekMode:       absenceCalObj.weekMode,
      	timeFormat:     absenceCalObj.timeFormat,

      	// define events to be included
 		eventSources: [
 		/*{
                url: 'https://www.google.com/calendar/feeds/it.italian%23holiday%40group.v.calendar.google.com/public/basic',
                color: 'red',
                textColor: 'white'
          },*/

 			{
          		events: function (start, end, callback) {
          			var currDate = $('#calendar').fullCalendar('getDate').stripTime().format();
          			var currentYear = jQuery.fullCalendar(currDate).format("YYYY");
          			firstYear = currentYear.concat(jQuery.fullCalendar(currDate).format("MM"));

          			// build the URI for the json file
          			var uriParam = currentYear.concat('/01/01/').concat(currentYear.concat('/12/31'));

          			// get the absences
          			//-----------------------------------------------------------------
          			var datas = new Array();
          			if(loadedYears.indexOf(currentYear) <  0) {
          				loadedYears.push(currentYear);

          				uriAbsenceToGet = absenceCalObj.getUriRestToGetEntity('absence', uriParam);
          				console.log('uriAbsenceToGet='+uriAbsenceToGet);

          				var data = new Array();
          				data.push('POST');
          				data.push(uriAbsenceToGet);
          				data.push(JSON.stringify(persons));

          				var dataJson = JSON.stringify(data);

          				// exec the URI call
          				var absencePeriod = _RestJsonCall (absenceCalObj.uriProxy, 'POST', false, dataJson);

          				jQuery.each(absencePeriod, function (i, event) {
          					if (absenceToColor[event.code]) {
          						event['color'] = absenceToColor[event.code];
          					} else {
          						event['color'] = absenceToColor['X'];
          					}

          					event['absenceType'] = event.code;
          					event['title'] = event.name + ' ' +event.surname;
          					//event['start'] = event.dateFrom;
          					//event['end'] = event.dateTo;
          					event['allDay'] = true;
          					event['id'] = 'absence' + indexAbsence;

          					indexAbsence++;
          					jQuery('#calendar').fullCalendar('renderEvent', event, true);

          					// set data.end = data.start for check problem
          					if (event.end == null) {
          						event.end = event.start;
          					}
          				});
	               	}
          		}
         	}
      		],

                /*eventClick: function(calEvent, jsEvent, view) {
                                if (calEvent.id.toString().match(/absence/g)) {
                     			alert(calEvent.title + ' ' + idToDescription[calEvent.absenceType] + ' ' + calEvent.absenceType + ' \ndal ' +jQuery.fullCalendar.formatDate(calEvent.start, "dd-MM-yyyy")+ '\nal '+jQuery.fullCalendar.formatDate(calEvent.end, "dd-MM-yyyy"));
                                } else {
                      			alert(calEvent.title +' \ninizio ' +jQuery.fullCalendar.formatDate(calEvent.start, "dd-MM-yyyy")+ '\nfine '+jQuery.fullCalendar.formatDate(calEvent.end, "dd-MM-yyyy"));
                                }

                },*/ // end eventClick

            eventRender: function(event, element) {
                      var inizio = jQuery.fullCalendar(event.start).format("MM");
                      var fine = jQuery.fullCalendar(event.end).format("MM");
                      var currentView = jQuery('#calendar').fullCalendar('getDate').stripTime().format();
                      var currentViewMonth = jQuery.fullCalendar(currentView).format("MM");

                      if( (inizio != currentViewMonth) && (fine != currentViewMonth) && (!event.id.match(/@google.com/g)) ){
                            element.addClass('oldEvent');
                      }
            } // end eventRender
      });
  } // FINE createCalendarAbsenceView();
