package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Office;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QOffice is a Querydsl query type for Office
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QOffice extends EntityPathBase<Office> {

    private static final long serialVersionUID = -1289700640L;

    public static final QOffice office = new QOffice("office");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath address = createString("address");

    public final NumberPath<Integer> code = createNumber("code", Integer.class);

    public final ListPath<models.ConfGeneral, QConfGeneral> confGeneral = this.<models.ConfGeneral, QConfGeneral>createList("confGeneral", models.ConfGeneral.class, QConfGeneral.class, PathInits.DIRECT2);

    public final ListPath<models.ConfYear, QConfYear> confYear = this.<models.ConfYear, QConfYear>createList("confYear", models.ConfYear.class, QConfYear.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final ListPath<models.RemoteOffice, QRemoteOffice> remoteOffices = this.<models.RemoteOffice, QRemoteOffice>createList("remoteOffices", models.RemoteOffice.class, QRemoteOffice.class, PathInits.DIRECT2);

    public final ListPath<models.UsersPermissionsOffices, QUsersPermissionsOffices> userPermissionOffices = this.<models.UsersPermissionsOffices, QUsersPermissionsOffices>createList("userPermissionOffices", models.UsersPermissionsOffices.class, QUsersPermissionsOffices.class, PathInits.DIRECT2);

    public QOffice(String variable) {
        super(Office.class, forVariable(variable));
    }

    public QOffice(Path<? extends Office> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOffice(PathMetadata<?> metadata) {
        super(Office.class, metadata);
    }

}

