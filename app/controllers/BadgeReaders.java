package controllers;

import helpers.Web;

import javax.inject.Inject;

import models.BadgeReader;
import models.Role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.mysema.query.SearchResults;

import dao.BadgeReaderDao;
import dao.OfficeDao;
import dao.RoleDao;

@With( {Resecure.class, RequestInit.class})
public class BadgeReaders extends Controller {
	
	private static final Logger log = LoggerFactory.getLogger(BadgeReaders.class);
	
	@Inject
	private static BadgeReaderDao badgeReaderDao;
	@Inject
	private static RoleDao roleDao;

	public static void index() {
		flash.keep();
		list(null);
	}
	
	public static void list(String name){

		
		SearchResults<?> results = badgeReaderDao.badgeReaders(
				Optional.<String>fromNullable(name)).listResults();
		
		render(results, name);
	}
	
	public static void show(Long id) {
		final BadgeReader badgeReader = BadgeReader.findById(id);
		notFoundIfNull(badgeReader);
		render(badgeReader);
	}

	public static void edit(Long id) {
		
		final BadgeReader badgeReader = BadgeReader.findById(id);
		notFoundIfNull(badgeReader);
		render(badgeReader);
	}

	public static void blank() {
		final BadgeReader badgeReader = new BadgeReader();
		render("@edit", badgeReader);
	}

	public static void save(@Valid BadgeReader badgeReader) {
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", badgeReader,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			render("@edit", badgeReader);
		} else {
			badgeReader.save();
			flash.success(Web.msgSaved(BadgeReader.class));
			index();
		}
	}
	
	public static void delete(Long id) {
		final BadgeReader badgeReader = BadgeReader.findById(id);
		notFoundIfNull(badgeReader);
		
		//if(badgeReader.seats.isEmpty()) {
			badgeReader.delete();
			flash.success(Web.msgDeleted(BadgeReader.class));
			index();
		//}
		flash.error(Web.msgHasErrors());
		index();
	}
	
}
