package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.AbsenceTypeGroup;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsenceTypeGroup is a Querydsl query type for AbsenceTypeGroup
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsenceTypeGroup extends EntityPathBase<AbsenceTypeGroup> {

    private static final long serialVersionUID = -556639028L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceTypeGroup absenceTypeGroup = new QAbsenceTypeGroup("absenceTypeGroup");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.AbsenceType, QAbsenceType> absenceTypes = this.<models.AbsenceType, QAbsenceType>createList("absenceTypes", models.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    public final EnumPath<models.enumerate.AccumulationBehaviour> accumulationBehaviour = createEnum("accumulationBehaviour", models.enumerate.AccumulationBehaviour.class);

    public final EnumPath<models.enumerate.AccumulationType> accumulationType = createEnum("accumulationType", models.enumerate.AccumulationType.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath label = createString("label");

    public final NumberPath<Integer> limitInMinute = createNumber("limitInMinute", Integer.class);

    public final BooleanPath minutesExcess = createBoolean("minutesExcess");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QAbsenceType replacingAbsenceType;

    public QAbsenceTypeGroup(String variable) {
        this(AbsenceTypeGroup.class, forVariable(variable), INITS);
    }

    public QAbsenceTypeGroup(Path<? extends AbsenceTypeGroup> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceTypeGroup(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceTypeGroup(PathMetadata<?> metadata, PathInits inits) {
        this(AbsenceTypeGroup.class, metadata, inits);
    }

    public QAbsenceTypeGroup(Class<? extends AbsenceTypeGroup> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.replacingAbsenceType = inits.isInitialized("replacingAbsenceType") ? new QAbsenceType(forProperty("replacingAbsenceType"), inits.get("replacingAbsenceType")) : null;
    }

}

