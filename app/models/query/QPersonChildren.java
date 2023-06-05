package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonChildren;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonChildren is a Querydsl query type for PersonChildren
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonChildren extends EntityPathBase<PersonChildren> {

    private static final long serialVersionUID = 954421400L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonChildren personChildren = new QPersonChildren("personChildren");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> bornDate = createDate("bornDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath surname = createString("surname");

    public final StringPath taxCode = createString("taxCode");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonChildren(String variable) {
        this(PersonChildren.class, forVariable(variable), INITS);
    }

    public QPersonChildren(Path<? extends PersonChildren> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonChildren(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonChildren(PathMetadata metadata, PathInits inits) {
        this(PersonChildren.class, metadata, inits);
    }

    public QPersonChildren(Class<? extends PersonChildren> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

