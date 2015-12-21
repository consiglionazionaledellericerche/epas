package dao.history;

import org.hibernate.envers.RevisionType;
import org.joda.time.LocalDateTime;

import com.google.common.base.Function;

import models.base.BaseModel;
import models.base.Revision;

/**
 * @author marco
 */
public class HistoryValue<T extends BaseModel> {

  public final T value;
  public final Revision revision;
  public final RevisionType type;


  HistoryValue(T value, Revision revision, RevisionType type) {
    this.value = value;
    this.revision = revision;
    this.type = type;
  }

  public static <T extends BaseModel> Function<Object[], HistoryValue<T>>
  fromTuple(final Class<T> cls) {

    return new Function<Object[], HistoryValue<T>>() {
      @Override
      public HistoryValue<T> apply(Object[] tuple) {
        return new HistoryValue<T>(cls.cast(tuple[0]), (Revision) tuple[1],
                (RevisionType) tuple[2]);
      }
    };
  }

  public String formattedRevisionDate() {

    LocalDateTime time = this.revision.getRevisionDate();

    if (time == null) {
      return "";
    }

    return time.toString("dd/MM/yyyy - HH:mm");
  }

  public String formattedOwner() {

    if (this.revision.owner != null) {
      return this.revision.owner.username;
    } else {
      return "ePAS";
    }
  }

  public boolean typeIsDel() {
    return type.name().equals("DEL");
  }

  public boolean typeIsAdd() {
    return type.name().equals("ADD");
  }

}
