package models.absences.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.absences.AbsenceTrouble;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAbsenceTrouble is a Querydsl query type for AbsenceTrouble
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAbsenceTrouble(String variable) {
        this(AbsenceTrouble.class, forVariable(variable), INITS);
    }

    public QAbsenceTrouble(Path<? extends AbsenceTrouble> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAbsenceTrouble(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAbsenceTrouble(PathMetadata metadata, PathInits inits) {
        this(AbsenceTrouble.class, metadata, inits);
    }

    public QAbsenceTrouble(Class<? extends AbsenceTrouble> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absence = inits.isInitialized("absence") ? new QAbsence(forProperty("absence"), inits.get("absence")) : null;
    }

}

