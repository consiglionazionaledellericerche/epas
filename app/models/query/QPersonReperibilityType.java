package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonReperibilityType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonReperibilityType is a Querydsl query type for PersonReperibilityType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonReperibilityType extends EntityPathBase<PersonReperibilityType> {

    private static final long serialVersionUID = -1571481893L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonReperibilityType personReperibilityType = new QPersonReperibilityType("personReperibilityType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<models.Person, QPerson> managers = this.<models.Person, QPerson>createList("managers", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final QMonthlyCompetenceType monthlyCompetenceType;

    public final SetPath<models.ReperibilityTypeMonth, QReperibilityTypeMonth> monthsStatus = this.<models.ReperibilityTypeMonth, QReperibilityTypeMonth>createSet("monthsStatus", models.ReperibilityTypeMonth.class, QReperibilityTypeMonth.class, PathInits.DIRECT2);

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonReperibility, QPersonReperibility> personReperibilities = this.<models.PersonReperibility, QPersonReperibility>createList("personReperibilities", models.PersonReperibility.class, QPersonReperibility.class, PathInits.DIRECT2);

    public final QPerson supervisor;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonReperibilityType(String variable) {
        this(PersonReperibilityType.class, forVariable(variable), INITS);
    }

    public QPersonReperibilityType(Path<? extends PersonReperibilityType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonReperibilityType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonReperibilityType(PathMetadata metadata, PathInits inits) {
        this(PersonReperibilityType.class, metadata, inits);
    }

    public QPersonReperibilityType(Class<? extends PersonReperibilityType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.monthlyCompetenceType = inits.isInitialized("monthlyCompetenceType") ? new QMonthlyCompetenceType(forProperty("monthlyCompetenceType"), inits.get("monthlyCompetenceType")) : null;
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.supervisor = inits.isInitialized("supervisor") ? new QPerson(forProperty("supervisor"), inits.get("supervisor")) : null;
    }

}

