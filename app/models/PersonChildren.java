package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;



/**
 * 
 * @author dario
 * questa classe è in relazione con la classe delle persone e serve a tenere traccia dei figli dei dipendenti per poter verificare se è possibile,
 * per il dipendente in questione, usufruire dei giorni di permesso per malattia dei figli che sono limitati nel tempo e per l'età del figlio
 */
@Entity
@Audited
@Table(name="person_children")
public class PersonChildren extends BaseModel{

	private static final long serialVersionUID = 2528486222814596830L;

	public String name;
	
	public String surname;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate bornDate;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
}
