package controllers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import cnr.sync.consumers.OfficeConsumer;
import cnr.sync.dto.SeatDTO;
import cnr.sync.manager.RestOfficeManager;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;


@With( {Resecure.class, RequestInit.class} )
public class Import extends Controller{
	
	@Inject
	private static OfficeConsumer officeConsumer;
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
	
	public static void importOffices(final List<Integer> offices){
		
		if(offices == null ){
			flash.error("Selezionare almeno una Sede da importare");
			officeList();
		}
		
		List<SeatDTO> importedOffices = Cache.get(IMPORTED_OFFICES, List.class);
		
		if(importedOffices == null){
			
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
		
		Logger.info("id istituti: %s", filteredOffices);
		restOfficeManager.saveImportedSeats(filteredOffices);
		
		Offices.showOffices();
	}

}