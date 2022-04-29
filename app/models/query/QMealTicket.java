package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.MealTicket;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMealTicket is a Querydsl query type for MealTicket
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QMealTicket extends EntityPathBase<MealTicket> {

    private static final long serialVersionUID = 1842091731L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMealTicket mealTicket = new QMealTicket("mealTicket");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QPerson admin;

    public final StringPath block = createString("block");

    public final EnumPath<models.enumerate.BlockType> blockType = createEnum("blockType", models.enumerate.BlockType.class);

    public final StringPath code = createString("code");

    public final QContract contract;

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> expireDate = createDate("expireDate", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath returned = createBoolean("returned");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QMealTicket(String variable) {
        this(MealTicket.class, forVariable(variable), INITS);
    }

    public QMealTicket(Path<? extends MealTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMealTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMealTicket(PathMetadata metadata, PathInits inits) {
        this(MealTicket.class, metadata, inits);
    }

    public QMealTicket(Class<? extends MealTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new QPerson(forProperty("admin"), inits.get("admin")) : null;
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

