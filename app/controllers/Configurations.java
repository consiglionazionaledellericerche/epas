package controllers;

/*
 * Variabile config						|	Parametro html							|	Restrizioni implementate
 * _____________________________________|___________________________________________|_________________________________________________________
 * 
 * inUse								|	inUse									|
 * beginDate							|	inizioValiditaParametri					|
 * endDate	 							|	fineValiditaParametri					|
 * initUseProgram	 					|	inizioUsoProgramma						|
 * instituteName						|	nomeIstituto							|
 * emailToContact	  					|	email									|
 * seatCode								|	codiceSede								|	>0										html5
 * urlToPresence	 					|	urlPresenze								|
 * userToPresence	 					|	userPresenze							|
 * passwordToPresence			 		|	passwordPresenze						|
 * numberOfViewingCoupleColumn	 		|	colonneEntrataUscita					|	>0
 * dayOfPatron							|	giornoPatrono							|	data valida					
 * monthOfPatron						|	mesePatrono								|	data valida
 * 
 * insertAndModifyWorkingTimeWith		|											|
 * PlusToReduceAtRealWorkingTime		|	configurazioneSegnoPiuPerModifica		|					
 * 
 * addWorkingTimeInExcess 				|	configurazioneTempoLavoroInEccesso		|					
 * isLastDayBeforeXmasEntire			|	configurazioneGiornoInteroPrimaNatale	|					
 * isLastDayBeforeEasterEntire			|	configurazioneGiornoInteroPrimaPasqua	|			
 * isLastDayOfTheYearEntire				|	configurazioneGiornoInteroUltimoDellAnno|				
 * isFirstOrLastMissionDayAHoliday		|	configurazioneMissioneInizioFineFestivo	|				
 * isHolidayInMissionAWorkingDay		|	configurazioneFestivoMissione			|							
 * monthExpiryVacationPastYear		 	|	meseScadenzaFerieAP						|	data valida					
 * dayExpiryVacationPastYear		 	|	giornoScadenzaFerieAP					|	data valida
 * 
 * minimumRemainingTime					|											|
 * ToHaveRecoveryDay					|	tempoMinimoPerAvereRiposoCompensativo	|	>0
 * 
 * monthExpireRecoveryDaysOneThree		|	meseUtilizzoResiduiAP13					|	null || mese valido
 * monthExpireRecoveryDaysFourNine		|	meseUtilizzoResiduiAP49					|	null || mese valido
 * maxRecoveryDaysOneThree				|	maxGiorniRecupero13						|	null || >0
 * maxRecoveryDaysFourNine				|	maxGiorniRecupero49						|	null || >0
 * maximumOvertimeHours					|	oreMassimeStraordinarioMensili			|	null || >0
 * 
 * residual								|	configurazioneCompensazioneResidui		|
 * 										|	ConAnnoPrecedente						|	ResidualWithPastYear Enum
 * 								
 * holydaysAndVacationsOverPermitted 	|	configurazioneInserimento				|				
 * 										|	ForzatoFeriePermessi					|
 * 
 * capacityOneThree						|	configurazioneCapienzaRiposi13			|	CapacityCompensatoryRestOneThree Enum
 * capacityFourEight					|	configurazioneCapienzaRiposi49			|	CapacityCompensatoryRestFourEight Enum
 * hourMaxToCalculateWorkTime			|	oraMaxEntroCuiCalcolareUscita			|	ora valida [0-23]							html5
 * canPeopleAutoDeclareWorkingTime		|	configurazioneAutoDichiarazione			|		
 * canPeopleAutoDeclareAbsences			|	configurazioneAutoAssenze				|	
 * canPeopleUseWebStamping				|	configurazioneTimbraturaWeb				|
 *  
 */

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.DateUtility;
import models.WebStampingAddress;
import models.WorkingTimeType;
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
	public static void save(){
		
		Configuration config = null;
		List<Configuration> list = null;
		
		Long configId = params.get("configId", Long.class);
		if(configId==null)
		{
			//nuova configurazione
			 config = new Configuration();
			 config.inUse = false;
		}
		else
		{
			//modifica configurazione
			config = Configuration.findById(configId);
			if(config==null)
			{
				flash.error(String.format("La configurazione che si vuole modificare e' inesistente"));
				render("@Stampings.redirectToIndex");
			}
		}

		Boolean inUse = params.get("inUse", Boolean.class);
		if(inUse==true)
		{
			//mettere not in used tutte le altre
			list = Configuration.getAllConfiguration();
			for(Configuration c : list)
			{
				if(c.id!=configId)
					c.inUse = false;
			}
		}

		if(config.inUse==true && inUse==false)
		{
			flash.error(String.format("La configurazione attuale non può transire dallo stato inUse allo stato notInUse, operazione annullata"));
			render("@list", config);
		}
		config.inUse = inUse;
		
		
		//**********************************************//
		//	Dati di connessione e di visualizzazione	//
		//**********************************************//
		if(!isParamPositiveNumber("codiceSede"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'codice sede'"));
			render("@goback");
		}
		if(!isParamPositiveNumber("colonneEntrataUscita"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'numero colonne entrata uscita'"));
			render("@goback");
		}
		
		config.beginDate = new LocalDate(params.get("inizioValiditaParametri")).toDate();				
		config.endDate = new LocalDate(params.get("fineValiditaParametri")).toDate();
		config.initUseProgram = new LocalDate(params.get("inizioUsoProgramma")).toDate();		
		config.instituteName = params.get("nomeIstituto");										
		config.emailToContact  = params.get("email");											
		config.seatCode = Integer.parseInt(params.get("codiceSede"));
		config.urlToPresence = params.get("urlPresenze");										
		config.userToPresence = params.get("userPresenze");									
		config.passwordToPresence = params.get("passwordPresenze");							
		config.numberOfViewingCoupleColumn = Integer.parseInt(params.get("colonneEntrataUscita"));	
		
		
		
		//**********************************//
		//	Festivita						//
		//**********************************//
		if(!isParamProperData("mesePatrono", "giornoPatrono"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'Festa del Patrono'"));
			render("@goback");
		}
		
		config.dayOfPatron = Integer.parseInt(params.get("giornoPatrono"));					
		config.monthOfPatron = Integer.parseInt(params.get("mesePatrono"));		
	

		//**********************************//
		//	Pausa pranzo e buoni (Disabled)	//
		//**********************************//
		//		if(!isParamPositiveNumber("tempoLavoroGiornalieroPerBuono"))
		//		{
		//			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo lavoro giornaliero per buono'"));
		//			Application.indexAdmin();
		//		}
		//		if(!isParamPositiveNumber("tempoIntervalloPasto"))
		//		{
		//			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo intervallo pasto'"));
		//			Application.indexAdmin();
		//		}
		//		config.workingTime = Integer.parseInt(params.get("tempoLavoroGiornalieroPerBuono"));
		//		config.workingTimeToHaveMealTicket = Integer.parseInt(params.get("tempoIntervalloPasto"));	
		
		
		
		//**********************************//
		//	Orari e tempi di lavoro			//
		//**********************************//
			
		config.insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime = params.get("configurazioneSegnoPiuPerModifica", Boolean.class);								
		config.addWorkingTimeInExcess = params.get("configurazioneTempoLavoroInEccesso", Boolean.class);							
		config.isLastDayBeforeXmasEntire = params.get("configurazioneGiornoInteroPrimaNatale", Boolean.class);						
		config.isLastDayBeforeEasterEntire = params.get("configurazioneGiornoInteroPrimaPasqua", Boolean.class);						
		config.isLastDayOfTheYearEntire = params.get("configurazioneGiornoInteroUltimoDellAnno", Boolean.class);				
		config.isFirstOrLastMissionDayAHoliday = params.get("configurazioneMissioneInizioFineFestivo", Boolean.class);					
		config.isHolidayInMissionAWorkingDay = params.get("configurazioneFestivoMissione", Boolean.class);										
		
		//TODO params.get("tempoMedioLavoroGiornaliero"); 
		//(Tempo di lavoro giornaliero default (in minuti) valore medio se il tempo di lavoro cambia di giorno in giorno: 432)	
		//per adesso non viene considerato
		
		//**************************************************//
		//	Ferie, permessi, straordinari e residui			//
		//**************************************************//
		if(!isParamProperData("meseScadenzaFerieAP", "giornoScadenzaFerieAP"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'giorno scadenza ferie dell'anno precedente'"));
			render("@goback");
		}
		if(!isParamPositiveNumber("tempoMinimoPerAvereRiposoCompensativo"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo minimo per avere riposo compensativo'"));
			render("@goback");
		}	
		if(!isParamZero("meseUtilizzoResiduiAP13") && !isParamProperMonth("meseUtilizzoResiduiAP13"))	//ne null ne mese
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'mese utilizzo residui livelli 1-3'"));
			render("@goback");
		}
		if(!isParamZero("meseUtilizzoResiduiAP49") && !isParamProperMonth("meseUtilizzoResiduiAP49"))	//ne null ne mese
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'mese utilizzo residui livelli 4-9'"));
			render("@goback");
		}
		if(!isParamZero("maxGiorniRecupero13") && !isParamPositiveNumber("maxGiorniRecupero13"))		//ne null ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'massimo giorni di recupero livelli 1-3'"));
			render("@goback");
		}
		if(!isParamZero("maxGiorniRecupero49") && !isParamPositiveNumber("maxGiorniRecupero49"))		//ne null ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'massimo giorni di recupero livelli 4-9'"));
			render("@goback");
		}
		if(!isParamZero("oreMassimeStraordinarioMensili") && !isParamPositiveNumber("oreMassimeStraordinarioMensili"))		//ne null ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'ore massime straordinario mensili'"));
			render("@goback");
		}
		
		
		config.monthExpiryVacationPastYear = Integer.parseInt(params.get("meseScadenzaFerieAP"));										
		config.dayExpiryVacationPastYear = Integer.parseInt(params.get("giornoScadenzaFerieAP"));		
		config.minimumRemainingTimeToHaveRecoveryDay = Integer.parseInt(params.get("tempoMinimoPerAvereRiposoCompensativo"));	
		if(params.get("meseUtilizzoResiduiAP13").equals(""))
		{
			config.monthExpireRecoveryDaysOneThree = null;
		}
		else
		{
			config.monthExpireRecoveryDaysOneThree = Integer.parseInt(params.get("meseUtilizzoResiduiAP13"));	
		}
		if(params.get("meseUtilizzoResiduiAP49").equals(""))
		{
			config.monthExpireRecoveryDaysFourNine = null;
		}
		else
		{
			config.monthExpireRecoveryDaysFourNine = Integer.parseInt(params.get("meseUtilizzoResiduiAP49"));	
		}
		if(params.get("maxGiorniRecupero13").equals(""))
		{
			config.maxRecoveryDaysOneThree = null;
		}
		else
		{
			config.maxRecoveryDaysOneThree = Integer.parseInt(params.get("maxGiorniRecupero13"));	

		}
		if(params.get("maxGiorniRecupero49").equals(""))
		{
			config.maxRecoveryDaysFourNine = null;
		}
		else
		{
			config.maxRecoveryDaysFourNine = Integer.parseInt(params.get("maxGiorniRecupero49"));	
		}

		String configurazioneCompensazioneResiduiConAnnoPrecedente = params.get("configurazioneCompensazioneResiduiConAnnoPrecedente");	
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("entroMese"))
			config.residual = ResidualWithPastYear.atMonthInWhichCanUse;
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("mese"))
			config.residual = ResidualWithPastYear.atMonth;
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("giorno"))
			config.residual = ResidualWithPastYear.atDay;

		if(params.get("oreMassimeStraordinarioMensili").equals(""))
		{
			config.maximumOvertimeHours = null;
		}
		else
		{
			config.maximumOvertimeHours = Integer.parseInt(params.get("oreMassimeStraordinarioMensili"));	
		}
		
		config.holydaysAndVacationsOverPermitted  = params.get("configurazioneInserimentoForzatoFeriePermessi", Boolean.class);

		String configurazioneCapienzaRiposi13 = params.get("configurazioneCapienzaRiposi13");	
		if(configurazioneCapienzaRiposi13.equals("residuoGiorno"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onDayResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineMese"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndOfMonthResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineMesePrecedente"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastMonthResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineTrimestre"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastQuarterResidual;


		String configurazioneCapienzaRiposi49 = params.get("configurazioneCapienzaRiposi49");
		if(configurazioneCapienzaRiposi49.equals("residuoGiorno"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onDayResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineMese"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndOfMonthResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineMesePrecedente"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastMonthResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineTrimestre"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastQuarterResidual;
		
		
		
		//**************************************************//
		//	Uscite e autodichiarazioni						//
		//**************************************************//
		if(!isParamProperHour("oraMaxEntroCuiCalcolareUscita"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'ora massima entro cui calcolare l'uscita mancante'"));
			render("@goback");
		}
		config.hourMaxToCalculateWorkTime = params.get("oraMaxEntroCuiCalcolareUscita", Integer.class);		
		config.canPeopleAutoDeclareWorkingTime = params.get("configurazioneAutoDichiarazione", Boolean.class);	
		config.canPeopleAutoDeclareAbsences = params.get("configurazioneAutoAssenze", Boolean.class);	
		
		//**************************************************//
		//	Timbratura via web								//
		//**************************************************//
		
		config.canPeopleUseWebStamping = params.get("configurazioneTimbraturaWeb", Boolean.class);			
		//get text
	
		//**************************************************//
		//	Info per stampa in pdf							//
		//**************************************************//
		// get text
		
		//salvataggio
		
		config.save();
		if(list!=null)
		{
			for(Configuration c : list)
			{
				if(configId!=c.id)
					c.save();
			}
		}
		
		flash.success(String.format("Configurazione modificata con successo!"));
		render("@Stampings.redirectToIndex");
		
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
		flash.clear();
		List<Configuration> configList = Configuration.findAll();
		render(configList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void manageExitCausal(){
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void insertExitCausal(){
		
	}
	
	
	/**
	 * 
	 * @param param
	 * @return true se il parametro e' stringa vuota, false altrimenti
	 */
	public static boolean isParamNull(String param)
	{
		String paramString = params.get(param);
		if(paramString.equals(""))
		{
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param param
	 * @return true se il parametro e' un intero maggiore di 0, false altrimenti
	 */
	public static boolean isParamPositiveNumber(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>=0)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il parametro e' 0, false altrimenti
	 */
	public static boolean isParamZero(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt==0)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param paramMonth
	 * @param paramDay
	 * @return true se i parametri day e month compongono una data valida, false altrimenti
	 */
	public static boolean isParamProperData(String paramMonth, String paramDay)
	{
		try
		{
			Integer month = Integer.parseInt(params.get(paramMonth));										
			Integer day = Integer.parseInt(params.get(paramDay));					
			if(! DateUtility.isFebruary29th(month, day))
			{
				new LocalDate().withMonthOfYear(month).withDayOfMonth(day);
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il paramentro passato e' compreso fra 1 e 12, false altrimenti
	 */
	public static boolean isParamProperMonth(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>0 && paramInt<13)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il paramentro passato e' compreso fra 0 e 23, false altrimenti
	 */
	public static boolean isParamProperHour(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>-1 && paramInt<24)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
}
