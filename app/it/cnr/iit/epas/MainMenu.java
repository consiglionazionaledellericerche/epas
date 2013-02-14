/**
 * 
 */
package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;

import controllers.Security;

import play.Logger;
import play.cache.Cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import models.Permission;
import models.Person;

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
		
		Set<Permission> permissions = Security.getPerson().getAllPermissions();
		
		Set<String> permissionDescriptions = new HashSet<String>();
		for(Permission p : permissions){
			permissionDescriptions.add(p.description);
			
		}
		
        for (ActionMenuItem menuItem : ActionMenuItem.values()) {
            if (permissionDescriptions.contains(menuItem.getPermission())) {
                actions.add(menuItem);
            }
            
        }
       
        return actions;
    } 
	
	public List<Integer> getDays() {
		List<Integer> days = new ArrayList<Integer>();
		for(Integer i = 1; i < LocalDate.now().dayOfMonth().withMaximumValue().getDayOfMonth(); i++){
			days.add(i);
		}
		return days;
	}
}
