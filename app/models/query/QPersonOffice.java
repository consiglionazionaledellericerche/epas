package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonOffice;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonOffice is a Querydsl query type for PersonOffice
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonOffice extends EntityPathBase<PersonOffice> {

    private static final long serialVersionUID = -1203596187L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonOffice personOffice = new QPersonOffice("personOffice");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonOffice(String variable) {
        this(PersonOffice.class, forVariable(variable), INITS);
    }

    public QPersonOffice(Path<? extends PersonOffice> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonOffice(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonOffice(PathMetadata metadata, PathInits inits) {
        this(PersonOffice.class, metadata, inits);
    }

    public QPersonOffice(Class<? extends PersonOffice> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}
