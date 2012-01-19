package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "auth_users")
public class AuthUser extends Model{
	
	private static final long serialVersionUID = -1726409927134109278L;
	
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
	
	public String user;

	public String password;

	public Date dataCpas;

	public Short scadenzaPassword;

	public Timestamp ultimaModifica;
	
	@Enumerated(EnumType.STRING)
	public AuthRed authred;
	
	@Enumerated(EnumType.STRING)
	public AuthMod authmod;
	
	@Enumerated(EnumType.STRING)
	public AuthSys autsys;
	
	public String authIp;

	public String passwordMD5;
}
