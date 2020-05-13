package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.TeleworkStamping;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTeleworkStamping is a Querydsl query type for TeleworkStamping
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTeleworkStamping extends EntityPathBase<TeleworkStamping> {

    private static final long serialVersionUID = -630270146L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTeleworkStamping teleworkStamping = new QTeleworkStamping("teleworkStamping");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DateTimePath<org.joda.time.LocalDateTime> date = createDateTime("date", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath note = createString("note");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonDay personDay;

    public final EnumPath<models.enumerate.StampTypes> stampType = createEnum("stampType", models.enumerate.StampTypes.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QTeleworkStamping(String variable) {
        this(TeleworkStamping.class, forVariable(variable), INITS);
    }

    public QTeleworkStamping(Path<? extends TeleworkStamping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTeleworkStamping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTeleworkStamping(PathMetadata metadata, PathInits inits) {
        this(TeleworkStamping.class, metadata, inits);
    }

    public QTeleworkStamping(Class<? extends TeleworkStamping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personDay = inits.isInitialized("personDay") ? new QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
    }

}

