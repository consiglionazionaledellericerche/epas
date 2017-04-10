package models.absences;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;

import models.Qualification;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType.JustifiedTypeName;
import models.base.BaseModel;
import models.enumerate.QualificationMapping;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

@Entity
@Table(name = "absence_types")
@Audited
public class AbsenceType extends BaseModel {

  private static final long serialVersionUID = 7157167508454574329L;

  @ManyToMany
  public List<Qualification> qualifications = Lists.newArrayList();

  @Getter
  @Required
  public String code;

  @Column(name = "certification_code")
  public String certificateCode;

  public String description;

  @Column(name = "valid_from")
  public LocalDate validFrom;

  @Column(name = "valid_to")
  public LocalDate validTo;

  @Column(name = "internal_use")
  public boolean internalUse = false;
  
  @Getter
  @Column(name = "considered_week_end")
  public boolean consideredWeekEnd = false;
  
  @Getter
  @Column(name = "time_for_mealticket")
  public boolean timeForMealTicket = false;
  
  @Getter
  @Column(name = "justified_time")
  public Integer justifiedTime;
  
  @Getter
  @ManyToMany
  @JoinTable(name = "absence_types_justified_types", 
      joinColumns = { @JoinColumn(name = "absence_types_id") }, 
      inverseJoinColumns = { @JoinColumn(name = "justified_types_id") })
  public Set<JustifiedType> justifiedTypesPermitted = Sets.newHashSet();
  
  @Getter
  @Column(name = "replacing_time")
  public Integer replacingTime;
  
  @Getter
  @ManyToOne
  @JoinColumn(name = "replacing_type_id")
  public JustifiedType replacingType;
  
  @OneToMany(mappedBy = "absenceType")
  @LazyCollection(LazyCollectionOption.EXTRA)
  public Set<Absence> absences = Sets.newHashSet();

  @ManyToMany(mappedBy = "takenCodes")
  public Set<TakableAbsenceBehaviour> takenGroup = Sets.newHashSet();

  @ManyToMany(mappedBy = "takableCodes")
  public Set<TakableAbsenceBehaviour> takableGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "complationCodes")
  public Set<ComplationAbsenceBehaviour> complationGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "replacingCodes")
  public Set<ComplationAbsenceBehaviour> replacingGroup = Sets.newHashSet();
  
  // Metodi
  
  /**
   * Descrizione limitata a 60 caratteri.
   * @return short description
   */
  @Transient
  public String getShortDescription() {
    if (description != null && description.length() > 60) {
      return description.substring(0, 60) + "...";
    }
    return description;
  }
  
  /**
   * Se il codice è scaduto.
   * @return esito
   */
  @Transient
  public boolean isExpired() {
    boolean newResult = false;
    LocalDate begin = this.validFrom;
    LocalDate end = this.validTo;
    if (begin == null) {
      begin = new LocalDate(2000, 1, 1); //molto prima di epas...
    }
    if (end == null) {
      end = new LocalDate(2100, 1, 1);   //molto dopo di epas...
    }
    if (DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(begin, end))) {
      newResult = false;
    } else {
      newResult = true;
    }
    boolean oldResult = false;
    if (validTo == null) {
      oldResult = false;
    } else {
      oldResult = LocalDate.now().isAfter(validTo);
    }
    
    if (oldResult != newResult) {
      throw new IllegalStateException();
    }
    
    return newResult;
    
  }

  @Override
  public String toString() {
    return Joiner.on(" - ").skipNulls().join(code, description);
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello all day.
   * @return esito
   */
  @Transient
  public boolean isAllDayPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.all_day)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello absence type minutes.
   * @return esito
   */  
  @Transient
  public boolean isAbsenceTypeMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.absence_type_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello specified minutes.
   * @return esito
   */
  @Transient
  public boolean isSpecifiedMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.specified_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello nothing.
   * @return esito
   */
  @Transient
  public boolean isNothingPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.nothing)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se il codice di assenza è utilizzabile per tutte le qualifiche del mapping.
   * @param mapping mapping
   * @return esito
   */
  @Transient
  public boolean isQualificationMapping(QualificationMapping mapping) {
    Set<Integer> set = ContiguousSet.create(mapping.getRange(), 
        DiscreteDomain.integers());
    Set<Integer> actuals = Sets.newHashSet();
    for (Qualification qualification : qualifications) {
      actuals.add(qualification.qualification);
    }
    for (Integer item : set) {
      if (!actuals.contains(item)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * I gruppi coinvolti dal tipo assenza.
   * 
   * @param onlyProgrammed non filtrare i soli programmati
   * @return entity set
   */
  public Set<GroupAbsenceType> involvedGroupAbsenceType(boolean onlyProgrammed) {

    //TODO: da fare la fetch perchè è usato in tabellone timbrature per ogni codice assenza.
    
    Set<GroupAbsenceType> groups = Sets.newHashSet();
    for (TakableAbsenceBehaviour behaviour : this.takableGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour behaviour : this.takenGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.complationGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.replacingGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    if (!onlyProgrammed) {
      return groups;
    }
    Set<GroupAbsenceType> filteredGroup = Sets.newHashSet();
    for (GroupAbsenceType groupAbsenceType : groups) {
      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        filteredGroup.add(groupAbsenceType);
      }
    }
    return filteredGroup;
  }
  
  /**
   * Se il codice è coinvolto solo in gruppi semplici.
   * @return esito
   */
  public boolean onlySimpleGroupInvolved() {
    for (GroupAbsenceType group : involvedGroupAbsenceType(false)) {
      if (group.pattern == GroupAbsenceTypePattern.simpleGrouping) {
        continue;
      }
      return false;
    }
    return true;
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   * @return absent se il completamento non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultAbsenceType defaultType : DefaultAbsenceType.values()) {
      if (defaultType.name().substring(2).equals(this.code)) {
        if (defaultType.certificationCode.equals(this.certificateCode)
            && defaultType.description.equals(this.description)
            && defaultType.internalUse == this.internalUse
            && defaultType.justifiedTime.equals(this.justifiedTime)
            && defaultType.consideredWeekEnd == this.consideredWeekEnd
            && defaultType.timeForMealTicket == this.timeForMealTicket
            && defaultType.replacingTime.equals(this.replacingTime)
            ) {
          //Tipi permessi
          if (defaultType.justifiedTypeNamesPermitted.size() 
              != this.justifiedTypesPermitted.size()) {
            return Optional.of(false); 
          }
          for (JustifiedType justifiedType : this.justifiedTypesPermitted) {
            if (!defaultType.justifiedTypeNamesPermitted.contains(justifiedType.name)) {
              return Optional.of(false);
            }
          }
          
          //replecing type nullable
          if (defaultType.replacingType == null) {
            if (this.replacingType != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.replacingType.equals(this.replacingType.name)) {
              return Optional.of(false);
            }
          }
          //valid from nullable
          if (defaultType.validFrom == null) {
            if (this.validFrom != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.validFrom.equals(this.validFrom)) {
              return Optional.of(false);
            }
          }
          //valid to nullable
          if (defaultType.validTo == null) {
            if (this.validTo != null) {
              return Optional.of(false);
            }
          } else {
            if (!defaultType.validTo.equals(this.validTo)) {
              return Optional.of(false);
            }
          }
          
          return Optional.of(true);
        } else {
          return Optional.of(false);
        }
      } 
    }
    return Optional.absent();
  }
  
  /**
  
    
    public Set<JustifiedTypeName> justifiedTypeNamesPermitted; //ultimo
    
    public JustifiedTypeName replacingType;
    
    public LocalDate validFrom;
    public LocalDate validTo;
   */
  
  /**
   * Tipi assenza di default.
   * 
   * @author alessandro
   *
   */
  public static enum DefaultAbsenceType {

    A_18M("18M", "Permesso assistenza parenti/affini disabili L. 104/92 in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_18("18", "Permesso assistenza parenti/affini disabili L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_18H1("18H1", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H2("18H2", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H3("18H3", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H4("18H4", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H5("18H5", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H6("18H6", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H7("18H7", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H8("18H8", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_18H9("18H9", "Permesso assistenza parenti/affini disabili L. 104/92 completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),
    A_18P("18P", "Permesso provvisorio assistenza parenti/affini disabili L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    
    A_182M("182M", "Permesso assistenza secondo parenti/affini disabili L. 104/92 in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_182("182", "Permesso assistenza secondo parenti/affini disabili L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_182H1("182H1", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H2("182H2", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H3("182H3", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H4("182H4", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H5("182H5", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H6("182H6", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H7("182H7", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H8("182H8", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_182H9("182H9", "Permesso assistenza secondo parenti/affini disabili L.104/92 compl. 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),
    A_182P("182P", "Permesso provvisorio assistenza secondo parenti/affini disabili L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),

    A_19M("19M", "Permesso per dipendente disabile L. 104/92 in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_19("19", "Permesso per dipendente disabile L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_19H1("19H1", "Permesso per dipendente disabile L. 104/92 completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H2("19H2", "Permesso per dipendente disabile L. 104/92 completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H3("19H3", "Permesso per dipendente disabile L. 104/92 completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H4("19H4", "Permesso per dipendente disabile L. 104/92 completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H5("19H5", "Permesso per dipendente disabile L. 104/92 completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H6("19H6", "Permesso per dipendente disabile L. 104/92 completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H7("19H7", "Permesso per dipendente disabile L. 104/92 completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H8("19H8", "Permesso per dipendente disabile L. 104/92 completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_19H9("19H9", "Permesso per dipendente disabile L. 104/92 completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),
    A_19P("19P", "Permesso provvisorio per dipendente disabile L. 104/92 intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    
    //Il tempo per buono pasto in questo momento è disabilitato. Capire.
    A_26("26", "Permesso per dipendente disabile L. 104/92 due ore giornaliere",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    
    A_C17("C17", "Congedo assistenza figlio disabile L. 104/92",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_C18("C18", "Congedo straordinario per assistenza L. 104/92",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    
    A_661M("661M", "Permesso orario per motivi personali in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_661H1("661H1", "Permesso orario per motivi personali completamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        60, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H2("661H2", "Permesso orario per motivi personali completamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        120, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H3("661H3", "Permesso orario per motivi personali completamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        180, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H4("661H4", "Permesso orario per motivi personali completamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        240, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H5("661H5", "Permesso orario per motivi personali completamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        300, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H6("661H6", "Permesso orario per motivi personali completamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        360, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H7("661H7", "Permesso orario per motivi personali completamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        420, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H8("661H8", "Permesso orario per motivi personali completamento 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        480, JustifiedTypeName.absence_type_minutes, null, null),
    A_661H9("661H9", "Permesso orario per motivi personali completamento 9 ore",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        540, JustifiedTypeName.absence_type_minutes, null, null),

    A_89("89", "Permesso diritto allo studio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_89M("89M", "Permesso diritto allo studio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),

    A_09B("09B", "Permesso visita medica completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_09M("09M", "Permesso visita medica in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),

    A_92("92", "Missione",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_92H1("92H1", "Missione 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, true, true,
        0, null, null, null),
    A_92H2("92H2", "Missione 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, true, true,
        0, null, null, null),
    A_92H3("92H3", "Missione 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, true, true,
        0, null, null, null),
    A_92H4("92H4", "Missione 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, true, true,
        0, null, null, null),
    A_92H5("92H5", "Missione 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, true, true,
        0, null, null, null),
    A_92H6("92H6", "Missione 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, true, true,
        0, null, null, null),
    A_92H7("92H7", "Missione 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, true, true,
        0, null, null, null),


    A_31("31", "Ferie anno precedente",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_94("94", "festività soppresse (ex legge 937/77)",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_32("32", "Ferie anno corrente",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_37("37", "ferie anno precedente (dopo il 31/8)",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),

    A_91("91", "Riposo compensativo",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),


    A_PB("PB", "Permesso breve 36 ore anno",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes_limit), 0, false, false,
        0, null, null, null),

    A_105BP("105BP", "Lavoro Fuori Sede - Intera Giornata",
        false, ImmutableSet.of(JustifiedTypeName.assign_all_day), 0, false, false,
        0, null, null, null),
    
    A_23("23", "Astensione facoltativa post partum 100% primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_23M("23M", "Astensione facoltativa post partum 100% primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_23H7("23H7", "Astensione facoltativa post partum 100% primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_23U("23U", "Astensione facoltativa post partum 100% primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_25("25", "Astensione facoltativa post partum 30% primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_25M("25M", "Astensione facoltativa post partum 30% primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_25H7("25H7", "Astensione facoltativa post partum 30% primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_25U("25U", "Astensione facoltativa post partum 30% primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_24("24", "Astensione facoltativa post partum non retrib. primo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_24M("24M", "Astensione facoltativa post partum non retrib. primo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_24H7("24H7", "Astensione facoltativa post partum non retrib. primo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_24U("24U", "Astensione facoltativa post partum non retrib. primo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),

    A_232("232", "Astensione facoltativa post partum 100% secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_232M("232M", "Astensione facoltativa post partum 100% secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_232H7("232H7", "Astensione facoltativa post partum 100% secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_232U("232U", "Astensione facoltativa post partum 100% secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),
    
    A_252M("252M", "Astensione facoltativa post partum 30% secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_252("252", "Astensione facoltativa post partum 30% secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_252H7("252H7", "Astensione facoltativa post partum 30% secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_252U("252U", "Astensione facoltativa post partum 30% secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_242("242", "Astensione facoltativa post partum non retrib. secondo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    A_242M("242M", "Astensione facoltativa post partum non retrib. secondo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_242H7("242H7", "Astensione facoltativa post partum non retrib. secondo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_242U("242U", "Astensione facoltativa post partum non retrib. secondo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),

    A_233("233", "Astensione facoltativa post partum 100% terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_233M("233M", "Astensione facoltativa post partum 100% terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_233H7("233H7", "Astensione facoltativa post partum 100% terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_233U("233U", "Astensione facoltativa post partum 100% terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_253("253", "Astensione facoltativa post partum 30% terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_253M("253M", "Astensione facoltativa post partum 30% terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_253H7("253H7", "Astensione facoltativa post partum 30% terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_253U("253U", "Astensione facoltativa post partum 30% terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, true, false,
        0, null, null, null),

    A_243("243", "Astensione facoltativa post partum non retrib. terzo figlio intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),

    A_243M("243M", "Astensione facoltativa post partum non retrib. terzo figlio in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_243H7("243H7", "Astensione facoltativa post partum non retrib. terzo figlio completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    A_243U("243U", "Astensione facoltativa post partum non retrib. terzo figlio intera giornata altro genitore",
        true, ImmutableSet.of(JustifiedTypeName.all_day_limit), 0, false, false,
        0, null, null, null),
        
    A_25P("25P", "Prolungamento astensione facoltativa post partum 30% intera giornata",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_25PM("25M", "Prolungamento astensione facoltativa post partum 30% in ore e minuti",
        true, ImmutableSet.of(JustifiedTypeName.specified_minutes), 0, false, false,
        0, null, null, null),
    A_25PH7("25H7", "Prolungamento astensione facoltativa post partum 30% completamento giornata",
        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
        0, JustifiedTypeName.all_day, null, null),
    
    A_20("20", "Congedo/permesso DPR 1026 Art. 20",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_21("21", "Congedo/permesso per maternità",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_111("111", "Malattia",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11S("11S", "Malattia superiore a 15 gg lav.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_115("115", "Malattia superiore a 12 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_116("116", "Malattia superiore a 18 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_117("117", "Terapia invalidante grave patologia",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_118("118", "Malattia con responsabilita' di terzi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_119("119", "Malattia superiore a nove mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11R("11R", "Malattia con ricovero",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11R5("11R5", "Ricovero dopo malattia superiore a 12 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11R9("11R9", "Ricovero dopo malattia superiore a 9 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11C("11C", "Malattia post-ricovero",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11C5("11C5", "Convalescenza dopo malattia superiore a 12 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    A_11C9("11C9", "Convalescenza dopo malattia superiore a 9 mesi",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    //TODO: Questo codice in attestati è nel gruppo malattia. Verificare
    A_631("631", "Permesso visita medica",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
        0, null, null, null),
    
    A_12("12", "malattia primo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_13("13", "malattia primo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_14("14", "malattia primo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_122("122", "malattia secondo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_132("132", "malattia secondo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_142("142", "malattia secondo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, new LocalDate(1990, 1, 1), new LocalDate(2008, 12, 31)),
    
    A_123("123", "malattia terzo figlio/a <= 3 anni retribuita 100%",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_133("133", "malattia terzo figlio/a > 3 anni senza retr.",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),

    A_143("143", "malattia terzo figlio non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, new LocalDate(1990, 1, 1), new LocalDate(2008, 12, 31)),
    
    //Altri codici

    A_45("45", "Congedo straordinario permesso per matrimonio",
        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
        0, null, null, null),
    
    A_103("103", "Telelavoro",
        false, ImmutableSet.of(JustifiedTypeName.assign_all_day), 0, true, false,
        0, null, null, null),
    A_103BP("103BP", "Telelavoro buono pasto",
        false, ImmutableSet.of(JustifiedTypeName.assign_all_day), 0, true, true,
        0, null, null, null),
    
    A_71("71", "permesso sindacale 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_72("72", "permesso sindacale 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_73("73", "permesso sindacale 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_74("74", "permesso sindacale 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_75("75", "permesso sindacale 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, true,
        0, null, null, null),
    A_76("76", "permesso sindacale 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_77("77", "permesso sindacale 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    A_78("78", "permesso sindacale 8 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
        0, null, null, null),
    
    A_71A("71A", "permesso sindacale 1 ora non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_72A("72A", "permesso sindacale 2 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_73A("73A", "permesso sindacale 3 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_74A("74A", "permesso sindacale 4 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_75A("75A", "permesso sindacale 5 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_76A("76A", "permesso sindacale 6 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_77A("77A", "permesso sindacale 7 ore non retribuito",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    A_78A("78A", "permesso sindacale 8 ore non retribuita",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
        0, null, null, null),
    
    A_71S("71S", "perm.1 ora rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_72S("72S", "perm.2 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_73S("73S", "perm.3 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_74S("74S", "perm.4 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_75S("75S", "perm.5 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_76S("76S", "perm.6 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_77S("77S", "perm.7 ore rapp.lavoratori",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    
    A_71R("71R", "perm. sind. 1 ora R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_72R("72R", "perm. sind. 2 ore R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_73R("73R", "perm. sind. 3 ore R.S.U",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_74R("74R", "perm. sind. 4 ore R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_75R("75R", "perm. sind. 5 ore R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_76R("76R", "perm. sind. 6 ore R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_77R("77R", "perm. sind. 7 ore R.S.U.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    
    A_71D("71D", "perm. sind. 1 ora dirigenti sidac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_72D("72D", "perm. sind. 2 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_73D("73D", "perm. sind. 3 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_74D("74D", "perm. sind. 4 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_75D("75D", "perm. sind. 5 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_76D("76D", "perm. sind. 6 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_77D("77D", "perm. sind. 7 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    A_78D("78D", "perm. sind. 8 ore dirigenti sindac.",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
        0, null, null, null),
    
    A_01("01", "assemblea 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_02("02", "assemblea 2 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_03("03", "assemblea 3 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_04("04", "assemblea 4 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_05("05", "assemblea 5 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_06("06", "assemblea 6 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_07("07", "assemblea 7 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null),
    A_08("08", "assemblea 8 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
        0, null, null, null),
    
    A_FA1("FA1", "formazione e aggiornamento 1 ora",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
        0, null, null, null),
    A_FA2("FA2", "formazione e aggiornamento 2 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
        0, null, null, null),
    A_FA3("FA3", "formazione e aggiornamento 3 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
        0, null, null, null),
    A_FA4("FA4", "formazione e aggiornamento 4 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
        0, null, null, null),
    A_FA5("FA5", "formazione e aggiornamento 5 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
        0, null, null, null),
    A_FA6("FA6", "formazione e aggiornamento 6 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
        0, null, null, null),
    A_FA7("FA7", "formazione e aggiornamento 7 ore",
        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
        0, null, null, null);

    




//    A_71S("71S", "perm.1 ora rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_72S("72S", "perm.2 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_73S("73S", "perm.3 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_74S("74S", "perm.4 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_75S("75S", "perm.5 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_76S("76S", "perm.6 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_77S("77S", "perm.7 ore rapp.lavoratori",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_ES_L133("ES-L133", "esonero servizio art.72 L.133/08",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_18P("18P", "perm. provv. assist. parenti aff. handic",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_243S("243S", "ast. fac. post partum < 3 a.n.r. 3o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_35R("35R", "dottorato di ricerca retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_78R("78R", "perm. sind. 8 ore RSU",
//        true, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_91MD("91", "Riposo compensativo per missione di domenica",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_91MS("91", "Riposo compensativo per missione di sabato",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_96A("96A", "sospensione dal lavoro",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_96B("96B", "sospensione dal servizio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_980("980", "ass. non giust. per vis. med. fisc.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_985("985", "ass. non giust. per vis. med. fisc.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_OA5("OA5", "ore aggiuntive 5",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_OA6("OA6", "ore aggiuntive 6",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_OA7("OA7", "ore aggiuntive 7",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_134("134", "malattia quarto figlio/a > 3 anni senza retr.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_OA4("OA4", "ore aggiuntive 4",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_OA3("OA3", "ore aggiuntive 3",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_OA2("OA2", "ore aggiuntive 2",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_C17("C17", "congedo assistenza figlio/a handicap",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_C18("C18", "congedo assist. fratello/sor. handicap.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_FA1("FA1", "formazione e aggiornamento 1 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_FA2("FA2", "formazione e aggiornamento 2 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_FA3("FA3", "formazione e aggiornamento 3 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_FA4("FA4", "formazione e aggiornamento 4 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_FA5("FA5", "formazione e aggiornamento 5 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_FA6("FA6", "formazione e aggiornamento 6 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_FA7("FA7", "formazione e aggiornamento 7 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_OA1("OA1", "ore aggiuntive 1",
//        false, ImmutableSet.of(JustifiedTypeName.nothing), 0, false, false,
//        0, null, null, null),
//    A_42("42", "permesso cure invalidita' per servizio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_61("61", "causa forza maggiore recuperato",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_62("62", "distacco sindacale",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_62A("62A", "aspettativa sindacale non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_65("65", "causa forza maggiore da recuperare",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_67("67", "permesso donazione sangue",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_681("681", "permesso lutto di famiglia",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_691("691", "permesso II lutto di famiglia",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_60("60", "aspettativa richiamo armi > 2. mese",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_6N("6N", "permesso motivi privati",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_43("43", "ferie radiazioni ionizzanti",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_441("441", "permesso esami",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_45("45", "cong. str./permesso matrimonio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_46("46", "cong. str./permesso straordinario richiamo armi",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_50("50", "aspettativa per riunione al coniuge all'estero",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_52("52", "aspettativa per infermita' causa servizio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_53("53", "aspettativa per servizio militare",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_54("54", "aspettativa per motivi di famiglia studio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_54P("54P", "aspettativa per periodo di prova",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_58("58", "aspettativa per funzione pubblica non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_70("70", "malattia non retribuita conservazione posto (dipendenti ex art. 6 legge 70/75)",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_71("71", "permesso sindacale 1 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_71A("71A", "permesso sindacale 1 ora non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_78A("78A", "permesso sindacale 8 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_79("79", "concorso interno",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_80("80", "sciopero intera giornata",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_81("81", "sciopero 1 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_82("82", "sciopero 2 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_83("83", "sciopero 3 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_84("84", "sciopero 4 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_85("85", "sciopero 5 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_86("86", "sciopero 6 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_78("78", "permesso sindacale 8 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_77A("77A", "permesso sindacale 7 ore non retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_77("77", "permesso sindacale 7 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_72("72", "permesso sindacale 2 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_72A("72A", "permesso sindacale 2 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_73("73", "permesso sindacale 3 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_73A("73A", "permesso sindacale 3 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_74("74", "permesso sindacale 4 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_74A("74A", "permesso sindacale 4 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_75("75", "permesso sindacale 5 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, true,
//        0, null, null, null),
//    A_75A("75A", "permesso sindacale 5 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_76("76", "permesso sindacale 6 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_76A("76A", "permesso sindacale 6 ore non retribuita",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_9A("9A", "audizione procedimento disciplinare",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_41("41", "riposo compensativo per missione Antartide",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_01("01", "assemblea 1 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_102("102", "collocamento fuori ruolo all'estero",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_02("02", "assemblea 2 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_03("03", "assemblea 3 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_04("04", "assemblea 4 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_05("05", "assemblea 5 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_06("06", "assemblea 6 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_07("07", "assemblea 7 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_08("08", "assemblea 8 ora",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_10("10", "permessi 1 ora (handcap)",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_101("101", "collocamento fuori ruolo in italia",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_16("16", "congedo straordinario per volontariato",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_16S("16S", "congedo straordinario per volontariato: esercitazioni",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_32U("32U", "ferie usufruite in congedo motivi studio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_33("33", "congedo per motivi di studio retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_33B("33B", "congedo studio retribuito/borsa CNR",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_33C("33C", "congedo studio retribuito/cooperazione",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_34("34", "congedo studio non retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//
//    A_35("35", "dottorato di ricerca non retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_36("36", "post dottorato non retribuito",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_38("38", "infortunio in itinere o causa servizio da riconoscere",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_39("39", "aspettativa in itinere riconosciuto",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_20("20", "congedo/permesso DPR 1026 Art. 20",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_21("21", "congedo/permesso per maternità",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_22("22", "permesso 2h per figlio hand. <= 3 anni",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_27("27", "permesso per il figlio hand. <= 3 anni",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_29("29", "permesso per figlio hand. <=3 anni retribuita 30%",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_40("40", "ferie per missione Antartide",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_54F("54F", "aspett. motivi fam. progetto recupero",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_55("55", "aspett. per funzioni pubbliche",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_62D("62D", "perm. sind. cumul. sotto forma distac.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_62S50O("62S50O", "dist. sind.a temp. det. p. t. 50% oriz.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_62S50V("62S50V", "dist. sind.a temp. det. p. t. 50% vert.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_631("631", "permesso visita medica",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_662("662", "permesso grave inferm.coniuge o parente",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_662C("662C", "comp. gior. perm. grave infor  coniug. parent",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_71D("71D", "perm. sind. 1 ora dirigenti sidac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_54E("54E", "congedo eventi cause particolari",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_54D("54D", "Aspett. per nomina a Dirett. Ammin.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, true, false,
//        0, null, null, null),
//    A_54C("54C", "Apett. per cooperaz. Paesi in svilupp",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_31P("31P", "cong. ord./ferie anno prec. pagate",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_31R("31R", "ferie usuf. nel period. preavv. s. sti.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_32P("32P", "cong. ord./ferie anno corrente pagate",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_32R("32R", "ferie usufr. nel periodo preavv. s. sti.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_401("401", "cong. ord./ferie per miss. Antartide",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_402("402", "cong. ord./ferie per miss antartide",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_412("412", "riposo comp per miss. antartide",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_48("48", "entr. usc. da recup. per ass. giust.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_49("49", "entr usc. a/r per mal.vis. med/cp",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_53C("53C", "aspett. serv. civ. (obiett. coscienza)",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_71R("71R", "perm. sind. 1 ora R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_72D("72D", "perm. sind. 2 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_72R("72R", "perm. sind. 2 ore R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_78D("78D", "perm. sind. 8 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_73D("73D", "perm. sind. 3 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_73R("73R", "perm. sind. 3 ore R.S.U",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null),
//    A_74D("74D", "perm. sind. 4 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_74R("74R", "perm. sind. 4 ore R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_75D("75D", "perm. sind. 5 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_75R("75R", "perm. sind. 5 ore R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 300, false, false,
//        0, null, null, null),
//    A_76D("76D", "perm. sind. 6 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_76R("76R", "perm. sind. 6 ore R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 360, false, false,
//        0, null, null, null),
//    A_77D("77D", "perm. sind. 7 ore dirigenti sindac.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_77R("77R", "perm. sind. 7 ore R.S.U.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_90("90", "causa forza maggiore da non recuperare",
//        true, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_96("96", "sospensione cautelare",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_97("97", "sospensione dalla qualifica",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_98("98", "assenza non giustificata",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_99("99", "permesso per diritto allo studio",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_88("88", "Sciopero 8 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 480, false, false,
//        0, null, null, null),
//    A_87("87", "Sciopero 7 ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_44("44", "Congedo straordinario concorso ed esami",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_24("24", "ast. fac. post partum > 3 a.n.r. 1o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_93("93", "incarico di insegnamento",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_834("834", "Non conosciuto",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_242("242", "ast. fac. post  partum > 3 a.n.r. 2o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_242S("242S", "ast. fac. post partum < 3 a.n.r.  2o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_24S("24S", "ast. fac. post partum < 3 a.n.r. 1o figl",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_243("243", "ast. fac. post partum > 3 a.n.r. 3o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_16A("16A", "aspettativa per cooperazione MAE",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_17C("17C", "comp.perm.ass.figlio >=3 a. <=18 a",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_20A("20A", "permesso adozione stato straniero",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_FER("", "Ferie (anno prec., anno in corso, perm. legge)",
//        false, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_661H("", "PERM.ORARIO GRAVI MOTIVI", 
//        true, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_103("103", "Telelavoro",
//        false, ImmutableSet.of(JustifiedTypeName.assign_all_day), 0, false, false,
//        0, null, null, null),
//    A_91CE("91", "RIP. COMP.CHIUSURA ENTE",
//        true, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_182("", "PERM ASSIST.PARENTI 2",
//        true, ImmutableSet.of(JustifiedTypeName.all_day), 0, false, false,
//        0, null, null, null),
//    A_26("26", "Permesso portatori di Handicap 2 Ore",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_24H7("24h7", "giustificazione oraria di 7 ore per ast. fac. post partum > 3 a.n.r. 1o figl.",
//        false, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 420, false, false,
//        0, null, null, null),
//    A_24H4("", "giustificazione oraria di 4 ore per ast. fac. post partum > 3 a.n.r. 1o figl.",
//        true, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 240, false, false,
//        0, null, null, null),
//    A_24H1("", "congedo 1 ora per ast. fac. post partum > 3 a.n.r. 1o figl.",
//        true, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 60, false, false,
//        0, null, null, null),
//    A_24H2("", "congedo 2 ore per ast. fac. post partum > 3 a.n.r. 1o figl.",
//        true, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 120, false, false,
//        0, null, null, null),
//    A_24H3("", "congedo 3 ore per ast. fac. post partum > 3 a.n.r. 1o figl.",
//        true, ImmutableSet.of(JustifiedTypeName.absence_type_minutes), 180, false, false,
//        0, null, null, null);



    public String certificationCode;
    public String description;
    public boolean internalUse;

    public Set<JustifiedTypeName> justifiedTypeNamesPermitted;
    public Integer justifiedTime;                              

    public boolean consideredWeekEnd;

    public boolean timeForMealTicket;

    public Integer replacingTime;
    public JustifiedTypeName replacingType;                     //nullable

    public LocalDate validFrom;                                 //nullable
    public LocalDate validTo;                                   //nullable

    private DefaultAbsenceType(String certificationCode, String description, boolean internalUse, 
        Set<JustifiedTypeName> justifiedTypeNamesPermitted, Integer justifiedTime, 
        boolean consideredWeekEnd, boolean timeForMealTicket, 
        Integer replacingTime, JustifiedTypeName replacingType,
        LocalDate validFrom, LocalDate validTo) {
      this.certificationCode = certificationCode;
      this.description = description;
      this.internalUse = internalUse;
      this.justifiedTypeNamesPermitted = justifiedTypeNamesPermitted;
      this.justifiedTime = justifiedTime;
      this.consideredWeekEnd = consideredWeekEnd;
      this.timeForMealTicket = timeForMealTicket;
      this.replacingTime = replacingTime;
      this.replacingType = replacingType;
      this.validFrom = validFrom;
      this.validTo = validTo;

    }

    /**
     * Ricerca i codici assenza modellati e non presenti fra quelle passate in arg (db).
     * @return list
     */
    public static List<DefaultAbsenceType> missing(List<AbsenceType> allAbsenceTypes) {
      List<DefaultAbsenceType> missing = Lists.newArrayList();
      for (DefaultAbsenceType defaultTypes : DefaultAbsenceType.values()) {
        boolean found = false;
        for (AbsenceType type : allAbsenceTypes) {
          if (defaultTypes.name().substring(2).equals(type.code)) {
            found = true;
            break;
          }
        }
        if (!found) {
          missing.add(defaultTypes);
        }
      }
      return missing;
    }

    /**
     * L'enumerato corrispettivo del absenceType (se esiste...) 
     * @return optional dell'enumerato
     */
    public static Optional<DefaultAbsenceType> byCode(AbsenceType absenceType) {
      for (DefaultAbsenceType defaultAbsenceType : DefaultAbsenceType.values()) {
        if (absenceType.code.equals(defaultAbsenceType.name().substring(2))) {
          return Optional.of(defaultAbsenceType);
        }
      }
      return Optional.absent();
    }

  }

}
