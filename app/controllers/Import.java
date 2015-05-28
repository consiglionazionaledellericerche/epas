package controllers;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import models.Office;
import play.Logger;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.With;
import cnr.sync.consumers.OfficeConsumer;
import cnr.sync.dto.OfficeDTO;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;


@With( {Resecure.class, RequestInit.class} )
public class Import extends Controller{
	
	@Inject
	static OfficeConsumer officeConsumer;

	public static void officeList(){

		List<OfficeDTO> importedOffices = Lists.newArrayList();
		
		try {
			importedOffices = officeConsumer.getOffices().get();
		} catch (IllegalStateException | InterruptedException
				| ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		render(importedOffices);
	}		
	
	public static void importOffices(List<Long> offices,List<OfficeDTO> importedOffices){
		
		Logger.info("id : %s", offices);
		Logger.info("importedOffices : %s", importedOffices);
		
	}

}