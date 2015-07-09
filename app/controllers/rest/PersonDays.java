package controllers.rest;

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import helpers.JsonResponse;
import manager.ContractMonthRecapManager;
import models.Absence;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.Stamping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;

import cnr.sync.dto.PersonDayDTO;
import cnr.sync.dto.PersonMonthDTO;
import play.mvc.Controller;
import play.mvc.With;
import controllers.Resecure;
import controllers.Stampings;
import controllers.Resecure.BasicAuth;
import dao.ContractMonthRecapDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

//@With(Resecure.class)
public class PersonDays extends Controller{

	@Inject
	static PersonDao personDao;
	@Inject
	static PersonDayDao personDayDao;
	@Inject
	static IWrapperFactory wrapperFactory;
	

	@BasicAuth
	public static void getDaySituation(String email, LocalDate date){
		Person person = personDao.byEmail(email).orNull();
		if(person == null){
			JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
					+ "mail cnr che serve per la ricerca.");
		}
		PersonDay pd = personDayDao.getPersonDay(person, date).orNull();
		if(pd == null){
			JsonResponse.notFound("Non sono presenti informazioni per "
					+person.name+" "+person.surname+ " nel giorno "+date);
		}
		PersonDayDTO pdDTO = new PersonDayDTO();
		pdDTO.buonopasto = pd.isTicketAvailable;
		pdDTO.differenza = pd.difference;
		pdDTO.progressivo = pd.progressive;
		pdDTO.tempolavoro = pd.timeAtWork;
		if(pd.absences != null && pd.absences.size() > 0){
			for(Absence abs : pd.absences){
				pdDTO.codiceassenza.add(abs.absenceType.code);
			}	
		}
		if(pd.stampings != null && pd.stampings.size() > 0){
			for(Stamping s : pd.stampings){
				pdDTO.timbrature.add(s.date.toString());
			}
		}
		renderJSON(pdDTO);
	}
	
	
	public static void getMonthSituation(String email, int month, int year){
		Person person = personDao.byEmail(email).orNull();
		if(person == null){
			JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
					+ "mail cnr che serve per la ricerca.");
		}
		List<Contract> monthContracts = wrapperFactory
				.create(person).getMonthContracts(year, month);
		PersonMonthDTO pmDTO = new PersonMonthDTO();
		for(Contract contract : monthContracts) {
			Optional<ContractMonthRecap> cmr = wrapperFactory.create(contract)
					.getContractMonthRecap(new YearMonth(year, month));
			if(cmr.isPresent()){
				
				//TODO: continuare a prendere i valori da restituire
				pmDTO.buoniMensa = cmr.get().remainingMealTickets;
				pmDTO.possibileUtilizzareResiduoAnnoPrecedente = cmr.get().possibileUtilizzareResiduoAnnoPrecedente;
				pmDTO.progressivoFinaleMese = cmr.get().progressivoFinaleMese;
				pmDTO.straordinari = cmr.get().straordinariMinuti;
				pmDTO.residuoTotaleAnnoCorrente = cmr.get().remainingMinutesCurrentYear;
				pmDTO.residuoTotaleAnnoPassato = cmr.get().remainingMinutesLastYear;
			}
			else{
				JsonResponse.notFound("Non sono presenti informazioni per "
						+person.name+" "+person.surname+ " nel mese di "+DateUtility.fromIntToStringMonth(month));
			}
		}
		renderJSON(pmDTO);
		
	}

}
