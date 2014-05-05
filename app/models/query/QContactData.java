package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ContactData;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContactData is a Querydsl query type for ContactData
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContactData extends EntityPathBase<ContactData> {

    private static final long serialVersionUID = 1920660038L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContactData contactData = new QContactData("contactData");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath email = createString("email");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath fax = createString("fax");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath mobile = createString("mobile");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath telephone = createString("telephone");

    public QContactData(String variable) {
        this(ContactData.class, forVariable(variable), INITS);
    }

    public QContactData(Path<? extends ContactData> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContactData(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContactData(PathMetadata<?> metadata, PathInits inits) {
        this(ContactData.class, metadata, inits);
    }

    public QContactData(Class<? extends ContactData> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

