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
@Table(name = "build_up_edge_behaviours")
public class BuildUpEdgeBehaviour extends Model{

	private static final long serialVersionUID = -4570498420971254278L;
	
	public String label;
}
