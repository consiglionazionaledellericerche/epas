package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.MealTicketCard;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMealTicketCard is a Querydsl query type for MealTicketCard
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QMealTicketCard extends EntityPathBase<MealTicketCard> {

    private static final long serialVersionUID = -376544125L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMealTicketCard mealTicketCard = new QMealTicketCard("mealTicketCard");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<models.MealTicket, QMealTicket> mealTickets = this.<models.MealTicket, QMealTicket>createList("mealTickets", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QMealTicketCard(String variable) {
        this(MealTicketCard.class, forVariable(variable), INITS);
    }

    public QMealTicketCard(Path<? extends MealTicketCard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMealTicketCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMealTicketCard(PathMetadata metadata, PathInits inits) {
        this(MealTicketCard.class, metadata, inits);
    }

    public QMealTicketCard(Class<? extends MealTicketCard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

