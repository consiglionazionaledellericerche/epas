/**
 *
 */
package models.enumerate;

/**
 * @author cristian
 */
public enum WorkingTimeTypeValues implements Identified {

  NORMALE_MOD(1l);

  private long id;

  private WorkingTimeTypeValues(Long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }
}
