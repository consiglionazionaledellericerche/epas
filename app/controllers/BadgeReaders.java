package controllers;

import java.util.List;

import helpers.Web;

import javax.inject.Inject;

import models.BadgeReader;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import net.sf.oval.constraint.MinLength;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
	private static SecurityRules rules;
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
		
		final User user = badgeReader.user;
		render(badgeReader, user);
	}

	public static void blank() {
		
		render();
	}
	
	public static void updateInfo(@Valid BadgeReader badgeReader) {
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", badgeReader,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			render("@edit", badgeReader);
		}
		
		rules.checkIfPermitted(badgeReader.owner);
		
		badgeReader.save();
		
		flash.success(Web.msgSaved(BadgeReader.class));
		edit(badgeReader.id);
	}
	
	public static void changePassword(Long id, 
			@MinLength(5) @Required String newPass) {
		
		final BadgeReader badgeReader = BadgeReader.findById(id);
		notFoundIfNull(badgeReader);
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", badgeReader,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			render("@edit", badgeReader, newPass);
		}
		
		Codec codec = new Codec();
		badgeReader.user.password = codec.hexMD5(newPass);
		flash.success(Web.msgSaved(BadgeReader.class));
		edit(id);
		
	}

	public static void save(@Valid BadgeReader badgeReader, @Valid User user) {
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", badgeReader,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			render("@blank", badgeReader);
		} 
		if (user.password.length() < 5) { 
			response.status = 400;
			validation.addError("user.password", 
					"almeno 5 caratteri");
			render("@blank", badgeReader, user);
		}
		
		Codec codec = new Codec();
		user.password = codec.hexMD5(user.password);
		user.save();
		badgeReader.user = user;
		badgeReader.save();
		flash.success(Web.msgSaved(BadgeReader.class));
		index();
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
	
	public static void joinOffice(Long officeId) {
	
		final Office office = Office.findById(officeId);
		notFoundIfNull(office);
		
		//Lista tutti i badgeReader ancora non associati a office
		List<BadgeReader> badgeReaderList = Lists.newArrayList();
		
		UsersRolesOffices uro = new UsersRolesOffices();
		uro.office = office;
		uro.role = roleDao.getRoleByName(Role.BADGE_READER);
		
		render(uro,  badgeReaderList);
	}
	
	public static void saveJoinOffice(@Valid UsersRolesOffices uro) {
		
		if (Validation.hasErrors()) {
			response.status = 400;
			log.warn("validation errors for {}: {}", uro,
					validation.errorsMap());
			flash.error(Web.msgHasErrors());
			render("@blank", uro);
		} 
		
		rules.checkIfPermitted(uro.office);
		
		uro.save();
		
		flash.success("Lettore Badge associato correttamente.");
		flash.keep();
		Offices.edit(uro.office.id);
	}
	
	public static void unjoinOffice(Long uroId) {
		
		UsersRolesOffices uro = UsersRolesOffices.findById(uroId);
		notFoundIfNull(uro);
		
		rules.checkIfPermitted(uro.office);
		
		uro.delete();
		
		flash.success("Operazione avvenuta con successo.");
		
		Offices.edit(uro.office.id);
	}
	
}
