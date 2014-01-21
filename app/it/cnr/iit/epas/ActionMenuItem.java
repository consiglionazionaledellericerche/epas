package it.cnr.iit.epas;

/**
 * @author cristian
 *
 */
public enum ActionMenuItem {
	stampingsAdmin("Timbrature", "insertAndUpdateStamping"),
    personList("Lista persone", "insertAndUpdatePerson"),
    yearlyAbsences("Assenze annuali", "insertAndUpdateAbsence"),
//    absencesAdmin("Gestione assenze", "insertAndUpdateAbsence"),
    vacationsAdmin("Gestione ferie e permessi", "insertAndUpdateVacations"),
    competencesAdmin("Gestione competenze", "insertAndUpdateCompetences"),    
    manageWorkingTime("Gestione orari di lavoro", "insertAndUpdateWorkingTime"),
    confParameters("Configurazione parametri", "insertAndUpdateConfiguration"),
    offices("Gestione sedi", "insertAndUpdateOffices"),
    administrator("Gestione amministratori", "insertAndUpdateAdministrator"),
    totalMonthlyAbsences("Totale assenze mensili", "insertAndUpdateAbsence"),
    monthRecap("Riepilogo mensile", "insertAndUpdatePerson"),
    missingStamping("Timbrature mancanti", "insertAndUpdateStamping"),
    dailyPresence("Presenza giornaliera", "insertAndUpdatePerson"),
    mealTicketSituation("Situazione buoni mensa", "insertAndUpdatePerson"),
    manageAbsenceCode("Gestione codici d'assenza", "insertAndUpdateAbsence"),
    manageCompetence("Gestione codici competenze", "insertAndUpdateCompetences"),
    printTag("Stampa cartellino", "insertAndUpdateStamping"),
    uploadSituation("Attestati presenza", "uploadSituation"),
    separateMenu("-----------------------------------------------",""),    
	stampings("Situazione mensile", "viewPersonalSituation"),
	changePassword("Gestione password", "viewPersonalSituation"),
	absences("Assenze mensili", "viewPersonalSituation"),
	absencesperperson("Assenze annuali", "viewPersonalSituation"),
	vacations("Ferie", "viewPersonalSituation"),
	competences("Competenze", "viewPersonalSituation"),
	hourrecap("Riepilogo orario", "viewPersonalSituation"),
	printPersonTag("Stampa cartellino presenze", "viewPersonalSituation");
	//changePassword("", "");

    
    private String description;
    private String permission;

    private ActionMenuItem(String description, String permission) {
        this.description = description;
        this.permission = permission;
    }

    public String getAction() {
        return this.name();
    }

    public String getDescription() {
        return this.description;
    }
    public String getPermission() {
        return permission;
    } 
}
