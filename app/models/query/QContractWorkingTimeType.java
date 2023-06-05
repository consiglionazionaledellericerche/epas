package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ContractWorkingTimeType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractWorkingTimeType is a Querydsl query type for ContractWorkingTimeType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QContractWorkingTimeType extends EntityPathBase<ContractWorkingTimeType> {

    private static final long serialVersionUID = 162260706L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractWorkingTimeType contractWorkingTimeType = new QContractWorkingTimeType("contractWorkingTimeType");

    public final models.base.query.QPropertyInPeriod _super = new models.base.query.QPropertyInPeriod(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final QContract contract;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final QWorkingTimeType workingTimeType;

    public QContractWorkingTimeType(String variable) {
        this(ContractWorkingTimeType.class, forVariable(variable), INITS);
    }

    public QContractWorkingTimeType(Path<? extends ContractWorkingTimeType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractWorkingTimeType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractWorkingTimeType(PathMetadata metadata, PathInits inits) {
        this(ContractWorkingTimeType.class, metadata, inits);
    }

    public QContractWorkingTimeType(Class<? extends ContractWorkingTimeType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.workingTimeType = inits.isInitialized("workingTimeType") ? new QWorkingTimeType(forProperty("workingTimeType"), inits.get("workingTimeType")) : null;
    }

}

