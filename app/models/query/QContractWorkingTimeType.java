package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ContractWorkingTimeType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContractWorkingTimeType is a Querydsl query type for ContractWorkingTimeType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractWorkingTimeType extends EntityPathBase<ContractWorkingTimeType> {

    private static final long serialVersionUID = 162260706L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractWorkingTimeType contractWorkingTimeType = new QContractWorkingTimeType("contractWorkingTimeType");

    public final models.base.query.QPeriodModel _super;

    //inherited
    public final DatePath<org.joda.time.LocalDate> begin;

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final QContract contract;

    //inherited
    public final SimplePath<com.google.common.base.Optional<org.joda.time.LocalDate>> end;

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId;

    //inherited
    public final NumberPath<Long> id;

    //inherited
    public final BooleanPath persistent;

    public final QWorkingTimeType workingTimeType;

    public QContractWorkingTimeType(String variable) {
        this(ContractWorkingTimeType.class, forVariable(variable), INITS);
    }

    public QContractWorkingTimeType(Path<? extends ContractWorkingTimeType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractWorkingTimeType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractWorkingTimeType(PathMetadata<?> metadata, PathInits inits) {
        this(ContractWorkingTimeType.class, metadata, inits);
    }

    public QContractWorkingTimeType(Class<? extends ContractWorkingTimeType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new models.base.query.QPeriodModel(type, metadata, inits);
        this.begin = _super.begin;
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.end = _super.end;
        this.entityId = _super.entityId;
        this.id = _super.id;
        this.persistent = _super.persistent;
        this.workingTimeType = inits.isInitialized("workingTimeType") ? new QWorkingTimeType(forProperty("workingTimeType"), inits.get("workingTimeType")) : null;
    }

}

