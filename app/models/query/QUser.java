package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.User;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1339629807L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QBadgeReader badgeReader;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> expireRecoveryToken = createDate("expireRecoveryToken", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath password = createString("password");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath recoveryToken = createString("recoveryToken");

    public final StringPath username = createString("username");

    public final ListPath<models.UsersRolesOffices, QUsersRolesOffices> usersRolesOffices = this.<models.UsersRolesOffices, QUsersRolesOffices>createList("usersRolesOffices", models.UsersRolesOffices.class, QUsersRolesOffices.class, PathInits.DIRECT2);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUser(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUser(PathMetadata<?> metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.badgeReader = inits.isInitialized("badgeReader") ? new QBadgeReader(forProperty("badgeReader"), inits.get("badgeReader")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

