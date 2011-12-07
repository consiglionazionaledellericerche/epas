package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */
@Data
public class Contract {
	
	public Qualification qualification;
	
	public ContractLevel contractLevel;
	
	public Person person;
	
	public Date beginContract;
	
	public Date endContract;
	
	public Date previousContract;
}
