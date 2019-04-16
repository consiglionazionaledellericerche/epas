package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.UsersRolesOffices;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUsersRolesOffices is a Querydsl query type for UsersRolesOffices
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QUsersRolesOffices extends EntityPathBase<UsersRolesOffices> {

    private static final long serialVersionUID = -100507842L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUsersRolesOffices usersRolesOffices = new QUsersRolesOffices("usersRolesOffices");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QRole role;

    public final QUser user;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QUsersRolesOffices(String variable) {
        this(UsersRolesOffices.class, forVariable(variable), INITS);
    }

    public QUsersRolesOffices(Path<? extends UsersRolesOffices> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUsersRolesOffices(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUsersRolesOffices(PathMetadata metadata, PathInits inits) {
        this(UsersRolesOffices.class, metadata, inits);
    }

    public QUsersRolesOffices(Class<? extends UsersRolesOffices> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.role = inits.isInitialized("role") ? new QRole(forProperty("role")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

