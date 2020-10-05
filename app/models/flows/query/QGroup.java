package models.flows.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.flows.Group;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroup is a Querydsl query type for Group
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QGroup extends EntityPathBase<Group> {

    private static final long serialVersionUID = 405099186L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroup group = new QGroup("group1");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final BooleanPath active = createBoolean("active");

    public final ListPath<models.flows.Affiliation, QAffiliation> affiliations = this.<models.flows.Affiliation, QAffiliation>createList("affiliations", models.flows.Affiliation.class, QAffiliation.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath label = createString("label");

    public final models.query.QPerson manager;

    public final StringPath name = createString("name");

    public final models.query.QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath sendFlowsEmail = createBoolean("sendFlowsEmail");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QGroup(String variable) {
        this(Group.class, forVariable(variable), INITS);
    }

    public QGroup(Path<? extends Group> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroup(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroup(PathMetadata metadata, PathInits inits) {
        this(Group.class, metadata, inits);
    }

    public QGroup(Class<? extends Group> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.manager = inits.isInitialized("manager") ? new models.query.QPerson(forProperty("manager"), inits.get("manager")) : null;
        this.office = inits.isInitialized("office") ? new models.query.QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

