package manager.recaps.recomputation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import models.base.IPropertyInPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public class RecomputeRecap {

  public List<IPropertyInPeriod> periods = Lists.newArrayList();
  
  public LocalDate recomputeFrom;
  public Optional<LocalDate> recomputeTo;
  public boolean onlyRecaps;

  public boolean initMissing;

  public boolean needRecomputation;

  public int days;
}
