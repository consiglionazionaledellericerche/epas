package controllers;

import com.google.common.base.Optional;
import com.mysema.query.SearchResults;
import dao.OfficeDao;
import dao.RoleDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import helpers.Web;
import models.Institute;
import models.Office;
import models.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import javax.inject.Inject;

@With( {Resecure.class, RequestInit.class})
public class Offices extends Controller {
	
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static RoleDao roleDao;

	private static final Logger log = LoggerFactory.getLogger(Offices.class);
	
	public static void index() {
		flash.keep();
		list(null);
	}
	
	public static void list(String name) {
		
		//la lista di institutes su cui si ha tecnical admin in almeno un office
		
		SearchResults<?> results = officeDao.institutes(
				Optional.<String>fromNullable(name),
				Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN))
				.listResults();
		
		render(results, name);
	}
	
	public static void show(Long id) {
		final Office office = Office.findById(id);
		notFoundIfNull(office);
		render(office);
	}

	public static void edit(Long id) {
		final Office office = Office.findById(id);
		notFoundIfNull(office);
		
		IWrapperOffice wOffice = wrapperFactory.create(office);
		
		render(office, wOffice);
	}

	public static void blank(Long instituteId) {
		final Institute institute = Institute.findById(instituteId);
		notFoundIfNull(institute);
		final Office office = new Office();
		office.institute = institute;
		
		render(office);
	}

	public static void save(@Valid Office office) {
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", office,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			IWrapperOffice wOffice = wrapperFactory.create(office);
			render("@edit", office, wOffice);
		} else {
			office.save();
			flash.success(Web.msgSaved(Office.class));
			Institutes.index();
		}
	}
	
	public static void delete(Long id) {
		
		final Office office = Office.findById(id);
		notFoundIfNull(office);
	
		// TODO: if( nessuna persona nella sede?? ) {
			office.delete();
			flash.success(Web.msgDeleted(Institute.class));
			Institutes.index();
		//}
		flash.error(Web.msgHasErrors());
		Institutes.index();
	}
	
}
