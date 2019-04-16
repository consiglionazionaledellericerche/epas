package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Configuration;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QConfiguration is a Querydsl query type for Configuration
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QConfiguration extends EntityPathBase<Configuration> {

    private static final long serialVersionUID = 680577106L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConfiguration configuration = new QConfiguration("configuration");

    public final models.base.query.QPropertyInPeriod _super = new models.base.query.QPropertyInPeriod(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final EnumPath<manager.configurations.EpasParam> epasParam = createEnum("epasParam", manager.configurations.EpasParam.class);

    public final StringPath fieldValue = createString("fieldValue");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QConfiguration(String variable) {
        this(Configuration.class, forVariable(variable), INITS);
    }

    public QConfiguration(Path<? extends Configuration> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QConfiguration(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QConfiguration(PathMetadata metadata, PathInits inits) {
        this(Configuration.class, metadata, inits);
    }

    public QConfiguration(Class<? extends Configuration> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

