package models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import models.base.BaseModel;
import net.sf.oval.constraint.NotNull;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Oggetto che modella i lettori badge.
 * @author cristian.
 */
@Entity
@Table(name = "badge_readers")
@Audited
public class BadgeReader extends BaseModel {

  private static final long serialVersionUID = -3508739971079270193L;

  @Unique
  @NotNull
  @Required
  public String code;

  public String description;

  public String location;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  public User user;


  @OrderBy("code ASC")
  @OneToMany(mappedBy = "badgeReader")
  public Set<Badge> badges = Sets.newHashSet();

  @ManyToMany
  public List<BadgeSystem> badgeSystems = Lists.newArrayList();
  
  @OneToMany(mappedBy = "badgeReader")
  @NotAudited
  public List<Zone> zones = Lists.newArrayList();


  public boolean enabled = true;

  @Override
  public String toString() {
    return this.code;
  }
}
