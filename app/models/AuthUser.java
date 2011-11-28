package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
	
	public enum AuthRed {
		SI,
		NO
	}
	public enum AuthMod {
		SI,
		NO
	}
	public enum AuthSys {
		SI,
		NO
	}
	
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
	
	@Enumerated(EnumType.STRING)
	public AuthRed authred;
	
	@Enumerated(EnumType.STRING)
	public AuthMod authmod;
	
	@Enumerated(EnumType.STRING)
	public AuthSys autsys;
	
	@Column
	public String authIp;
	@Column
	public String passwordMD5;
}
