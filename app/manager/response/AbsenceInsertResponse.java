package manager.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.joda.time.LocalDate;

@Data
public class AbsenceInsertResponse {

	public final static String CODICE_FERIE_GIA_PRESENTE = 
			"Il codice di assenza è già presente in almeno uno dei giorni in cui lo si voleva inserire. Controllare.";
	public final static String CODICE_GIORNALIERO_GIA_PRESENTE = 
			"Esiste già un codice di assenza giornaliero nel periodo indicato. Operazione annullata.";
	public final static String NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO = 
			"Nessun codice ferie disponibile per il periodo richiesto";
	public final static String FERIE_IN_GIORNO_FESTIVO = "Le ferie non possono essere prese in un giorno festivo";
	public final static String RIPOSI_COMPENSATIVI_ESAURITI = 
			"Numero di giorni di riposo compensativo esauriti per l'anno corrente";
	public final static String MONTE_ORE_INSUFFICIENTE = 
			"Monte ore insufficiente per l'assegnamento del riposo compensativo";
	public final static String CODICE_NON_WEEKEND = 
			"Codice non assegnabile a un giorno festivo";
	public final static String CODICI_MALATTIA_FIGLI_NON_DISPONIBILE = 
			"Impossibile usufruire dei codici ferie per malattia dei figli";
	public final static String NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37 = 
			"Nessun codice ferie dell'anno precedente 37 utilizzabile";

	private LocalDate date;
	private String absenceCode;
	private String warning;
	private boolean insertSucceeded = false;	
	private boolean isHoliday = false;
	private boolean InTrouble = false;
	private boolean isDayInReperibilityOrShift = false;	

	public AbsenceInsertResponse(LocalDate date, String absenceCode) {
		super();
		this.date = date;
		this.absenceCode = absenceCode;
	}

}
