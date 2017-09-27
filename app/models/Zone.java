package models;

import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import models.base.BaseModel;

import play.data.validation.Unique;


@Entity
@Table(name = "zones")
//@Audited
public class Zone extends BaseModel {

  private static final long serialVersionUID = 2466096445310199806L;

  @Unique
  @NotNull
  public String name;
  
  public String description;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "badge_reader_id")
  @Nullable
  public BadgeReader badgeReader;
  
  @OneToMany(mappedBy = "zone")
  public List<ZoneToZones> zoneLinkedAsMaster = Lists.newArrayList();
  
  @OneToMany(mappedBy = "zoneLinked")
  public List<ZoneToZones> zoneLinkedAsSlave = Lists.newArrayList();
}
