package models.base;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.google.common.base.Objects;

/**
 * @author marco
 *
 */
@Entity
@RevisionEntity
@Table(name="revinfo")
public class Revision {
	
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name="rev")
    public int id;

    @RevisionTimestamp
    @Column(name="revtstmp")
    public long timestamp;

    @Transient
    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    @Override
    public boolean equals(Object o) {

    	if (o instanceof Revision) {
    		final Revision other = (Revision) o;
    		return id == other.id; 
    	} else {
    		return false;
    	}
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("id", id)
				.add("date", getRevisionDate())
				.omitNullValues()
				.toString();
	}
}
