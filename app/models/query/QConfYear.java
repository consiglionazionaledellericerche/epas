package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ConfYear;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QConfYear is a Querydsl query type for ConfYear
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QConfYear extends EntityPathBase<ConfYear> {

    private static final long serialVersionUID = 2027912357L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConfYear confYear = new QConfYear("confYear");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath field = createString("field");

    public final StringPath fieldValue = createString("fieldValue");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QConfYear(String variable) {
        this(ConfYear.class, forVariable(variable), INITS);
    }

    public QConfYear(Path<? extends ConfYear> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QConfYear(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QConfYear(PathMetadata metadata, PathInits inits) {
        this(ConfYear.class, metadata, inits);
    }

    public QConfYear(Class<? extends ConfYear> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

