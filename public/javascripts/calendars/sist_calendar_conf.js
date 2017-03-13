/***************************************
/* sistCalendar Class
/* Contiene le ini zializzazioni dei calendari contenuti nel portale SistORG
/*
/****************************************/


/***************************************
/* Inizializzazione
/****************************************/
function sistCalendar (category, mode)
{
	// calendar configuration di full calendar
	//------------------------------------
	this.defCategory = ['reperibility', 'shift', 'absence'];   // categorie previste
	this.category = category;		// categoria di calendario (reperibility, shift, absence)
	this.mode = mode;			// modalità del calendario (view, admin)

	// Sovrascrive i nomi di default in italiano
	this.monthNames      = ['Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno', 'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'];
	this.monthNamesShort = ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'];
	this.dayNames        = ['Domenica', 'Lunedi', 'Martedi', 'Mercoledi', 'Giovedi', 'Venerdi', 'Sabato'];
	this.dayNamesShort   = ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab'];

	// definisce il testo dei bottoni
	this.buttonText =  {
        day: 'giorno',
        week: 'settimana',
        month: 'mese',
        today: 'oggi'
   };

	// definisce l'header
	this.header = {
		left: '',
		center: 'title',
		right: ''
	};

	this.firstDay =       1;      // start from monday
    this.weekMode =       'variable';
    this.timeFormat =     'H:mm{ - H:mm}';
	this.selectable =     false;  // highlight multiple days

	// properties
	if (this.mode == "view") {
        this.editable =       false;
        this.droppable =      false; // this allows things to be dropped onto the calendar !!!
		// console.log("this.mode="+this.mode);

	} else {
		// console.log("this.mode="+this.mode);
        this.editable =       true;
        this.droppable =      true; // this allows things to be dropped onto the calendar !!!
	}


	// Uri al modulo Drupal che effettua le chiamate REST in scrittura verso EPAS
	//this.uriProxy	= 'https://sistorg.devel.iit.cnr.it/sistorg-dev4/rest/proxy/calendar';
	this.uriProxy	= 'https://sistorg.iit.cnr.it/rest/proxy/calendar';

	//this.epasServer = 'epas.devel.iit.cnr.it/';
	//this.epasServer = 'scorpio.nic.it:9001/';
	this.epasServer = 'epas.tools.iit.cnr.it/';

	// Route base per le chiamate REST	
	//this.basicRestRoute 	= 'epas.devel.iit.cnr.it/' + this.category;
	//this.basicRestRoute 	=  this.epasServer + this.category;
	this.basicRestRoute 	= 'epas.tools.iit.cnr.it/' + this.category;

}



/********************************************************
// Metodi
/********************************************************/
sistCalendar.prototype = {

	// Crea i div droppabili delle persone sul calendario
	createElement: function(id, label, color) {
    	return $('<div>', { id: id, class: "external-event " +color, text: label});
	},

    // Crea i div droppabili dei turnisti sul calendario
    createShiftElement: function(id, nome, turno, color) {
       return $('<div>', { id: id, class: "external-event " +turno+ " " +color, text: nome});
    },

	// colori delle persone rappresentati nel calendario
	// default: 5 reperibili, 2 turnisti, 1 jolly per i turnisti
	getPersonColor: function(jolly) {
		var colors;
        if (this.category == 'reperibility') {
                colors = new Array("MEDIUMSEAGREEN", "GOLDENROD", "CORNFLOWERBLUE", "DARKORCHID", "TOMATO", "PINK");
        } else {
			if (jolly) { 
				colors = "CORNFLOWERBLUE";
			} else { 
				colors = new Array("TOMATO", "MEDIUMSEAGREEN"); 
			}
        }
		return colors;
	},


	// colori delle tipologie di assenze
	getAbsenceColor: function() {
		var colors = new Array("MEDIUMSEAGREEN", "GOLDENROD", "CORNFLOWERBLUE", "DARKORCHID", "TOMATO");
		return colors;
	},

	// Uri per il servizio REST di lettura di un certo tipo di
	// reperibilità o turni o assenze di persone
	getUriRestToGetEntity: function (type, param) {
		if (type == 'absence') {
			return this.epasServer + 'absenceFromJson/absenceInPeriod/' + param;
		} else {
        	return this.basicRestRoute + '/' + type + '/find/' + param;
		}
	},

	// Uri per il servizio REST di scrittura di un certo tipo di
	// reperibilità o turni
	getUriRestToPutEntity: function (type, param) {
		return this.basicRestRoute + '/' + type + '/update/' + param;
	},


	// assenze di turnisti e reperibili
	getUriRestToGetAbsence: function (type, param) {
		return this.basicRestRoute + '/' + type + '/absence/' + param;
    },


	// lista delle persone reperibili o di turno
	getUriRestToGetPersons: function (type) {
        return this.basicRestRoute + '/' + type + '/personList';
    },

    // time table di un tipo di turno
    getUriRestToGetShiftTimeTable: function (type) {
        return this.basicRestRoute + '/' + type + '/timeTable';
    },

	// lista delle tipologie di assenze più frequesnti in ePAS
	getUriRestToGetAbsenceTypes: function(param) {
		return this.basicRestRoute + 'FromJson/frequentAbsence/' + param;
	},


	// calendario annuale delle reperibilità
	getUriRestToGetYearlyPDF: function (type, param) {
		if (this.category == 'reperibility') {
        	return this.basicRestRoute + '/' + type + '/exportYearAsPDF/' + param;
			alert("la getUriRestToGetYearlyPDF da " +this.basicRestRoute + '/' + type + '/exportYearAsPDF/' + param);
		} else {
			return '';
		}
    },

	// calendario mensile dei turni
	getUriRestToGetMonthlyCalPDF: function (type, param) {
        if (this.category == 'shift') {
            return this.basicRestRoute + '/' + type + '/exportMonthCalAsPDF/' + param;
			alert("la getUriRestToGetMonthlyCalPDF da: " + this.basicRestRoute + '/' + type + '/exportMonthCalAsPDF/' + param);
        } else {
            return '';
        }
    },

	// report mensile
	getUriRestToGetMontlyPDF: function (type, param) {
       	return this.basicRestRoute + '/' + type + '/exportMonthAsPDF/' + param;
    },

	// ICal
	getUriRestToGetICal: function (type) {
		if (this.category == 'reperibility') {
                	return this.basicRestRoute + '/' + type + '/iCal/';
		} else {
			return '';
		}
  	},
  
  	selectPopup: function(allowed,  grpTypes, cType, repCalObj, repGrpObj){
		// Rendo visible la finestra di scelta
		var overlay = document.getElementById('popup-overlay');
		// Rendo visibile la finestra di selezione
		overlay.style.visibility = 'visible';
    		// Appendo un div contenitore all'overlay
		$('#popup-overlay').append('<div id=\'popup-panel\'></div>');
		// Appendo un div titolo al contenitore
		$('#popup-panel').append('<div id=\'popup-title\'></div>');
		//console.log('category='+repCalObj.category);
		// Inserisco il titolo
		$('#popup-title').append('<span>'+repCalObj.category+' calendar</span>');
		var title = document.getElementById('popup-title');
		// Appendo un div contenuto
		$('#popup-panel').append('<div id=\'popup-content\'></div>');
		var content = document.getElementById('popup-content');
		var string = "<br><strong>Scegliere il gruppo per il quale visualizzare il calendario:</strong><br><br><br>";
		// Apro la select e aggiungo il campo di default con valore nullo
		string += "Gruppo: <select id='cal'><option value=''> ------ </option>";

		// Ciclo groupTypes per creare le opzioni della select
		for (key in grpTypes) {
			if(repCalObj.category == 'shift'){
				string += "<option value='" + grpTypes[key] + "'>" + repGrpObj.shiftType[grpTypes[key]]  + "</option>";
				//console.log(repCalObj.category);
			} else if(repCalObj.category == 'absence') {
				string += "<option value='" + grpTypes[key] + "'>" + repGrpObj.absenceType[grpTypes[key]]  + "</option>";
                console.log(grpTypes[key]);
			} else {
				string += "<option value='" + grpTypes[key] + "'>" + repGrpObj.repType[grpTypes[key]]  + "</option>";
			}
		}
		// Chiudo la select
		string += "</select>";

		// Inserisco la stringa creata dentro la finestra
		content.innerHTML += string;

		// Alla scelta del calendario nascondo la finestra di scelta e creo il calendario
		$("#cal").on('change', function(){
			calendarType = parseInt($("#cal").val());
			repGrpObj.calendarChoise = calendarType;
			overlay.style.visibility = 'hidden';
			userAllowed = true;

            $.cookie('calendar', calendarType, { expires: 7, path: '/', domain: 'sistorg.iit.cnr.it' });

			//console.log('cType='+cType);
			// Chiamo la funzione di creazione del calendario e creo il cookie con il numero scelto nella select
			switch(cType){
				case 'repView':
					createCalendarRepView(userAllowed, calendarType, repCalObj, repGrpObj);
				break;
				case 'repAdmin':
					createCalendarRepAdmin(userAllowed, calendarType, repCalObj, repGrpObj);
				break;
				case 'shiftView':
					createCalendarShiftView(userAllowed, calendarType, repCalObj, repGrpObj);
				break;
				case 'shiftAdmin':
					createCalendarShiftAdmin(userAllowed, calendarType, repCalObj, repGrpObj);
				break;
			}
		});
	}

}
