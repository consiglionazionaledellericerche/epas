package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Contract;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContract is a Querydsl query type for Contract
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QContract extends EntityPathBase<Contract> {

    private static final long serialVersionUID = 2041582646L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContract contract = new QContract("contract");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final SetPath<models.ContractMandatoryTimeSlot, QContractMandatoryTimeSlot> contractMandatoryTimeSlots = this.<models.ContractMandatoryTimeSlot, QContractMandatoryTimeSlot>createSet("contractMandatoryTimeSlots", models.ContractMandatoryTimeSlot.class, QContractMandatoryTimeSlot.class, PathInits.DIRECT2);

    public final ListPath<models.ContractMonthRecap, QContractMonthRecap> contractMonthRecaps = this.<models.ContractMonthRecap, QContractMonthRecap>createList("contractMonthRecaps", models.ContractMonthRecap.class, QContractMonthRecap.class, PathInits.DIRECT2);

    public final SetPath<models.ContractStampProfile, QContractStampProfile> contractStampProfile = this.<models.ContractStampProfile, QContractStampProfile>createSet("contractStampProfile", models.ContractStampProfile.class, QContractStampProfile.class, PathInits.DIRECT2);

    public final EnumPath<models.enumerate.ContractType> contractType = createEnum("contractType", models.enumerate.ContractType.class);

    public final SetPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createSet("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final DatePath<org.joda.time.LocalDate> endContract = createDate("endContract", org.joda.time.LocalDate.class);

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isTemporaryMissing = createBoolean("isTemporaryMissing");

    public final StringPath label = createString("label");

    public final ListPath<models.MealTicket, QMealTicket> mealTickets = this.<models.MealTicket, QMealTicket>createList("mealTickets", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final StringPath perseoId = createString("perseoId");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final SetPath<models.PersonalWorkingTime, QPersonalWorkingTime> personalWorkingTimes = this.<models.PersonalWorkingTime, QPersonalWorkingTime>createSet("personalWorkingTimes", models.PersonalWorkingTime.class, QPersonalWorkingTime.class, PathInits.DIRECT2);

    public final QContract previousContract;

    public final SimplePath<com.google.common.collect.Range<org.joda.time.LocalDate>> range = createSimple("range", com.google.common.collect.Range.class);

    public final BooleanPath sourceByAdmin = createBoolean("sourceByAdmin");

    public final DatePath<org.joda.time.LocalDate> sourceDateMealTicket = createDate("sourceDateMealTicket", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> sourceDateRecoveryDay = createDate("sourceDateRecoveryDay", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> sourceDateResidual = createDate("sourceDateResidual", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> sourceDateVacation = createDate("sourceDateVacation", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> sourcePermissionUsed = createNumber("sourcePermissionUsed", Integer.class);

    public final NumberPath<Integer> sourceRecoveryDayUsed = createNumber("sourceRecoveryDayUsed", Integer.class);

    public final NumberPath<Integer> sourceRemainingMealTicket = createNumber("sourceRemainingMealTicket", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesCurrentYear = createNumber("sourceRemainingMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesLastYear = createNumber("sourceRemainingMinutesLastYear", Integer.class);

    public final NumberPath<Integer> sourceVacationCurrentYearUsed = createNumber("sourceVacationCurrentYearUsed", Integer.class);

    public final NumberPath<Integer> sourceVacationLastYearUsed = createNumber("sourceVacationLastYearUsed", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final ListPath<models.VacationPeriod, QVacationPeriod> vacationPeriods = this.<models.VacationPeriod, QVacationPeriod>createList("vacationPeriods", models.VacationPeriod.class, QVacationPeriod.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QContract(String variable) {
        this(Contract.class, forVariable(variable), INITS);
    }

    public QContract(Path<? extends Contract> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContract(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContract(PathMetadata metadata, PathInits inits) {
        this(Contract.class, metadata, inits);
    }

    public QContract(Class<? extends Contract> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
        this.previousContract = inits.isInitialized("previousContract") ? new QContract(forProperty("previousContract"), inits.get("previousContract")) : null;
    }

}

