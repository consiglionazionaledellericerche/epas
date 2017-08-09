package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ReperibilityTypeMonth;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QReperibilityTypeMonth is a Querydsl query type for ReperibilityTypeMonth
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QReperibilityTypeMonth extends EntityPathBase<ReperibilityTypeMonth> {

    private static final long serialVersionUID = -850160198L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReperibilityTypeMonth reperibilityTypeMonth = new QReperibilityTypeMonth("reperibilityTypeMonth");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final BooleanPath approved = createBoolean("approved");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonReperibilityType personReperibilityType;

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public final ComparablePath<org.joda.time.YearMonth> yearMonth = createComparable("yearMonth", org.joda.time.YearMonth.class);

    public QReperibilityTypeMonth(String variable) {
        this(ReperibilityTypeMonth.class, forVariable(variable), INITS);
    }

    public QReperibilityTypeMonth(Path<? extends ReperibilityTypeMonth> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QReperibilityTypeMonth(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QReperibilityTypeMonth(PathMetadata<?> metadata, PathInits inits) {
        this(ReperibilityTypeMonth.class, metadata, inits);
    }

    public QReperibilityTypeMonth(Class<? extends ReperibilityTypeMonth> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personReperibilityType = inits.isInitialized("personReperibilityType") ? new QPersonReperibilityType(forProperty("personReperibilityType"), inits.get("personReperibilityType")) : null;
    }

}

