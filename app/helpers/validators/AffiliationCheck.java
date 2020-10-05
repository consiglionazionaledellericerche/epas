package helpers.validators;

import models.flows.Affiliation;
import play.data.validation.Check;

public class AffiliationCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Affiliation)) {
      return false;
    }
    final Affiliation affiliation = (Affiliation) validatedObject;
    if (affiliation.getPerson().affiliations.stream()
        .filter(a -> !a.id.equals(affiliation.getId()))
        .anyMatch(aff -> aff.overlap(affiliation))) {
      setMessage("validation.affiliation.membershipAlreadyPresent");
      return false;
    }
    return true;
  }

}
