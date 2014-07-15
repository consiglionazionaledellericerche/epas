package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.AuthUser;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QAuthUser is a Querydsl query type for AuthUser
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAuthUser extends EntityPathBase<AuthUser> {

    private static final long serialVersionUID = -254137161L;

    public static final QAuthUser authUser = new QAuthUser("authUser");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath authIp = createString("authIp");

    public final EnumPath<AuthUser.AuthMod> authmod = createEnum("authmod", AuthUser.AuthMod.class);

    public final EnumPath<AuthUser.AuthRed> authred = createEnum("authred", AuthUser.AuthRed.class);

    public final EnumPath<AuthUser.AuthSys> autsys = createEnum("autsys", AuthUser.AuthSys.class);

    public final DateTimePath<java.util.Date> dataCpas = createDateTime("dataCpas", java.util.Date.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath password = createString("password");

    public final StringPath passwordMD5 = createString("passwordMD5");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Short> scadenzaPassword = createNumber("scadenzaPassword", Short.class);

    public final DateTimePath<java.sql.Timestamp> ultimaModifica = createDateTime("ultimaModifica", java.sql.Timestamp.class);

    public final StringPath username = createString("username");

    public QAuthUser(String variable) {
        super(AuthUser.class, forVariable(variable));
    }

    public QAuthUser(Path<? extends AuthUser> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuthUser(PathMetadata<?> metadata) {
        super(AuthUser.class, metadata);
    }

}

