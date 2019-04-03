package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.TimeVariation;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTimeVariation is a Querydsl query type for TimeVariation
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTimeVariation extends EntityPathBase<TimeVariation> {

    private static final long serialVersionUID = 696424738L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTimeVariation timeVariation1 = new QTimeVariation("timeVariation1");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final models.absences.query.QAbsence absence;

    public final DatePath<org.joda.time.LocalDate> dateVariation = createDate("dateVariation", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> timeVariation = createNumber("timeVariation", Integer.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QTimeVariation(String variable) {
        this(TimeVariation.class, forVariable(variable), INITS);
    }

    public QTimeVariation(Path<? extends TimeVariation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTimeVariation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTimeVariation(PathMetadata metadata, PathInits inits) {
        this(TimeVariation.class, metadata, inits);
    }

    public QTimeVariation(Class<? extends TimeVariation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absence = inits.isInitialized("absence") ? new models.absences.query.QAbsence(forProperty("absence"), inits.get("absence")) : null;
    }

}

