package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 *
 */

public class AuthUser extends Model{
	@Column 
	public String user;
	@Column
	public String password;
	@Column
	public Date dataCpas;
	@Column
	public short scadenzaPassword;
	@Column
	public Timestamp ultimaModifica;
	@Column
	public enum authred {SI,NO};
	@Column
	public enum authmod {SI,NO};
	@Column
	public enum authsys {SI,NO};
	@Column
	public String authIp;
	@Column
	public String passwordMD5;
}
