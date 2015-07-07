package controllers;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import models.Office;
import models.Person;
import play.Logger;
import play.cache.Cache;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;
import cnr.sync.consumers.OfficeConsumer;
import cnr.sync.consumers.PeopleConsumer;
import cnr.sync.dto.SeatDTO;
import cnr.sync.manager.RestOfficeManager;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;


@With( {Resecure.class, RequestInit.class} )
public class Import extends Controller{
	
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static OfficeConsumer officeConsumer;
	@Inject
	private static PeopleConsumer peopleConsumer;
	@Inject
	private static RestOfficeManager restOfficeManager;
	
	private final static String IMPORTED_OFFICES = "importedOffices";
	
	public static void officeList(){
		
		List<SeatDTO> importedOffices = Lists.newArrayList();
		
		try {
			importedOffices = officeConsumer.getOffices().get();
		} catch (IllegalStateException | InterruptedException
				| ExecutionException e) {
			flash.error("Impossibile recuperare la lista degli istituti da Perseo");
			e.printStackTrace();
		}
		
		Cache.add(IMPORTED_OFFICES, importedOffices);
		
		render(importedOffices);
	}		
	
	public static void importOffices(@Required final List<Integer> offices){
		
		if(validation.hasErrors()){
			flash.error("Selezionare almeno una Sede da importare");
			officeList();
		}
		
		List<SeatDTO> importedOffices = Cache.get(IMPORTED_OFFICES, List.class);
		
		if(importedOffices == null ){
			
			try {
				importedOffices = officeConsumer.getOffices().get();
			} catch (IllegalStateException | InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Collection<SeatDTO> filteredOffices = Collections2.filter(importedOffices, 
				new Predicate<SeatDTO>() {
					@Override
					public boolean apply(SeatDTO input) {
						return offices.contains(input.id);
					}
				});
		
		int synced = restOfficeManager.saveImportedSeats(filteredOffices);
		if(synced == 0){
			flash.error("Non è stato possibile importare/sincronizzare alcuna sede, controllare i log");
		}
		else{
			flash.success("Importate/sincronizzate correttamente %s Sedi",synced);
		}
		Offices.showOffices();
	}
	
	public static void syncSeatPeople(@Required Long id){
		if(validation.hasErrors()){
			flash.error("Impossibile effettuare la sincronizzazione della sede");
			Offices.showOffices();
		}
		Office seat = officeDao.getOfficeById(id);
		
		Set<Person> importedPeople = Sets.newHashSet();
				
		try {
			importedPeople = peopleConsumer.seatPeople(seat.code).get();
		} catch (IllegalStateException | InterruptedException
				| ExecutionException e) {
			flash.error("Impossibile recuperare la lista degli istituti da Perseo");
			e.printStackTrace();
		}
//		
//		for(Person p : importedPeople ){
//			Logger.info("Persone Importata: %s-%s-%s-%s-%s-%s",p.fullName(),p.birthday,p.email,p.cnr_email,p.number,p.badgeNumber);
//		}

	}
	

}
