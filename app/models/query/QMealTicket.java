package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.MealTicket;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QMealTicket is a Querydsl query type for MealTicket
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QMealTicket extends EntityPathBase<MealTicket> {

    private static final long serialVersionUID = 1842091731L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMealTicket mealTicket = new QMealTicket("mealTicket");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QPerson admin;

    public final NumberPath<Integer> block = createNumber("block", Integer.class);

    public final StringPath code = createString("code");

    public final QContract contract;

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> expireDate = createDate("expireDate", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> quarter = createNumber("quarter", Integer.class);

    public final BooleanPath returned = createBoolean("returned");

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QMealTicket(String variable) {
        this(MealTicket.class, forVariable(variable), INITS);
    }

    public QMealTicket(Path<? extends MealTicket> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QMealTicket(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QMealTicket(PathMetadata<?> metadata, PathInits inits) {
        this(MealTicket.class, metadata, inits);
    }

    public QMealTicket(Class<? extends MealTicket> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new QPerson(forProperty("admin"), inits.get("admin")) : null;
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

