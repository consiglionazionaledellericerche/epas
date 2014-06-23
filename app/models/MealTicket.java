package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import play.data.validation.Required;

@Audited
@Entity
@Table(name = "meal_ticket")
public class MealTicket extends BaseModel{
	
	public static class BlockMealTicket {
		
		public Integer codeBlock;
		public List<MealTicket> mealTickets;
		
		public BlockMealTicket(Integer codeBlock) {
			
			this.codeBlock = codeBlock;
			this.mealTickets = Lists.newArrayList();
		}
	
	}
	
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	public Integer year;
	
	public Integer quarter;
	
	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@Required
	public Integer block;	/*esempio 5941 3165 01 */
	
	public Integer number;
	
	public String code; /* concatenzazione block + number */
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	public Person admin;
	

}
