package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonsOffices;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonsOffices is a Querydsl query type for PersonsOffices
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonsOffices extends EntityPathBase<PersonsOffices> {

    private static final long serialVersionUID = -2094078019L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonsOffices personsOffices = new QPersonsOffices("personsOffices");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonsOffices(String variable) {
        this(PersonsOffices.class, forVariable(variable), INITS);
    }

    public QPersonsOffices(Path<? extends PersonsOffices> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonsOffices(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonsOffices(PathMetadata metadata, PathInits inits) {
        this(PersonsOffices.class, metadata, inits);
    }

    public QPersonsOffices(Class<? extends PersonsOffices> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

