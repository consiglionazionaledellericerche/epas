package it.cnr.iit.epas;

/**
 * @author cristian
 *
 */
public enum ActionMenuItem {
	stampingsAdmin("Timbrature", "insertAndUpdateStamping"),
    personList("Lista persone", "insertAndUpdatePerson"),
    yearlyAbsences("Assenze annuali", "insertAndUpdateAbsence"),
    absencesAdmin("Gestione assenze", "insertAndUpdateAbsence"),
    vacationsAdmin("Gestione ferie e permessi", "insertAndUpdateVacations"),
    competencesAdmin("Gestione competenze", "insertAndUpdateCompetences"),
    changePassword("Gestione password", "insertAndUpdatePassword"),
    manageWorkingTime("Gestione orari di lavoro", "insertAndUpdateWorkingTime"),
    confParameters("Configurazione parametri", "insertAndUpdateConfiguration"),
    administrator("Gestione amministratori", "insertAndUpdateAdministrator"),
    totalMonthlyAbsences("Totale assenze mensili", "insertAndUpdateAbsence"),
    manageAbsenceCode("Gestione codici d'assenza", "insertAndUpdateAbsence"),
    missingStamping("Timbrature mancanti", "insertAndUpdatePerson"),
    dailyPresence("Presenza giornaliera", "insertAndUpdatePerson"),
    mealTicketSituation("Situazione buoni mensa", "insertAndUpdatePerson"),
    manageCompetence("Gestione codici competenze", "insertAndUpdateCompetences"),
	stampings("Situazione mensile", "viewPersonalSituation"),
	absences("Assenze", "viewPersonalSituation"),
	absencesperperson("Assenze per persona", "viewPersonalSituation"),
	vacations("Ferie", "viewPersonalSituation"),
	competences("Competenze", "viewPersonalSituation"),
	hourrecap("Riepilogo orario", "viewPersonalSituation");
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
