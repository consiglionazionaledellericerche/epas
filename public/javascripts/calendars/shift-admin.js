var Drupal = Drupal || { 'settings': {}, 'behaviors': {}, 'themes': {}, 'locale': {} };

// crea un calendario per i turni
var shiftCalendar = new sistCalendar("shift", "admin");
var groupConf = new sistCalendarGroup("shift", "admin");

// prende i colori per visualizzare le persone nel calendario
var shiftColor = shiftCalendar.getPersonColor(false);
var jollyColor = shiftCalendar.getPersonColor(true);

var uriShiftToPost = shiftCalendar.uriRepToPut;

// prende la uri REST per la scrittura dei turni sul DB
var uriProxy = shiftCalendar.uriProxy;

var userAllowed = false;

// tipo di attività dei turni
var calendarType = '';
var loadedPeriod = new Array();

jQuery(document).ready(function() {

    // defines date variables       
    var today = new Date();

    setTimeout(function(){
      jQuery('#loading').hide();
    }, 2000);
    
    
    // read the Group Ids of the user and all roles for the groups          
//    var gids = Drupal.settings.sistCalendar.gids['node'];
    
    // lista of calendar types that the group ca manage
    var types = new Array();
    
//    for (var gid in gids) {
//      groupTypes = groupConf.getTypes4Group(gid);
//      for(one in groupTypes){
//        if($.inArray(groupTypes[one], types) < 0){
//          types.push(groupTypes[one]);
//        }
//      }
//    }
    
	var json = $.getJSON('renderIds', function(data) {
		
		$.each(data, function(index, element) {
	        types.push(element);
	    });
		if (types.length > 1) {
			userAllowed = true;
			shiftCalendar.selectPopup(userAllowed, types, 'shiftAdmin', shiftCalendar, groupConf);
		} else {
			if (types.length == 1) {
				calendarType = types;
        groupConf.calendarChoise = calendarType;
        //console.log(calendarType);
				userAllowed = true;
				createCalendarShiftAdmin(userAllowed, calendarType, shiftCalendar, groupConf);
        jQuery('h1.title').append(" "+groupConf.getShiftDescription(types));
			} else {
				userAllowed = false;
				createCalendarShiftAdmin(userAllowed, calendarType, shiftCalendar, groupConf);			
			}	
		}
		
	});

    
		// Codice per bottone visuale successiva
		// ----------------------------------------
    jQuery('#next').click(function() {
     	jQuery('#calendar').fullCalendar('next');

      //view = $('#calendar').fullCalendar('getView');
      //alert("Primo giorno is " + view.visStart);

      var currDate = $('#calendar').fullCalendar('getDate');
      var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
      period = currentYear.concat(jQuery.fullCalendar.formatDate(currDate, "MM"));
      
      if(loadedPeriod.indexOf(currentYear) <  0) {
              loadedPeriod.push(currentYear);
      }
      jQuery('#calendar').fullCalendar('rerenderEvents');
    });

    // Codice per bottone visuale precedente
		// ----------------------------------------
    jQuery('#prev').click(function() {
     	jQuery('#calendar').fullCalendar('prev');

      var currDate = $('#calendar').fullCalendar('getDate');
      var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
      period = currentYear.concat(jQuery.fullCalendar.formatDate(currDate, "MM"));

      if(loadedPeriod.indexOf(currentYear) <  0) {
              loadedPeriod.push(currentYear);
      }
      jQuery('#calendar').fullCalendar('rerenderEvents');
    });

    // Codice per bottone visuale giorno odierno
		// ----------------------------------------
    jQuery('#oggi').click(function() {
      jQuery('#calendar').fullCalendar('today');
      jQuery('#calendar').fullCalendar('rerenderEvents');
    });

		// salva i turno del DB delle presenze
		// ----------------------------------------
		jQuery("#salva").click( function () {
			var obj = jQuery('#calendar').fullCalendar('clientEvents');
			
			var startDate = today;
      			var endDate = today;
			var calDate = $('#calendar').fullCalendar('getDate');

			// mese correntemente visualizzato (partendo da 0)
      			var mese = calDate.getMonth();
			var anno = calDate.getFullYear();

		
			turni = new Object();

			// tipologia di turni da leggere dal DB
                	var tipoTurni = groupConf.getShiftFromType(calendarType);
      			jQuery.each(tipoTurni, function (i, val) {
          			turni[val] = new Array();
      			});

      			jQuery.each(obj, function(i, val) {
				if ((val.id.toString().match('turno')) && (val.start.getMonth() == mese) && (val.start.getFullYear() == anno)) {
					if (val.cancelled.toString() == 'true') {
						var shiftPersona = {
							start: jQuery.fullCalendar.formatDate(val.start, "yyyy-MM-dd"),
	            					end: jQuery.fullCalendar.formatDate(val.end, "yyyy-MM-dd"),
							cancelled: true,
						}
					} else {
						var timeTableEnd = (val.shiftHour.toString().match('07:00')) ? "13:30" : "19:00"; 
						var shiftPersona = {
							id: val.personId,
							start: jQuery.fullCalendar.formatDate(val.start, "yyyy-MM-dd"),
							end: jQuery.fullCalendar.formatDate(val.end, "yyyy-MM-dd"),
							cancelled: false,
							shiftSlot: val.shiftSlot,
							//time_table_start: val.shiftHour,
							//time_table_end: timeTableEnd,
						};
					}
					
					// divide shifts for types
					turni[val.shiftType.toString()].push(shiftPersona);
          				
					if (val.start < startDate) {
              					startDate = val.start;
          				}
          				if (val.end > endDate) {
              					endDate = val.end;
          				}
				}
			});

			mese = mese + 1;

			jQuery.each(turni, function(i, val) {
            		// uri to put shifts
            		var uriPutRep = shiftCalendar.getUriRestToPutEntity(i, anno + '/' + mese);
        
            		var data = new Array();
            
            		var dataJson = JSON.stringify(val);
            
            		jQuery.ajax({
                		url: uriPutRep,
               	 		type: "PUT",
                		dataType: "json",
                		contentType: "application/json",
                		data: dataJson, 
                		success: function (responseData, textStatus, jqXHR) {
                    			alert("Le modifiche del turno " +i+ " sono state salvate con successo!!! :-)");
                		},
                		error: function (responseData, textStatus, errorThrown) {
					var msg = responseData.responseText;
					msg = msg.replace(/[[\]"]/g,'');
					//console.log(msg);
					alert('ERRORE durante il salvataggio del turno ' +i+'!\n' +msg);
                		}
            		});
        });
    });


    // button for the pdf document with the hourly monthly report 
    jQuery("#pdf-report-mese").click( function () {
	var currDate = $('#calendar').fullCalendar('getDate');
      	var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
      	var currentMonth = jQuery.fullCalendar.formatDate(currDate, "MM");

      	var conf = confirm('Sei sicuro di voler generare il report mensile del mese di '+currentMonth+'?');
      	if (conf) {
          	var uriParam = currentYear.concat('/').concat(currentMonth);
          	var uri = shiftCalendar.getUriRestToGetMontlyPDF(groupConf.calendarChoise, uriParam);

          	var req = new XMLHttpRequest();
            req.open("GET", uri, true);
            req.responseType = "blob";

            req.onload = function (event) {
              var blob = req.response;
              console.log(blob.size);
              var link=document.createElement('a');
              link.href=window.URL.createObjectURL(blob);
              link.download="Report"+"_Turni" + currentYear +"_"+currentMonth + ".pdf";
              link.click();
            };

            req.send();
      	}
     });

		// create the monthly calendar
    jQuery("#pdf-cal-mese").click(function () {
        var currDate = $('#calendar').fullCalendar('getDate');
        var currentYear = jQuery.fullCalendar.formatDate(currDate, "yyyy");
        var currentMonth = jQuery.fullCalendar.formatDate(currDate, "MM");

        var conf = confirm('Sei sicuro di voler generare il calendario PDF del mese di ' + currentMonth + '?');
        if (conf) {
            var uriParam = currentYear.concat('/').concat(currentMonth);
            var uri = shiftCalendar.getUriRestToGetMonthlyCalPDF(groupConf.calendarChoise , uriParam);

            var req = new XMLHttpRequest();
            req.open("GET", uri, true);
            req.responseType = "blob";

            req.onload = function (event) {
              var blob = req.response;
              console.log(blob.size);
              var link=document.createElement('a');
              link.href=window.URL.createObjectURL(blob);
              link.download="Calendario"+"_Turni" + currentYear +"_"+currentMonth + ".pdf";
              link.click();
            };

            req.send();
        }
    });
});
