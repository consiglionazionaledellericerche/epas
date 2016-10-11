package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.AbsenceTrouble;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsenceTrouble is a Querydsl query type for AbsenceTrouble
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsenceTrouble extends EntityPathBase<AbsenceTrouble> {

    private static final long serialVersionUID = -195189586L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceTrouble absenceTrouble = new QAbsenceTrouble("absenceTrouble");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QAbsence absence;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final EnumPath<AbsenceTrouble.AbsenceProblem> trouble = createEnum("trouble", AbsenceTrouble.AbsenceProblem.class);

    public QAbsenceTrouble(String variable) {
        this(AbsenceTrouble.class, forVariable(variable), INITS);
    }

    public QAbsenceTrouble(Path<? extends AbsenceTrouble> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceTrouble(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceTrouble(PathMetadata<?> metadata, PathInits inits) {
        this(AbsenceTrouble.class, metadata, inits);
    }

    public QAbsenceTrouble(Class<? extends AbsenceTrouble> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absence = inits.isInitialized("absence") ? new QAbsence(forProperty("absence"), inits.get("absence")) : null;
    }

}

