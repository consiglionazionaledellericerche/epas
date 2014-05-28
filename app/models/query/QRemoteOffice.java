package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.RemoteOffice;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QRemoteOffice is a Querydsl query type for RemoteOffice
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QRemoteOffice extends EntityPathBase<RemoteOffice> {

    private static final long serialVersionUID = -2013386810L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRemoteOffice remoteOffice = new QRemoteOffice("remoteOffice");

    public final QOffice _super = new QOffice(this);

    //inherited
    public final StringPath address = _super.address;

    //inherited
    public final NumberPath<Integer> code = _super.code;

    //inherited
    public final ListPath<models.ConfGeneral, QConfGeneral> confGeneral = _super.confGeneral;

    //inherited
    public final ListPath<models.ConfYear, QConfYear> confYear = _super.confYear;

    //inherited
    public final StringPath contraction = _super.contraction;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DatePath<org.joda.time.LocalDate> joiningDate = createDate("joiningDate", org.joda.time.LocalDate.class);

    //inherited
    public final StringPath name = _super.name;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final ListPath<models.Person, QPerson> persons = _super.persons;

    //inherited
    public final ListPath<RemoteOffice, QRemoteOffice> remoteOffices = _super.remoteOffices;

    //inherited
    public final ListPath<models.UsersRolesOffices, QUsersRolesOffices> usersRolesOffices = _super.usersRolesOffices;

    //inherited
    public final ListPath<models.WorkingTimeType, QWorkingTimeType> workingTimeType = _super.workingTimeType;

    public QRemoteOffice(String variable) {
        this(RemoteOffice.class, forVariable(variable), INITS);
    }

    public QRemoteOffice(Path<? extends RemoteOffice> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QRemoteOffice(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QRemoteOffice(PathMetadata<?> metadata, PathInits inits) {
        this(RemoteOffice.class, metadata, inits);
    }

    public QRemoteOffice(Class<? extends RemoteOffice> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office")) : null;
    }

}

