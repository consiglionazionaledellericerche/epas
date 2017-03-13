/***************************************
/* sistCalendar Class
/* Contiene la definizione dei gruppi e i permessi per i calendari previsti
/*
/****************************************/


/***************************************
/* Inizializzazione
/****************************************/
function sistCalendarGroup (category, mode)
{
	this.category = category;
	this.mode = mode;

	// definizione dei gruppi
	//------------------------------------
	this.groupDescriptions = {
		1966: "Algoritmi e matematica computazionale",
		1967: "Segreteria Tecnica Internet Governance",
		1968: "Web Applications for the Future Internet",
		1969: "Trustworthy and Secure Future Internet",
		1970: "Ubiquitous Internet",
		1972: "Unità Operazioni e Servizi ai Registrar",
		1973: "Unità Relazioni Esterne, Media e Comunicazione",
		1974: "Segreteria Amministrativa",
		1975: "Segreteria del Personale",
		1976: "Segreteria di Direzione",
		1977: "Segreteria Scientifica",
		1978: "Ufficio Tecnico e Servizi Ausiliari",
		1979: "Rete Telematica CNR di Pisa",
		1980: "Servizi Internet e Sviluppo Tecnologico",
		1984: "Unità Operativa di Supporto di Cosenza",
	};

	// tipi di reperibilità
	this.repType = {
		1: "Registro",
		2: "Ufficio tecnico",
	//	3: "Ufficio reti",
	};

	// tipi di turno
	this.shiftType = {
		1: "Registro",
	//	2: "Ufficio tecnico",
		3: "Ufficio reti",
	};

	// tipi di assenze
	this.absenceType = {
		1: "Registro",
		2: "Ufficio tecnico",
		3: "Ufficio reti",
	};


	// associa il tipo di attività coi turni
	this.shiftServices = {
		1: ['A', 'B'],
		2: ['D'],
		3: ['C'],
	};

	// Gruppi abilitati alla visulaizzazione delle reperibilità
	this.allowedGroup4RepView = {
		1: ['1980', '1976', '1978', '1979'],  /* Segreteria, Servizi Internet e Sviluppo Tecnologico, ???? */
		2: ['1978', '1976', '1980', '1979'],  /* Segreteria, Ufficio Tecnico */
	};

	// Gruppi abilitati alla gestione delle reperibilità
  this.allowedGroup4RepAdmin = {
          1: ['1980'],
          2: ['1978', '1980'],


  };

  // Gruppi abilitati alla visulaizzazione dei turni
  this.allowedGroup4ShiftView = {
      1: ['1980', '1976', '1972'],
	  2: ['1978', '1976'],
	  3: ['1979', '1976', '1980'],
  };

  // Gruppi abilitati alla gestione dei turni
  this.allowedGroup4ShiftAdmin = {
          1: ['1980', '1972', '1973'],
          2: ['1978'],
          3: ['1979'],
  };

  this.allowedGroup4AbsenceView = {
    1: ['1980'],
    2: ['1978'],
    3: ['1979'],
  }
}



/********************************************************
// Metodi
/********************************************************/
sistCalendarGroup.prototype = {

	// Controlla che il gruppo abbia il permesso sul calendario della caegoria creata con quel tipo di rep o turno
	isAllowed: function(group, type) {
		var allowed = false;
		var groups = new Array();

		var shiftGroups = (this.mode == 'view') ? this.allowedGroup4ShiftView[type] : this.allowedGroup4ShiftAdmin[type];
		var repGroups = (this.mode == 'view') ? this.allowedGroup4RepView[type] : this.allowedGroup4RepAdmin[type];

		groups = (this.category == 'reperibility') ? repGroups : shiftGroups;

		return groups.indexOf(group) != -1;
	},

	// ritorna le tipologie di reperibilità o turni che può vedere/amministrare un gruppo
	getTypes4Group: function(group) {
		var types = new Array();
		var groups = new Array();
		var allowedGroups = new Object();

		if (this.category == 'reperibility') {
	  		allowedGroups = (this.mode == 'view') ?	this.allowedGroup4RepView : this.allowedGroup4RepAdmin;
		} else if(this.category == 'shift'){
			allowedGroups = (this.mode == 'view') ? this.allowedGroup4ShiftView : this.allowedGroup4ShiftAdmin;
		} else{
			//console.log(this);
			allowedGroups = this.allowedGroup4AbsenceView;
		}

		for (var type in allowedGroups) {
	   		groups = allowedGroups[type];
	      if (groups.indexOf(group) != -1) {
	        types.push(type);
	      }
	    }
		console.log(types);
		return types;
	},

	// Ritorna la descrizione del gruppo
	getGroupDescription: function (group) {
		return this.groupDescriptions[group];
	},

	// Ritorna la descrizione della reperibilità
	getRepDescription: function (group) {
		return this.repType[group];
	},

	// Ritorna la descrizione delle assenze
	getAbsenceDescription: function (group){
		return this.absenceType[group];
	},

	// Ritorna la descrizione del gruppo
	getShiftDescription: function (shift) {
		return this.shiftType[shift];
	},

	getShiftFromType: function (shiftType) {
		return this.shiftServices[shiftType];
	}
}
