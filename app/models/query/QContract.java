package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Contract;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContract is a Querydsl query type for Contract
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContract extends EntityPathBase<Contract> {

    private static final long serialVersionUID = 2041582646L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContract contract = new QContract("contract");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final ListPath<models.ContractMonthRecap, QContractMonthRecap> contractMonthRecaps = this.<models.ContractMonthRecap, QContractMonthRecap>createList("contractMonthRecaps", models.ContractMonthRecap.class, QContractMonthRecap.class, PathInits.DIRECT2);

    public final SetPath<models.ContractStampProfile, QContractStampProfile> contractStampProfile = this.<models.ContractStampProfile, QContractStampProfile>createSet("contractStampProfile", models.ContractStampProfile.class, QContractStampProfile.class, PathInits.DIRECT2);

    public final SetPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createSet("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final DatePath<org.joda.time.LocalDate> endContract = createDate("endContract", org.joda.time.LocalDate.class);

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isTemporaryMissing = createBoolean("isTemporaryMissing");

    public final ListPath<models.MealTicket, QMealTicket> mealTickets = this.<models.MealTicket, QMealTicket>createList("mealTickets", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final BooleanPath onCertificate = createBoolean("onCertificate");

    public final NumberPath<Long> perseoId = createNumber("perseoId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final BooleanPath sourceByAdmin = createBoolean("sourceByAdmin");

    public final DatePath<org.joda.time.LocalDate> sourceDateMealTicket = createDate("sourceDateMealTicket", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> sourceDateResidual = createDate("sourceDateResidual", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> sourcePermissionUsed = createNumber("sourcePermissionUsed", Integer.class);

    public final NumberPath<Integer> sourceRecoveryDayUsed = createNumber("sourceRecoveryDayUsed", Integer.class);

    public final NumberPath<Integer> sourceRemainingMealTicket = createNumber("sourceRemainingMealTicket", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesCurrentYear = createNumber("sourceRemainingMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesLastYear = createNumber("sourceRemainingMinutesLastYear", Integer.class);

    public final NumberPath<Integer> sourceVacationCurrentYearUsed = createNumber("sourceVacationCurrentYearUsed", Integer.class);

    public final NumberPath<Integer> sourceVacationLastYearUsed = createNumber("sourceVacationLastYearUsed", Integer.class);

    public final ListPath<models.VacationPeriod, QVacationPeriod> vacationPeriods = this.<models.VacationPeriod, QVacationPeriod>createList("vacationPeriods", models.VacationPeriod.class, QVacationPeriod.class, PathInits.DIRECT2);

    public QContract(String variable) {
        this(Contract.class, forVariable(variable), INITS);
    }

    public QContract(Path<? extends Contract> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContract(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContract(PathMetadata<?> metadata, PathInits inits) {
        this(Contract.class, metadata, inits);
    }

    public QContract(Class<? extends Contract> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

