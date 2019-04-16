package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ConfPeriod;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QConfPeriod is a Querydsl query type for ConfPeriod
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QConfPeriod extends EntityPathBase<ConfPeriod> {

    private static final long serialVersionUID = -1348538327L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConfPeriod confPeriod = new QConfPeriod("confPeriod");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> dateFrom = createDate("dateFrom", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> dateTo = createDate("dateTo", org.joda.time.LocalDate.class);

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

    public QConfPeriod(String variable) {
        this(ConfPeriod.class, forVariable(variable), INITS);
    }

    public QConfPeriod(Path<? extends ConfPeriod> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QConfPeriod(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QConfPeriod(PathMetadata metadata, PathInits inits) {
        this(ConfPeriod.class, metadata, inits);
    }

    public QConfPeriod(Class<? extends ConfPeriod> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

