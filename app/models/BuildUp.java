package models;

import javax.persistence.Entity;
import javax.persistence.Table;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name="build_up")
public class BuildUp extends Model {

	private static final long serialVersionUID = -8882579011090368055L;
 
	public String label;
}
