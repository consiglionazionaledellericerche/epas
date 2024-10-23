package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.GroupOvertime;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroupOvertime is a Querydsl query type for GroupOvertime
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QGroupOvertime extends EntityPathBase<GroupOvertime> {

    private static final long serialVersionUID = 1330496668L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupOvertime groupOvertime = new QGroupOvertime("groupOvertime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> dateOfUpdate = createDate("dateOfUpdate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final models.flows.query.QGroup group;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHours = createNumber("numberOfHours", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QGroupOvertime(String variable) {
        this(GroupOvertime.class, forVariable(variable), INITS);
    }

    public QGroupOvertime(Path<? extends GroupOvertime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroupOvertime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroupOvertime(PathMetadata metadata, PathInits inits) {
        this(GroupOvertime.class, metadata, inits);
    }

    public QGroupOvertime(Class<? extends GroupOvertime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new models.flows.query.QGroup(forProperty("group"), inits.get("group")) : null;
    }

}

