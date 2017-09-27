package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import play.data.validation.Required;



@Entity
@Table(name = "zone_to_zones")
//@Audited
public class ZoneToZones extends BaseModel {

  private static final long serialVersionUID = 1252197401101094698L;

  @Required
  @ManyToOne
  @JoinColumn(name = "zone_id", insertable = false, updatable = false)
  public Zone zone;
  
  @Required
  @ManyToOne
  @JoinColumn(name = "zone_id", insertable = false, updatable = false)
  public Zone zoneLinked;
  
  @Required
  public int delay;  

  @Override
  public String toString() {
    return String.format(
        "Zone[%d] - zone.name = %s, zoneLinked.name= %s, delay = %d",
         id, zone.name, zoneLinked.name, delay);
  }
}
