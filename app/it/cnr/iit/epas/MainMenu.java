/**
 * 
 */
package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import models.Permission;
import models.Person;
import models.UsersPermissionsOffices;

import org.joda.time.LocalDate;

import play.Logger;
import play.cache.Cache;
import controllers.Security;

/**
 * @author cristian
 *
 */
@Getter
public class MainMenu {

	public static final String PERSON_ID_CACHE_PREFIX = "personId.";
	
	@Setter
	private Long personId = null;

	private int year;
	private int month;
	private Integer day;

	@Setter
	private ActionMenuItem method = null;

	private List<Person> persons;
	
	public MainMenu(int year, int month) {
		this.year = year;
		this.month = month;
	}
	
	public MainMenu(int year, int month, int day){
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public MainMenu(int year, int month, ActionMenuItem method){
		this.year = year;
		this.month = month;
		this.method = method;
	}
	
	public MainMenu(Long personId, int year, int month, ActionMenuItem method) {
		this.personId = personId;
		this.year = year; this.month = month;
		this.method = method;
	}
	
	public MainMenu(Long personId, int year, int month, int day, ActionMenuItem method){
		this.personId = personId;
		this.year = year;
		this.month = month;
		this.day = day;
		this.method = method;
	}
	
	public MainMenu(Long personId, int year, int month, ActionMenuItem action, List<Person> persons) {
		this(personId, year, month, action);
		this.persons = persons;
	}
	
	public MainMenu(Long personId, int year, int month, int day, ActionMenuItem action, List<Person> persons) {
		this(personId, year, month, day, action);
		this.persons = persons;
	}
	
	//NON USATO DA NESSUNA PARTE
	public Person getPerson() {
		
		if (personId == null) {
			return null;
		}
		
		Person person = Cache.get(PERSON_ID_CACHE_PREFIX + personId, Person.class);
		
		if (person == null) {
			Logger.debug("L'id della persona è: %s", personId);
			person = Person.findById(personId);

			Cache.set(PERSON_ID_CACHE_PREFIX + personId, person, "30mn");
		}
		
		return person;
	}
	
	public List<ActionMenuItem> getMethods() {
        
		List<ActionMenuItem> actions = new ArrayList<ActionMenuItem>();
		
		//Set<UsersPermissionsOffices> permissions = Security.getUser().getAllPermissions();
		List<UsersPermissionsOffices> permissions = Security.getUser().getAllPermissions();
		
		Set<String> permissionDescriptions = new HashSet<String>();
		for(UsersPermissionsOffices p : permissions){
			permissionDescriptions.add(p.permission.description);
			
		}
		
        for (ActionMenuItem menuItem : ActionMenuItem.values()) {
            if (permissionDescriptions.contains(menuItem.getPermission())) {
                actions.add(menuItem);
            }
            
        }
        if(actions.size() > 8){
        	for(int i=0; i < actions.size(); i++){
        		if(actions.get(i).toString().equals("stampings")){
        			actions.add(i, ActionMenuItem.separateMenu);
        			return actions;
        		}
        	}
        }
        return actions;
    } 
	
	public List<Integer> getDays() {
		List<Integer> days = new ArrayList<Integer>();	
		LocalDate date = new LocalDate().withYear(year).withMonthOfYear(month);
		for(Integer i = 1; i <= date.dayOfMonth().withMaximumValue().getDayOfMonth(); i++){
			days.add(i);
		}
		return days;
	}
}
