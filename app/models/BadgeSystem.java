package models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import models.base.BaseModel;

import net.sf.oval.constraint.NotNull;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;


/**
 * @author alessandro.
 */
@Entity
@Table(name = "badge_systems")
@Audited
public class BadgeSystem extends BaseModel {

  @Unique
  @NotNull
  @Required
  public String name;

  public String description;

  @OrderBy("code ASC")
  @OneToMany(mappedBy = "badgeSystem")
  public Set<Badge> badges = Sets.newHashSet();

  @ManyToMany(mappedBy = "badgeSystems")
  public List<BadgeReader> badgeReaders = Lists.newArrayList();

  @Required
  @ManyToOne
  public Office office;

  public boolean enabled = true;

  @Override
  public String toString() {
    return this.name;
  }
}
