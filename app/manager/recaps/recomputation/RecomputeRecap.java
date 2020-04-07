package manager.recaps.recomputation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.ToString;
import manager.configurations.EpasParam;
import models.base.IPropertyInPeriod;
import org.joda.time.LocalDate;

@ToString
public class RecomputeRecap {

  public List<IPropertyInPeriod> periods = Lists.newArrayList();
  
  public LocalDate recomputeFrom;
  public Optional<LocalDate> recomputeTo;
  public boolean onlyRecaps;

  //Dato da utilizzare in caso di modifica contratto.
  public boolean initMissing;
  
  //Dato da utilizzare in caso di modifica configurazione.
  public EpasParam epasParam; 
  
  
  public boolean needRecomputation;

  public int days;
}
