package controllers;

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;

import it.cnr.iit.epas.ActionMenuItem;
import models.WebStampingAddress;
import play.Logger;
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
	public static void save(Long configId){
		
		if(configId == null)
			configId = params.get("configId", Long.class);
		Logger.debug("L'id della configurazione è: %d", configId);
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
			config.canPeopleAutoDeclareWorkingTime = params.get("configurazioneAutoDichiarazione", Boolean.class);
			config.canPeopleAutoDeclareAbsences = params.get("configurazioneAutoAssenze", Boolean.class);
			config.canPeopleUseWebStamping = params.get("configurazioneTimbraturaWeb", Boolean.class);
			
			
			config.save();
			flash.success(String.format("Configurazione salvata con successo!"));
			Application.indexAdmin();
		}
		else{
			Configuration config = Configuration.findById(configId);
			Logger.debug("La configurazione caricata è: %s", config);
			String dataI = params.get("inizioValiditaParametri");
			
			LocalDate dataInizio = new LocalDate(dataI);
			Logger.debug("Data inizio configurazione: %s", dataInizio);
			if(dataInizio.toDate().compareTo(config.beginDate) != 0)
				config.beginDate = dataInizio.toDate();
			String dataF = params.get("fineValiditaParametri");
			LocalDate dataFine = new LocalDate(dataF);
			Logger.debug("Data fine configurazione: %s", dataFine);
			if(dataFine.toDate().compareTo(config.endDate) != 0)
				config.endDate = dataFine.toDate();
			String inizioUsoProgramma = params.get("fineValiditaParametri");
			LocalDate initUseProgram = new LocalDate(inizioUsoProgramma);
			Logger.debug("Data inizio uso programma: %s", initUseProgram);
			if(initUseProgram.toDate().compareTo(config.initUseProgram) != 0)
				config.initUseProgram = initUseProgram.toDate();
			
			if(!params.get("nomeIstituto").equals(config.instituteName))
				config.instituteName = params.get("nomeIstituto");
			if(!params.get("email", String.class).equals(config.emailToContact))
				config.emailToContact = params.get("email", String.class);
			if(params.get("codiceSede", Integer.class) != config.seatCode)
				config.seatCode = params.get("codiceSede", Integer.class);
			if(!params.get("urlPresenze").equals(config.urlToPresence))
				config.urlToPresence = params.get("urlPresenze");
			if(!params.get("userPresenze").equals(config.userToPresence))
				config.userToPresence = params.get("userPresenze");
			if(!params.get("passwordPresenze").equals(config.passwordToPresence))
				config.passwordToPresence = params.get("passwordPresenze");
			if(params.get("colonneEntrataUscita", Integer.class) != config.numberOfViewingCoupleColumn)
				config.numberOfViewingCoupleColumn = params.get("colonneEntrataUscita", Integer.class);
			if(params.get("giornoPatrono", Integer.class) != config.dayOfPatron)
				config.dayOfPatron = params.get("giornoPatrono", Integer.class);
			if(params.get("mesePatrono", Integer.class) != config.monthOfPatron)
				config.monthOfPatron = params.get("mesePatrono", Integer.class);
			/**
			 * TODO: vedi sopra...
			 */
			if(params.get("tempoLavoroGiornalieroPerBuono", Integer.class) != config.workingTime)
				config.workingTime = params.get("tempoLavoroGiornalieroPerBuono", Integer.class);
			if(params.get("tempoIntervalloPasto", Integer.class) != config.workingTimeToHaveMealTicket)
				config.workingTimeToHaveMealTicket = params.get("tempoIntervalloPasto", Integer.class);
			
			if(params.get("configurazioneSegnoPiuPerModifica", Boolean.class) != config.insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime)
				config.insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime = params.get("configurazioneSegnoPiuPerModifica", Boolean.class);
			if(params.get("configurazioneTempoLavoroInEccesso", Boolean.class) != config.addWorkingTimeInExcess)
				config.addWorkingTimeInExcess = params.get("configurazioneTempoLavoroInEccesso", Boolean.class);
			if(params.get("configurazioneGiornoInteroPrimaNatale", Boolean.class) != config.isLastDayBeforeXmasEntire)
				config.isLastDayBeforeXmasEntire = params.get("configurazioneGiornoInteroPrimaNatale", Boolean.class);
			if(params.get("configurazioneGiornoInteroPrimaPasqua", Boolean.class) != config.isLastDayBeforeEasterEntire)
				config.isLastDayBeforeEasterEntire = params.get("configurazioneGiornoInteroPrimaPasqua", Boolean.class);
			if(params.get("configurazioneGiornoInteroUltimoDellAnno", Boolean.class) != config.isLastDayOfTheYearEntire)
				config.isLastDayOfTheYearEntire = params.get("configurazioneGiornoInteroUltimoDellAnno", Boolean.class);
			if(params.get("configurazioneMissioneInizioFineFestivo", Boolean.class) != config.isFirstOrLastMissionDayAHoliday)
				config.isFirstOrLastMissionDayAHoliday = params.get("configurazioneMissioneInizioFineFestivo", Boolean.class);
			if(params.get("configurazioneFestivoMissione", Boolean.class) != config.isHolidayInMissionAWorkingDay)
				config.isHolidayInMissionAWorkingDay = params.get("configurazioneFestivoMissione", Boolean.class);
			/**
			 * TODO: vedi sopra...
			 */
			if(params.get("meseScadenzaFerieAP", Integer.class) != config.monthExpiryVacationPastYear)
				config.monthExpiryVacationPastYear = params.get("meseScadenzaFerieAP", Integer.class);
			if(params.get("giornoScadenzaFerieAP", Integer.class) != config.dayExpiryVacationPastYear)
				config.dayExpiryVacationPastYear = params.get("giornoScadenzaFerieAP", Integer.class);
			if(params.get("tempoMinimoPerAvereRiposoCompensativo", Integer.class) != config.minimumRemainingTimeToHaveRecoveryDay)
				config.minimumRemainingTimeToHaveRecoveryDay = params.get("tempoMinimoPerAvereRiposoCompensativo", Integer.class);
			if(params.get("meseUtilizzoResiduiAP13", Integer.class) != config.monthExpireRecoveryDaysOneThree)
				config.monthExpireRecoveryDaysOneThree = params.get("meseUtilizzoResiduiAP13", Integer.class);
			if(params.get("meseUtilizzoResiduiAP49", Integer.class) != config.monthExpireRecoveryDaysFourNine)
				config.monthExpireRecoveryDaysFourNine = params.get("meseUtilizzoResiduiAP49", Integer.class);
			if(params.get("maxGiorniRecupero13", Integer.class) != config.maxRecoveryDaysOneThree)
				config.maxRecoveryDaysOneThree = params.get("maxGiorniRecupero13", Integer.class);
			if(params.get("maxGiorniRecupero49", Integer.class) != config.maxRecoveryDaysFourNine)
				config.maxRecoveryDaysFourNine = params.get("maxGiorniRecupero49", Integer.class);
			
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedente").equals("entroMese"))
				config.residual = ResidualWithPastYear.atMonthInWhichCanUse;
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedente").equals("mese"))
				config.residual = ResidualWithPastYear.atMonth;
			if(params.get("configurazioneCompensazioneResiduiConAnnoPrecedente").equals("giorno"))
				config.residual = ResidualWithPastYear.atDay;
			
			if(params.get("oreMassimeStraordinarioMensili", Integer.class) != config.maximumOvertimeHours)
				config.maximumOvertimeHours = params.get("oreMassimeStraordinarioMensili", Integer.class);
			if(params.get("configurazioneInserimentoForzatoFeriePermessi", Boolean.class) != config.holydaysAndVacationsOverPermitted)
				config.holydaysAndVacationsOverPermitted = params.get("configurazioneInserimentoForzatoFeriePermessi", Boolean.class);
			
			if(params.get("configurazioneCapienzaRiposi13").equals("residuoGiorno"))
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onDayResidual;
			if(params.get("configurazioneCapienzaRiposi13").equals("residuoFineMese"))
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndOfMonthResidual;
			if(params.get("configurazioneCapienzaRiposi13").equals("residuoFineMesePrecedente"))
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastMonthResidual;
			if(params.get("configurazioneCapienzaRiposi13").equals("residuoFineTrimestre"))
				config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastQuarterResidual;
			
			if(params.get("configurazioneCapienzaRiposi49").equals("residuoGiorno"))
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onDayResidual;
			if(params.get("configurazioneCapienzaRiposi49").equals("residuoFineMese"))
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndOfMonthResidual;
			if(params.get("configurazioneCapienzaRiposi49").equals("residuoFineMesePrecedente"))
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastMonthResidual;
			if(params.get("configurazioneCapienzaRiposi49").equals("residuoFineTrimestre"))
				config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastQuarterResidual;
			
			if(params.get("oraMaxEntroCuiCalcolareUscita", Integer.class) != config.hourMaxToCalculateWorkTime)
				config.hourMaxToCalculateWorkTime = params.get("oraMaxEntroCuiCalcolareUscita", Integer.class);
			if(params.get("configurazioneAutoDichiarazione", Boolean.class) != config.canPeopleAutoDeclareWorkingTime)
				config.canPeopleAutoDeclareWorkingTime = params.get("configurazioneAutoDichiarazione", Boolean.class);
			if(params.get("configurazioneAutoAssenze", Boolean.class) != config.canPeopleAutoDeclareAbsences)
				config.canPeopleAutoDeclareAbsences = params.get("configurazioneAutoAssenze", Boolean.class);
			if(params.get("configurazioneTimbraturaWeb", Boolean.class) != config.canPeopleUseWebStamping)
				config.canPeopleUseWebStamping = params.get("configurazioneTimbraturaWeb", Boolean.class);
		
			config.save();
			flash.success(String.format("Configurazione modificata con successo!"));
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
