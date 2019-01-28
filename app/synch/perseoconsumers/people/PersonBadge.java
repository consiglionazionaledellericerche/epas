package synch.perseoconsumers.people;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * PersonBadge.
 *
 * @author daniele
 * @since 28/01/19
 */
@Getter
@Setter
@Builder
public class PersonBadge {

  private Long personId;
  private String badge;
}
