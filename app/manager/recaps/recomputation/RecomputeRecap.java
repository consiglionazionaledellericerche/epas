package manager.recaps.recomputation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import models.Configuration;
import models.base.IPropertyInPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public class RecomputeRecap {

  public List<IPropertyInPeriod> periods = Lists.newArrayList();
  
  public LocalDate recomputeFrom;
  public Optional<LocalDate> recomputeTo;
  public boolean onlyRecaps;

  //Dato da utilizzare in caso di modifica contratto.
  public boolean initMissing;
  
  //Dato da utilizzare in caso di modifica configurazione.
  public Configuration configuration;
  
  
  public boolean needRecomputation;

  public int days;
}
