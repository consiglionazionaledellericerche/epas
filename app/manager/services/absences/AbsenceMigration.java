package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import models.enumerate.JustifiedTimeAtWork;

import java.util.List;

//@Slf4j
public class AbsenceMigration {
  
  private final AbsenceComponentDao absenceComponentDao;

  public enum DefaultCategoryType {
    
    GENERAL("Assenze generali cnr", 1),
    PERMISSION("Permessi vari", 2),
    POST_PARTUM("Congedi parentali", 3),
    LAW_104_92("Disabilità legge 104/92", 4), 
    OTHER("Altre tipologie", 5);
    
    public String name;
    public int priority;
    
    private DefaultCategoryType(String name, int priority) {
      this.name = name;
      this.priority = priority;
    }
    
  }
  
  public enum DefaultComplation {
    C_18, C_19, C_661, C_23, C_25, C_89, C_09;
  }
  
  public enum DefaultTakable {
    T_18, T_19, T_661, T_23, T_25, T_89, T_09, T_FERIE_CNR, T_RIPOSI_CNR, T_MISSIONE, T_ALTRI;
  }
  
  public enum DefaultGroup {
    G_18, G_19, G_661, G_23, G_25, G_89, G_09, MISSIONE, ALTRI, FERIE_CNR, RIPOSI_CNR;
  }
  
  @Inject
  public AbsenceMigration(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
  
  public void migrateAbsence(Absence absence) {
    
    final JustifiedType nothing = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing);
    final JustifiedType specifiedMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    final JustifiedType absenceTypeMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.absence_type_minutes);
    final JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    final JustifiedType halfDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.half_day);
    final JustifiedType assignAllDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.assign_all_day); 

    if (absence.absenceType.justifiedTimeAtWork == null) {
      return;
    }

    // Assenze orarie.
    if (absence.absenceType.justifiedTimeAtWork.minutes != null 
        && absence.absenceType.justifiedTimeAtWork.minutes > 0) {
      absence.justifiedType = absenceTypeMinutes;
      absence.save();
      return;
    }

    if (absence.absenceType.justifiedTime != null && absence.absenceType.justifiedTime > 0) {
      absence.justifiedType = specifiedMinutes; //PEPE //RITING
      absence.save();
      return;
    }

    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {  
      absence.justifiedType = allDay;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.Nothing)) { 
      absence.justifiedType = nothing;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.HalfDay)) {
      absence.justifiedType = halfDay;
      absence.save();
      return;
    }
    if (absence.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
      absence.justifiedType = assignAllDay;
      absence.save();
      return;
    }

  }
  
  public void buildDefaultGroups() { 

    final JustifiedType nothing = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing);
    final JustifiedType specifiedMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    final JustifiedType absenceTypeMinutes = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.absence_type_minutes);
    final JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    final JustifiedType halfDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.half_day);
    final JustifiedType assignAllDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.assign_all_day); 

    final CategoryGroupAbsenceType generalCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.GENERAL.name, 
            DefaultCategoryType.GENERAL.priority);
    
    final CategoryGroupAbsenceType permissionCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.PERMISSION.name, 
            DefaultCategoryType.PERMISSION.priority);
    
    final CategoryGroupAbsenceType postpartumCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.POST_PARTUM.name, 
            DefaultCategoryType.POST_PARTUM.priority);
    
    final CategoryGroupAbsenceType lawCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.LAW_104_92.name, 
            DefaultCategoryType.LAW_104_92.priority);
    
    final CategoryGroupAbsenceType otherCategory = absenceComponentDao
        .getOrBuildCategoryType(DefaultCategoryType.OTHER.name, 
            DefaultCategoryType.OTHER.priority);
    
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    for (AbsenceType absenceType : absenceTypes) {
      absenceType.code = absenceType.code.toUpperCase();
      absenceType.justifiedTime = 0;
      if (absenceType.justifiedTimeAtWork == null) {
        // TODO: andrebbero disabilitate.
        continue;
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.Nothing)) {
        absenceType.justifiedTypesPermitted.add(nothing);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay)) {
        absenceType.justifiedTypesPermitted.add(allDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.HalfDay)) {
        absenceType.justifiedTypesPermitted.add(halfDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AssignAllDay)) {
        absenceType.justifiedTypesPermitted.add(assignAllDay);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.OneHour)) {
        absenceType.justifiedTime = 60;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.TwoHours)) {
        absenceType.justifiedTime = 120;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.ThreeHours)) {
        absenceType.justifiedTime = 180;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHours)) {
        absenceType.justifiedTime = 240;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHours)) {
        absenceType.justifiedTime = 300;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHours)) {
        absenceType.justifiedTime = 360;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHours)) {
        absenceType.justifiedTime = 420;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.EightHours)) {
        absenceType.justifiedTime = 480;  
        absenceType.justifiedTypesPermitted.add(absenceTypeMinutes);
      }
      if (absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.OneHourMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.TwoHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.ThreeHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHoursMealTimeCounting)
          || absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.EightHoursMealTimeCounting))
      {
        absenceType.timeForMealTicket = true;
      }
      
      absenceType.save();

    }

    // OSS 60 nothing perchè non giustifica niente ma serve per il completamento orario... 
    // vedere se modellarlo.

    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_18.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr18 = absenceComponentDao.buildOrEditAbsenceType("18", 
          "Permesso assistenza parenti/affini disabili L. 104/92 intera giornata", 
          0, Sets.newHashSet(allDay), false, false, false, "18");

      AbsenceType h118 = absenceComponentDao.buildOrEditAbsenceType("18H1", 
          "Permesso assistenza parenti/affini disabili L. 104/92 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H1");
      AbsenceType h218 = absenceComponentDao.buildOrEditAbsenceType("18H2", 
          "Permesso assistenza parenti/affini disabili L. 104/92 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H2");
      AbsenceType h318 = absenceComponentDao.buildOrEditAbsenceType("18H3", 
          "Permesso assistenza parenti/affini disabili L. 104/92 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H3");
      AbsenceType h418 = absenceComponentDao.buildOrEditAbsenceType("18H4", 
          "Permesso assistenza parenti/affini disabili L. 104/92 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H4");
      AbsenceType h518 = absenceComponentDao.buildOrEditAbsenceType("18H5", 
          "Permesso assistenza parenti/affini disabili L. 104/92 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H5");
      AbsenceType h618 = absenceComponentDao.buildOrEditAbsenceType("18H6", 
          "Permesso assistenza parenti/affini disabili L. 104/92 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H6");
      AbsenceType h718 = absenceComponentDao.buildOrEditAbsenceType("18H7", 
          "Permesso assistenza parenti/affini disabili L. 104/92 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H7");
      AbsenceType h818 = absenceComponentDao.buildOrEditAbsenceType("18H8", 
          "Permesso assistenza parenti/affini disabili L. 104/92 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), false, false, false, "18H8");

      AbsenceType m18 = absenceComponentDao.buildOrEditAbsenceType("18M", 
          "Permesso assistenza parenti/affini disabili L. 104/92 in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);
      AbsenceType h1c18 = absenceComponentDao.buildOrEditAbsenceType("18H1C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 1 ora", 
          60, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h2c18 = absenceComponentDao.buildOrEditAbsenceType("18H2C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 2 ore", 
          120, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h3c18 = absenceComponentDao.buildOrEditAbsenceType("18H3C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 3 ore", 
          180, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h4c18 = absenceComponentDao.buildOrEditAbsenceType("18H4C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 4 ore", 
          240, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h5c18 = absenceComponentDao.buildOrEditAbsenceType("18H5C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 5 ore", 
          300, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h6c18 = absenceComponentDao.buildOrEditAbsenceType("18H6C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 6 ore", 
          360, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h7c18 = absenceComponentDao.buildOrEditAbsenceType("18H7C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 7 ore", 
          420, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h8c18 = absenceComponentDao.buildOrEditAbsenceType("18H8C", 
          "Permesso assistenza parenti/affini disabili L. 104/92 completamento 8 ore", 
          480, Sets.newHashSet(nothing), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c18 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_18.name());

      if (!c18.isPresent()) {

        c18 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c18.get().name = DefaultComplation.C_18.name();
        c18.get().amountType = AmountType.minutes;
        c18.get().complationCodes.add(m18);
        c18.get().replacingCodes = 
            Sets.newHashSet(h1c18, h2c18, h3c18, h4c18, h5c18, h6c18, h7c18, h8c18);
        c18.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t18 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_18.name());

      if (!t18.isPresent()) {

        t18 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t18.get().name = DefaultTakable.T_18.name();
        t18.get().amountType = AmountType.units;
        t18.get().takableCodes = Sets.newHashSet(cnr18, m18, h118, h218, h318, h418, h518, h618, h718, h818);
        t18.get().takenCodes = Sets.newHashSet(cnr18, m18, h118, h218, h318, h418, h518, h618, h718, h818);
        t18.get().fixedLimit = 3;
        t18.get().save();
      }

      // Group Creation
      GroupAbsenceType group18 = new GroupAbsenceType();
      group18.category = lawCategory;
      group18.name = DefaultGroup.G_18.name();
      group18.description = "Permesso assistenza parenti/affini disabili L. 104/92 tre giorni mese";
      group18.pattern = GroupAbsenceTypePattern.programmed;
      group18.periodType = PeriodType.month;
      group18.complationAbsenceBehaviour = c18.get();
      group18.takableAbsenceBehaviour = t18.get();
      group18.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_19.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr19 = absenceComponentDao.buildOrEditAbsenceType("19", 
          "Permesso per dipendente disabile L. 104/92 intera giornata", 
          0, Sets.newHashSet(allDay), false, false, false, "19");

      AbsenceType h119 = absenceComponentDao.buildOrEditAbsenceType("19H1", 
          "Permesso per dipendente disabile L. 104/92 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H1");
      AbsenceType h219 = absenceComponentDao.buildOrEditAbsenceType("19H2", 
          "Permesso per dipendente disabile L. 104/92 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H2");
      AbsenceType h319 = absenceComponentDao.buildOrEditAbsenceType("19H3", 
          "Permesso per dipendente disabile L. 104/92 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H3");
      AbsenceType h419 = absenceComponentDao.buildOrEditAbsenceType("19H4", 
          "Permesso per dipendente disabile L. 104/92 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H4");
      AbsenceType h519 = absenceComponentDao.buildOrEditAbsenceType("19H5", 
          "Permesso per dipendente disabile L. 104/92 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H5");
      AbsenceType h619 = absenceComponentDao.buildOrEditAbsenceType("19H6", 
          "Permesso per dipendente disabile L. 104/92 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H6");
      AbsenceType h719 = absenceComponentDao.buildOrEditAbsenceType("19H7", 
          "Permesso per dipendente disabile L. 104/92 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H7");
      AbsenceType h819 = absenceComponentDao.buildOrEditAbsenceType("19H8", 
          "Permesso per dipendente disabile L. 104/92 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), false, false, false, "19H8");

      AbsenceType m19 = absenceComponentDao.buildOrEditAbsenceType("19M", 
          "Permesso per dipendente disabile L. 104/92 in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);
      AbsenceType h1c19 = absenceComponentDao.buildOrEditAbsenceType("19H1C", 
          "Permesso per dipendente disabile L. 104/92 completamento 1 ora", 
          60, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h2c19 = absenceComponentDao.buildOrEditAbsenceType("19H2C", 
          "Permesso per dipendente disabile L. 104/92 completamento 2 ore", 
          120, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h3c19 = absenceComponentDao.buildOrEditAbsenceType("19H3C", 
          "Permesso per dipendente disabile L. 104/92 completamento 3 ore", 
          180, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h4c19 = absenceComponentDao.buildOrEditAbsenceType("19H4C", 
          "Permesso per dipendente disabile L. 104/92 completamento 4 ore", 
          240, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h5c19 = absenceComponentDao.buildOrEditAbsenceType("19H5C", 
          "Permesso per dipendente disabile L. 104/92 completamento 5 ore", 
          300, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h6c19 = absenceComponentDao.buildOrEditAbsenceType("19H6C", 
          "Permesso per dipendente disabile L. 104/92 completamento 6 ore", 
          360, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h7c19 = absenceComponentDao.buildOrEditAbsenceType("19H7C", 
          "Permesso per dipendente disabile L. 104/92 completamento 7 ore", 
          420, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h8c19 = absenceComponentDao.buildOrEditAbsenceType("19H8C", 
          "Permesso per dipendente disabile L. 104/92 completamento 8 ore", 
          480, Sets.newHashSet(nothing), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c19 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_19.name());

      if (!c19.isPresent()) {

        c19 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c19.get().name = DefaultComplation.C_19.name();
        c19.get().amountType = AmountType.minutes;
        c19.get().complationCodes.add(m19);
        c19.get().replacingCodes = 
            Sets.newHashSet(h1c19, h2c19, h3c19, h4c19, h5c19, h6c19, h7c19, h8c19);
        c19.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t19 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_19.name());

      if (!t19.isPresent()) {

        t19 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t19.get().name = DefaultTakable.T_19.name();
        t19.get().amountType = AmountType.units;
        t19.get().takableCodes = Sets.newHashSet(cnr19, m19, h119, h219, h319, h419, h519, h619, h719, h819);
        t19.get().takenCodes = Sets.newHashSet(cnr19, m19, h119, h219, h319, h419, h519, h619, h719, h819);
        t19.get().fixedLimit = 3;
        t19.get().save();
      }

      // Group Creation
      GroupAbsenceType group19 = new GroupAbsenceType();
      group19.category = lawCategory;
      group19.name = DefaultGroup.G_19.name();
      group19.description = "Permesso per dipendente disabile L. 104/92 tre giorni mese";
      group19.pattern = GroupAbsenceTypePattern.programmed;
      group19.periodType = PeriodType.month;
      group19.complationAbsenceBehaviour = c19.get();
      group19.takableAbsenceBehaviour = t19.get();
      group19.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_661.name()).isPresent()) {

      //Update AbsenceType
//      AbsenceType cnr661 = absenceComponentDao.buildOrEditAbsenceType("661", 
//          "Permesso orario per motivi personali intera giornata", 
//          0, Sets.newHashSet(allDay), false, false, false, "661");

      AbsenceType h1661 = absenceComponentDao.buildOrEditAbsenceType("661H1", 
          "Permesso orario per motivi personali 1 ora", 
          60, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H1");
      AbsenceType h2661 = absenceComponentDao.buildOrEditAbsenceType("661H2", 
          "Permesso orario per motivi personali 2 ore", 
          120, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H2");
      AbsenceType h3661 = absenceComponentDao.buildOrEditAbsenceType("661H3", 
          "Permesso orario per motivi personali 3 ore", 
          180, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H3");
      AbsenceType h4661 = absenceComponentDao.buildOrEditAbsenceType("661H4", 
          "Permesso orario per motivi personali 4 ore", 
          240, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H4");
      AbsenceType h5661 = absenceComponentDao.buildOrEditAbsenceType("661H5", 
          "Permesso orario per motivi personali 5 ore", 
          300, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H5");
      AbsenceType h6661 = absenceComponentDao.buildOrEditAbsenceType("661H6", 
          "Permesso orario per motivi personali 6 ore", 
          360, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H6");
      AbsenceType h7661 = absenceComponentDao.buildOrEditAbsenceType("661H7", 
          "Permesso orario per motivi personali 7 ore", 
          420, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H7");
      AbsenceType h8661 = absenceComponentDao.buildOrEditAbsenceType("661H8", 
          "Permesso orario per motivi personali 8 ore", 
          480, Sets.newHashSet(absenceTypeMinutes), false, false, false, "661H8");

      AbsenceType m661 = absenceComponentDao.buildOrEditAbsenceType("661M", 
          "Permesso orario per motivi personali in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);
      AbsenceType h1c661 = absenceComponentDao.buildOrEditAbsenceType("661H1C", 
          "Permesso orario per motivi personali completamento 1 ora", 
          60, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h2c661 = absenceComponentDao.buildOrEditAbsenceType("661H2C", 
          "Permesso orario per motivi personali completamento 2 ore", 
          120, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h3c661 = absenceComponentDao.buildOrEditAbsenceType("661H3C", 
          "Permesso orario per motivi personali completamento 3 ore", 
          180, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h4c661 = absenceComponentDao.buildOrEditAbsenceType("661H4C", 
          "Permesso orario per motivi personali completamento 4 ore", 
          240, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h5c661 = absenceComponentDao.buildOrEditAbsenceType("661H5C", 
          "Permesso orario per motivi personali completamento 5 ore", 
          300, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h6c661 = absenceComponentDao.buildOrEditAbsenceType("661H6C", 
          "Permesso orario per motivi personali completamento 6 ore", 
          360, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h7c661 = absenceComponentDao.buildOrEditAbsenceType("661H7C", 
          "Permesso orario per motivi personali completamento 7 ore", 
          420, Sets.newHashSet(nothing), true, false, false, null);
      AbsenceType h8c661 = absenceComponentDao.buildOrEditAbsenceType("661H8C", 
          "Permesso orario per motivi personali completamento 8 ore", 
          480, Sets.newHashSet(nothing), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c661 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_661.name());

      if (!c661.isPresent()) {

        c661 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c661.get().name = DefaultComplation.C_661.name();
        c661.get().amountType = AmountType.minutes;
        c661.get().complationCodes.add(m661);
        c661.get().replacingCodes = 
            Sets.newHashSet(h1c661, h2c661, h3c661, h4c661, h5c661, h6c661, h7c661, h8c661);
        c661.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t661 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_661.name());

      if (!t661.isPresent()) {

        t661 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t661.get().name = DefaultTakable.T_661.name();
        t661.get().amountType = AmountType.minutes;
        t661.get().takableCodes = Sets.newHashSet(m661, h1661, h2661, h3661, h4661, h5661, h6661, h7661, h8661);
        t661.get().takenCodes = Sets.newHashSet(m661, h1661, h2661, h3661, h4661, h5661, h6661, h7661, h8661);
        t661.get().fixedLimit = 1080;
        t661.get().takableAmountAdjustment = TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent;
        t661.get().save();
      }

      // Group Creation
      GroupAbsenceType group661 = new GroupAbsenceType();
      group661.category = permissionCategory;
      group661.name = DefaultGroup.G_661.name();
      group661.description = "Permesso orario per motivi personali 18 ore anno";
      group661.pattern = GroupAbsenceTypePattern.programmed;
      group661.periodType = PeriodType.year;
      group661.complationAbsenceBehaviour = c661.get();
      group661.takableAbsenceBehaviour = t661.get();
      group661.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_25.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr25 = absenceComponentDao.buildOrEditAbsenceType("25", 
          "Astensione facoltativa post partum 30% primo figlio intera giornata", 
          0, Sets.newHashSet(allDay), false, false, false, "25");
      
      //Update AbsenceType
      AbsenceType cnr25u = absenceComponentDao.buildOrEditAbsenceType("25U", 
          "Astensione facoltativa post partum 30% primo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(nothing), false, false, false, null);
      
      AbsenceType cnr25h7 = absenceComponentDao.buildOrEditAbsenceType("25H7", 
          "Astensione facoltativa post partum 30% primo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), false, false, false, "25H7");
      
      AbsenceType m25 = absenceComponentDao.buildOrEditAbsenceType("25M", 
          "Astensione facoltativa post partum 30% primo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c25 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_25.name());

      if (!c25.isPresent()) {

        c25 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c25.get().name = DefaultComplation.C_25.name();
        c25.get().amountType = AmountType.units;
        c25.get().complationCodes.add(m25);
        c25.get().replacingCodes = Sets.newHashSet(cnr25h7);
        c25.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t25 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_25.name());

      if (!t25.isPresent()) {

        t25 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t25.get().name = DefaultTakable.T_25.name();
        t25.get().amountType = AmountType.units;
        t25.get().takableCodes = Sets.newHashSet(cnr25, cnr25u, m25);
        t25.get().takenCodes = Sets.newHashSet(cnr25, cnr25u, m25);
        t25.get().fixedLimit = 150;
        t25.get().save();
      }

      // Group Creation
      GroupAbsenceType group25 = new GroupAbsenceType();
      group25.category = postpartumCategory;
      group25.name = DefaultGroup.G_25.name();
      group25.description = "Astensione facoltativa post partum 30% primo figlio 0-6 anni 150 giorni";
      group25.pattern = GroupAbsenceTypePattern.programmed;
      group25.periodType = PeriodType.child1_0_6;
      group25.complationAbsenceBehaviour = c25.get();
      group25.takableAbsenceBehaviour = t25.get();
      group25.save();

    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_23.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr23 = absenceComponentDao.buildOrEditAbsenceType("23", 
          "Astensione facoltativa post partum 100% primo figlio intera giornata", 
          0, Sets.newHashSet(allDay), false, false, false, "23");
      
      //Update AbsenceType
      AbsenceType cnr23u = absenceComponentDao.buildOrEditAbsenceType("23U", 
          "Astensione facoltativa post partum 100% primo figlio intera giornata altro genitore", 
          0, Sets.newHashSet(nothing), false, false, false, null);
      
      AbsenceType cnr23h7 = absenceComponentDao.buildOrEditAbsenceType("23H7", 
          "Astensione facoltativa post partum 100% primo figlio completamento giornata", 
          0, Sets.newHashSet(nothing), false, false, false, "23H7");
      
      AbsenceType m23 = absenceComponentDao.buildOrEditAbsenceType("23M", 
          "Astensione facoltativa post partum 100% primo figlio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c23 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_23.name());

      if (!c23.isPresent()) {

        c23 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c23.get().name = DefaultComplation.C_23.name();
        c23.get().amountType = AmountType.units;
        c23.get().complationCodes.add(m23);
        c23.get().replacingCodes = Sets.newHashSet(cnr23h7);
        c23.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t23 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_23.name());

      if (!t23.isPresent()) {

        t23 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t23.get().name = DefaultTakable.T_23.name();
        t23.get().amountType = AmountType.units;
        t23.get().takableCodes = Sets.newHashSet(cnr23, cnr23u, m23);
        t23.get().takenCodes = Sets.newHashSet(cnr23, cnr23u, m23);
        t23.get().fixedLimit = 30;
        t23.get().save();
      }

      // Group Creation
      GroupAbsenceType group23 = new GroupAbsenceType();
      group23.category = postpartumCategory;
      group23.name = DefaultGroup.G_23.name();
      group23.description = "Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni";
      group23.chainDescription = "Astensione facoltativa post partum primo figlio";
      group23.pattern = GroupAbsenceTypePattern.programmed;
      group23.periodType = PeriodType.child1_0_12;
      group23.complationAbsenceBehaviour = c23.get();
      group23.takableAbsenceBehaviour = t23.get();
      
      group23.nextGroupToCheck = absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_25.name()).get();
      
      group23.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_89.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr89 = absenceComponentDao.buildOrEditAbsenceType("89", 
          "Permesso diritto allo studio completamento giornata", 
          0, Sets.newHashSet(nothing), false, false, false, "89");
      
      AbsenceType m89 = absenceComponentDao.buildOrEditAbsenceType("89M", 
          "Permesso diritto allo studio in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c89 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_89.name());

      if (!c89.isPresent()) {

        c89 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c89.get().name = DefaultComplation.C_89.name();
        c89.get().amountType = AmountType.units;
        c89.get().complationCodes.add(m89);
        c89.get().replacingCodes = Sets.newHashSet(cnr89);
        c89.get().save();

      }

      //Takable Creation
      Optional<TakableAbsenceBehaviour> t89 = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_89.name());

      if (!t89.isPresent()) {

        t89 = Optional.fromNullable(new TakableAbsenceBehaviour());
        t89.get().name = DefaultTakable.T_89.name();
        t89.get().amountType = AmountType.minutes;
        t89.get().takableCodes = Sets.newHashSet(m89);
        t89.get().takenCodes = Sets.newHashSet(m89);
        t89.get().fixedLimit = 9000;
        t89.get().takableAmountAdjustment = TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent;
        t89.get().save();
      }

      // Group Creation
      GroupAbsenceType group89 = new GroupAbsenceType();
      group89.category = permissionCategory;
      group89.name = DefaultGroup.G_89.name();
      group89.description = "Permesso diritto allo studio 150 ore anno";
      group89.pattern = GroupAbsenceTypePattern.programmed;
      group89.periodType = PeriodType.year;
      group89.complationAbsenceBehaviour = c89.get();
      group89.takableAbsenceBehaviour = t89.get();
      group89.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.G_09.name()).isPresent()) {

      //Update AbsenceType
      AbsenceType cnr09B = absenceComponentDao.buildOrEditAbsenceType("09B", 
          "Permesso visita medica completamento giornata", 
          0, Sets.newHashSet(nothing), false, false, false, "09B");
      
      AbsenceType m09 = absenceComponentDao.buildOrEditAbsenceType("09M", 
          "Permesso visita medica in ore e minuti", 
          0, Sets.newHashSet(specifiedMinutes), true, false, false, null);


      //Complation Creation
      Optional<ComplationAbsenceBehaviour> c09 = absenceComponentDao
          .complationAbsenceBehaviourByName(DefaultComplation.C_09.name());

      if (!c09.isPresent()) {

        c09 = Optional.fromNullable(new ComplationAbsenceBehaviour());
        c09.get().name = DefaultComplation.C_09.name();
        c09.get().amountType = AmountType.units;
        c09.get().complationCodes.add(m09);
        c09.get().replacingCodes = Sets.newHashSet(cnr09B);
        c09.get().save();

      }

      // Group Creation
      GroupAbsenceType group09 = new GroupAbsenceType();
      group09.category = permissionCategory;
      group09.name = DefaultGroup.G_09.name();
      group09.description = "Permesso visita medica";
      group09.pattern = GroupAbsenceTypePattern.programmed;
      group09.periodType = PeriodType.always;
      group09.complationAbsenceBehaviour = c09.get();
      group09.save();

    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.MISSIONE.name()).isPresent()) {
      
      //Complation Creation
      Optional<TakableAbsenceBehaviour> tMissione = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_MISSIONE.name());

      if (!tMissione.isPresent()) {

        tMissione = Optional.fromNullable(new TakableAbsenceBehaviour());
        tMissione.get().name = DefaultTakable.T_MISSIONE.name();
        tMissione.get().amountType = AmountType.units;

        AbsenceType missione92 = absenceComponentDao.buildOrEditAbsenceType("92", 
            "Missione", 0, Sets.newHashSet(allDay), false, true, false, "92");
        
        AbsenceType h192 = absenceComponentDao.buildOrEditAbsenceType("92H1", 
            "Missione 1 ora", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H1");
        
        AbsenceType h292 = absenceComponentDao.buildOrEditAbsenceType("92H2", 
            "Missione 2 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H2");
        
        AbsenceType h392 = absenceComponentDao.buildOrEditAbsenceType("92H3", 
            "Missione 3 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H3");
        
        AbsenceType h492 = absenceComponentDao.buildOrEditAbsenceType("92H4", 
            "Missione 4 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H4");
        
        AbsenceType h592 = absenceComponentDao.buildOrEditAbsenceType("92H5", 
            "Missione 5 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H5");
        
        AbsenceType h692 = absenceComponentDao.buildOrEditAbsenceType("92H6", 
            "Missione 6 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H6");
        
        AbsenceType h792 = absenceComponentDao.buildOrEditAbsenceType("92H7", 
            "Missione 7 ore", 60, Sets.newHashSet(absenceTypeMinutes), false, true, false, "92H7");
        
        tMissione.get().takableCodes = Sets
            .newHashSet(missione92, h192, h292, h392, h492, h592, h692, h792);
        
        //tMissione.get().takenCodes = Sets
        //    .newHashSet(missione92, h192, h292, h392, h492, h592, h692, h792);
        tMissione.get().fixedLimit = -1;
        tMissione.get().save();

      }
      
      // Group Creation
      GroupAbsenceType groupMission = new GroupAbsenceType();
      groupMission.category = generalCategory;
      groupMission.name = DefaultGroup.MISSIONE.name();
      groupMission.description = "Missione";
      groupMission.pattern = GroupAbsenceTypePattern.simpleGrouping;
      groupMission.periodType = PeriodType.always;
      groupMission.takableAbsenceBehaviour = tMissione.get();
      groupMission.save();
    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.ALTRI.name()).isPresent()) {
      
      //Complation Creation
      Optional<TakableAbsenceBehaviour> tAltri = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_ALTRI.name());
      
      if (!tAltri.isPresent()) {

        tAltri = Optional.fromNullable(new TakableAbsenceBehaviour());
        tAltri.get().name = DefaultTakable.T_ALTRI.name();
        tAltri.get().amountType = AmountType.units;
        tAltri.get().takableCodes = Sets.newHashSet();
        tAltri.get().takenCodes = Sets.newHashSet();
        tAltri.get().fixedLimit = -1;
        tAltri.get().save();
      }
      
      // Set boolean independente
      absenceTypes = AbsenceType.findAll();
      for (AbsenceType absenceType : absenceTypes) {
        if (absenceType.takableGroup.isEmpty() && absenceType.takenGroup.isEmpty() 
            && absenceType.complationGroup.isEmpty() && absenceType.replacingGroup.isEmpty()) {
          tAltri.get().takableCodes.add(absenceType);
          tAltri.get().save();
        }
      }

      // Group Creation
      GroupAbsenceType groupAltri = new GroupAbsenceType();
      groupAltri.category = otherCategory;
      groupAltri.name = DefaultGroup.ALTRI.name();
      groupAltri.description = "Altri Codici";
      groupAltri.pattern = GroupAbsenceTypePattern.simpleGrouping;
      groupAltri.periodType = PeriodType.always;
      groupAltri.takableAbsenceBehaviour = tAltri.get();
      groupAltri.save();
    }
    
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).isPresent()) {
      
      //Complation Creation
      Optional<TakableAbsenceBehaviour> tFerie = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_FERIE_CNR.name());
      
      if (!tFerie.isPresent()) {

        tFerie = Optional.fromNullable(new TakableAbsenceBehaviour());
        tFerie.get().name = DefaultTakable.T_FERIE_CNR.name();
        tFerie.get().amountType = AmountType.units;
        tFerie.get().takableCodes = Sets.newHashSet();
        tFerie.get().takenCodes = Sets.newHashSet();
        tFerie.get().fixedLimit = -1;
        
        AbsenceType ferie32 = absenceComponentDao.buildOrEditAbsenceType("32", 
            "Ferie anno corrente", 0, Sets.newHashSet(allDay), false, true, false, "32");
        
        AbsenceType ferie31 = absenceComponentDao.buildOrEditAbsenceType("31", 
            "Ferie anno precedente", 0, Sets.newHashSet(allDay), false, true, false, "31");
        
        AbsenceType ferie37 = absenceComponentDao.buildOrEditAbsenceType("37", 
            "ferie anno precedente (dopo il 31/8)", 0, Sets.newHashSet(allDay), false, true, false, "37");
        
        AbsenceType permesso94 = absenceComponentDao.buildOrEditAbsenceType("94", 
            "festività soppresse (ex legge 937/77)", 0, Sets.newHashSet(allDay), false, true, false, "94");
        
        tFerie.get().takableCodes = Sets.newHashSet(ferie31, ferie32, ferie37, permesso94);
        
        tFerie.get().save();
      }
      
      

      // Group Creation
      GroupAbsenceType groupFerieCnr = new GroupAbsenceType();
      groupFerieCnr.category = generalCategory;
      groupFerieCnr.name = DefaultGroup.FERIE_CNR.name();
      groupFerieCnr.description = "Ferie e permessi legge CNR";
      groupFerieCnr.pattern = GroupAbsenceTypePattern.vacationsCnr;
      groupFerieCnr.periodType = PeriodType.always;
      groupFerieCnr.takableAbsenceBehaviour = tFerie.get();
      groupFerieCnr.save();
    }
    
    if (!absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR.name()).isPresent()) {
      
      //Complation Creation
      Optional<TakableAbsenceBehaviour> tRiposi = absenceComponentDao
          .takableAbsenceBehaviourByName(DefaultTakable.T_RIPOSI_CNR.name());
      
      if (!tRiposi.isPresent()) {

        tRiposi = Optional.fromNullable(new TakableAbsenceBehaviour());
        tRiposi.get().name = DefaultTakable.T_RIPOSI_CNR.name();
        tRiposi.get().amountType = AmountType.units;
        tRiposi.get().takableCodes = Sets.newHashSet();
        tRiposi.get().takenCodes = Sets.newHashSet();
        tRiposi.get().fixedLimit = -1;
        
        AbsenceType riposo91 = absenceComponentDao.buildOrEditAbsenceType("91", 
            "Riposo compensativo", 0, Sets.newHashSet(allDay), false, true, false, "92");
        
        tRiposi.get().takableCodes = Sets.newHashSet(riposo91);
        
        tRiposi.get().save();
      }

      // Group Creation
      GroupAbsenceType groupRiposi = new GroupAbsenceType();
      groupRiposi.category = generalCategory;
      groupRiposi.name = DefaultGroup.RIPOSI_CNR.name();
      groupRiposi.description = "Riposi compensativi CNR";
      groupRiposi.pattern = GroupAbsenceTypePattern.compensatoryRestCnr;
      groupRiposi.periodType = PeriodType.always;
      groupRiposi.takableAbsenceBehaviour = tRiposi.get();
      groupRiposi.save();
    }
    
  }

}
