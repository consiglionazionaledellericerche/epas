package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.ContractYearRecap;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;


/**
 * QContractYearRecap is a Querydsl query type for ContractYearRecap
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractYearRecap extends EntityPathBase<ContractYearRecap> {

    private static final long serialVersionUID = 180186860L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractYearRecap contractYearRecap = new QContractYearRecap("contractYearRecap");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QContract contract;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> permissionUsed = createNumber("permissionUsed", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> recoveryDayUsed = createNumber("recoveryDayUsed", Integer.class);

    public final NumberPath<Integer> remainingMealTickets = createNumber("remainingMealTickets", Integer.class);

    public final NumberPath<Integer> remainingMinutesCurrentYear = createNumber("remainingMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> remainingMinutesLastYear = createNumber("remainingMinutesLastYear", Integer.class);

    public final NumberPath<Integer> vacationCurrentYearUsed = createNumber("vacationCurrentYearUsed", Integer.class);

    public final NumberPath<Integer> vacationLastYearUsed = createNumber("vacationLastYearUsed", Integer.class);

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QContractYearRecap(String variable) {
        this(ContractYearRecap.class, forVariable(variable), INITS);
    }

    public QContractYearRecap(Path<? extends ContractYearRecap> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractYearRecap(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractYearRecap(PathMetadata<?> metadata, PathInits inits) {
        this(ContractYearRecap.class, metadata, inits);
    }

    public QContractYearRecap(Class<? extends ContractYearRecap> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

