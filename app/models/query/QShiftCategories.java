package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ShiftCategories;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShiftCategories is a Querydsl query type for ShiftCategories
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShiftCategories extends EntityPathBase<ShiftCategories> {

    private static final long serialVersionUID = 322977946L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftCategories shiftCategories = new QShiftCategories("shiftCategories");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<models.Person, QPerson> managers = this.<models.Person, QPerson>createList("managers", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.ShiftType, QShiftType> shiftTypes = this.<models.ShiftType, QShiftType>createList("shiftTypes", models.ShiftType.class, QShiftType.class, PathInits.DIRECT2);

    public final QPerson supervisor;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QShiftCategories(String variable) {
        this(ShiftCategories.class, forVariable(variable), INITS);
    }

    public QShiftCategories(Path<? extends ShiftCategories> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShiftCategories(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShiftCategories(PathMetadata metadata, PathInits inits) {
        this(ShiftCategories.class, metadata, inits);
    }

    public QShiftCategories(Class<? extends ShiftCategories> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.supervisor = inits.isInitialized("supervisor") ? new QPerson(forProperty("supervisor"), inits.get("supervisor")) : null;
    }

}

