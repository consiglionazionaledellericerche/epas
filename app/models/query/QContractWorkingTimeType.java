package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.ContractWorkingTimeType;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;


/**
 * QContractWorkingTimeType is a Querydsl query type for ContractWorkingTimeType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractWorkingTimeType extends EntityPathBase<ContractWorkingTimeType> {

    private static final long serialVersionUID = 162260706L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractWorkingTimeType contractWorkingTimeType = new QContractWorkingTimeType("contractWorkingTimeType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final QContract contract;

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

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
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.workingTimeType = inits.isInitialized("workingTimeType") ? new QWorkingTimeType(forProperty("workingTimeType"), inits.get("workingTimeType")) : null;
    }

}

