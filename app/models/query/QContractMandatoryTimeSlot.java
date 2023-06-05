package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ContractMandatoryTimeSlot;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractMandatoryTimeSlot is a Querydsl query type for ContractMandatoryTimeSlot
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QContractMandatoryTimeSlot extends EntityPathBase<ContractMandatoryTimeSlot> {

    private static final long serialVersionUID = 1533674958L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractMandatoryTimeSlot contractMandatoryTimeSlot = new QContractMandatoryTimeSlot("contractMandatoryTimeSlot");

    public final models.base.query.QPropertyInPeriod _super = new models.base.query.QPropertyInPeriod(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final QContract contract;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QTimeSlot timeSlot;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QContractMandatoryTimeSlot(String variable) {
        this(ContractMandatoryTimeSlot.class, forVariable(variable), INITS);
    }

    public QContractMandatoryTimeSlot(Path<? extends ContractMandatoryTimeSlot> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractMandatoryTimeSlot(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractMandatoryTimeSlot(PathMetadata metadata, PathInits inits) {
        this(ContractMandatoryTimeSlot.class, metadata, inits);
    }

    public QContractMandatoryTimeSlot(Class<? extends ContractMandatoryTimeSlot> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.timeSlot = inits.isInitialized("timeSlot") ? new QTimeSlot(forProperty("timeSlot"), inits.get("timeSlot")) : null;
    }

}

