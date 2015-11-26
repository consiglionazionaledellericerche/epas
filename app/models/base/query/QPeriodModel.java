package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.PeriodModel;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPeriodModel is a Querydsl query type for PeriodModel
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QPeriodModel extends EntityPathBase<PeriodModel> {

    private static final long serialVersionUID = 1259083055L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPeriodModel periodModel = new QPeriodModel("periodModel");

    public final QBaseModel _super = new QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> begin = createDate("begin", org.joda.time.LocalDate.class);

    public final models.query.QContract contract;

    public final SimplePath<com.google.common.base.Optional<org.joda.time.LocalDate>> end = createSimple("end", com.google.common.base.Optional.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QPeriodModel(String variable) {
        this(PeriodModel.class, forVariable(variable), INITS);
    }

    public QPeriodModel(Path<? extends PeriodModel> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPeriodModel(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPeriodModel(PathMetadata<?> metadata, PathInits inits) {
        this(PeriodModel.class, metadata, inits);
    }

    public QPeriodModel(Class<? extends PeriodModel> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new models.query.QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

