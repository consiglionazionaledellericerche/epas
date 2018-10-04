package it.cnr.iit.epas;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DateInterval {

  private LocalDate begin;
  private LocalDate end;

  /**
   * Un DateInterval con begin obbligatorio.
   * @param begin
   * @param end
   * @return
   */
  public static DateInterval withBegin(LocalDate begin, Optional<LocalDate> end) {
    DateInterval dateInterval = new DateInterval();
    Preconditions.checkArgument(begin != null);
    dateInterval.begin = begin;
    if (end.isPresent()) {
      Preconditions.checkArgument(!begin.isAfter(end.get()));
      dateInterval.end = end.get();
    } else {
      dateInterval.end = DateUtility.setInfinity();
    }
    return dateInterval;
  }
  
  /**
   * Costruisce il dateInterval. <br>
   * - Se begin è null viene impostata MIN_DATE. <br>
   * - Se end è null viene impostata MAX_DATE. <br>
   * Se begin è successiva a end vengono invertite.
   * @param begin
   * @param end
   * @return
   */
  public static DateInterval build(LocalDate begin, LocalDate end) {

    if (begin == null && end == null) {
      begin = new LocalDate(0, 1, 1);
      end = DateUtility.setInfinity();
    } else if (begin == null) {
      begin = new LocalDate(0, 1, 1);
    } else if (end == null) {
      end = DateUtility.setInfinity();
    }

    //Non applico il riferimento ma costruisco nuovi oggetti
    LocalDate beginCopy = new LocalDate(begin);
    LocalDate endCopy = new LocalDate(end);

    DateInterval dateInterval = new DateInterval();
    
    if (begin.isAfter(end)) {
      dateInterval.begin = endCopy;
      dateInterval.end = beginCopy;
    } else {
      dateInterval.begin = beginCopy;
      dateInterval.end = endCopy;
    }
    
    return dateInterval;
  }

  
  /**
   * Questo costruttore è confondente. <br>
   * Sia date1 che date2 quando nulle vengono sostituite con MAX_DATE. <br>
   * se date1 è null e date2 è valorizzato, crea un intorno [date2, MAX_DATE]
   * il chè non è molto intuitivo.
   */
  public DateInterval(LocalDate date1, LocalDate date2) {

    if (date1 == null && date2 == null) {
      date1 = new LocalDate(0, 1, 1);
      date2 = DateUtility.setInfinity();
    } else if (date1 == null) {
      date1 = DateUtility.setInfinity();
    } else if (date2 == null) {
      date2 = DateUtility.setInfinity();
    }

    //Non applico il riferimento ma costruisco nuovi oggetti
    LocalDate date1Copy = new LocalDate(date1);
    LocalDate date2Copy = new LocalDate(date2);

    if (date1.isAfter(date2)) {
      this.begin = date2Copy;
      this.end = date1Copy;
    } else {
      this.begin = date1Copy;
      this.end = date2Copy;
    }
  }

  public LocalDate getBegin() {
    return begin;
  }

  public LocalDate getEnd() {
    return end;
  }

  /**
   * Se non è infinito.
   */
  public boolean isClosed() {
    if (DateUtility.isInfinity(this.end)) {
      return false;
    }
    return true;
  }
  
  public int dayInInterval() {
    return DateUtility.daysInInterval(this);
  }

  @Override
  public String toString() {
    return "[" + this.begin.toString() + "," + this.getEnd().toString() + "]";
  }


}
