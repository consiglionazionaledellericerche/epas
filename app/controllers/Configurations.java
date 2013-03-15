package controllers;

import java.util.Date;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.WebStampingAddress;
import play.mvc.Controller;
import play.mvc.With;
import models.Configuration;
import models.enumerate.CapacityCompensatoryRestFourEight;
import models.enumerate.CapacityCompensatoryRestOneThree;
import models.enumerate.ResidualWithPastYear;

@With( {Secure.class, NavigationMenu.class} )
public class Configurations extends Controller{
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void show(){
		Date now = new Date();
		Configuration configurations = Configuration.getConfiguration(now);

		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void save(Long configId, Configuration configuration){
		if(configId == null){
			Configuration config = new Configuration();
			config.beginDate = params.get("inizioValiditaParametri", Date.class);
			config.endDate = params.get("fineValiditaParametri", Date.class);
			config.initUseProgram = params.get("inizioUsoProgramma", Date.class);
			config.instituteName = params.get("nomeIstituto");
			config.emailToContact = params.get("email", String.class);
			config.seatCode = params.get("codiceSede", Integer.class);
			config.urlToPresence = params.get("urlPresenze");
			config.userToPresence = params.get("userPresenze");
			config.passwordToPresence = params.get("passwordPresenze");
			config.numberOfViewingCoupleColumn = params.get("colonneEntrataUscita", Integer.class);
			config.dayOfPatron = params.get("giornoPatrono", Integer.class);
			config.monthOfPatron = params.get("mesePatrono", Integer.class);
			/**
			 * TODO: il pezzo sulla show in cui si tratta la gestione della pausa pranzo e del buono pasto
			 */
			config.workingTime = params.get("tempoLavoroGiornalieroPerBuono", Integer.class);
			config.workingTimeToHaveMealTicket = params.get("tempoIntervalloPasto", Integer.class);
			
			config.insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime = params.get("configurazioneSegnoPiuPerModifica", Boolean.class);
			config.addWorkingTimeInExcess = params.get("configurazioneTempoLavoroInEccesso", Boolean.class);			
			config.isLastDayBeforeXmasEntire = params.get("configurazioneGiornoInteroPrimaNatale", Boolean.class);
			config.isLastDayBeforeEasterEntire = params.get("configurazioneGiornoInteroPrimaPasqua", Boolean.class);
			config.isLastDayOfTheYearEntire = params.get("configurazioneGiornoInteroUltimoDellAnno", Boolean.class);
			config.isFirstOrLastMissionDayAHoliday = params.get("configurazioneMissioneInizioFineFestivo", Boolean.class);
			config.isHolidayInMissionAWorkingDay = params.get("configurazioneFestivoMissione", Boolean.class);
			/**
			 * TODO: configurazioneIgnoraTempoLavoro e configurazionePiuCodiciAssenza della show, vanno implementati anche i campi nella classe
			 * configuration
			 */
			config.monthExpiryVacationPastYear = params.get("meseScadenzaFerieAP", Integer.class);
			config.dayExpiryVacationPastYear = params.get("giornoScadenzaFerieAP", Integer.class);
			config.minimumRemainingTimeToHaveRecoveryDay = params.get("tempoMinimoPerAvereRiposoCompensativo", Integer.class);
			config.monthExpireRecoveryDaysOneThree = params.get("meseUtilizzoResiduiAP13", Integer.class);
			config.monthExpireRecoveryDaysFourNine = params.get("meseUtilizzoResiduiAP49", Integer.class);
			config.maxRecoveryDaysOneThree = params.get("maxGiorniRecupero13", Integer.class);
			config.maxRecoveryDaysFourNine = params.get("maxGiorniRecupero49", Integer.class);
			
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedenteEntroMese", Boolean.class) == true)
				config.residual = ResidualWithPastYear.atMonthInWhichCanUse;
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedenteMese", Boolean.class) == true)
				config.residual = ResidualWithPastYear.atMonth;
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedenteGiorno", Boolean.class) == true)
				config.residual = ResidualWithPastYear.atDay;
			config.maximumOvertimeHours = params.get("oreMassimeStraordinarioMensili", Integer.class);
			
			config.holydaysAndVacationsOverPermitted = params.get("configurazioneInserimentoForzatoFeriePermessi", Boolean.class);
			if(params.get("configurazioneCapienzaRiposi13Giorno", Boolean.class)== true)
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onDayResidual;
			if(params.get("configurazioneCapienzaRiposi13Mese", Boolean.class)== true)
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndOfMonthResidual;
			if(params.get("configurazioneCapienzaRiposi13MesePrec", Boolean.class)== true)
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastMonthResidual;
			if(params.get("configurazioneCapienzaRiposi13Trimestre", Boolean.class)== true)
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastQuarterResidual;
			
			if(params.get("configurazioneCapienzaRiposi48Giorno", Boolean.class)== true)
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onDayResidual;
			if(params.get("configurazioneCapienzaRiposi48Mese", Boolean.class)== true)
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndOfMonthResidual;
			if(params.get("configurazioneCapienzaRiposi48MesePrec", Boolean.class)== true)
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastMonthResidual;
			if(params.get("configurazioneCapienzaRiposi48Trimestre", Boolean.class)== true)
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastQuarterResidual;
			
			config.hourMaxToCalculateWorkTime = params.get("oraMaxEntroCuiCalcolareUscita", Integer.class);
			/**
			 * TODO: la parte di autodichiarazione di assenze e orario di lavoro e autorizzazione timbratura via web e lista degli indirizzi
			 * autorizzati alla timbratura via web
			 */
			
			
			config.save();
			flash.success(String.format("Configurazione salvata con successo!"));
			Application.indexAdmin();
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void insertConfig(){
		Configuration configurations = new Configuration();
		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void edit(Long configId){
		Configuration configurations = Configuration.findById(configId);
		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void discard(){
		Configurations.list();
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void list(){
		List<Configuration> configList = Configuration.findAll();
		render(configList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void manageExitCausal(){
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void insertExitCausal(){
		
	}
}
