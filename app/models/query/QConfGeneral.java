package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ConfGeneral;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QConfGeneral is a Querydsl query type for ConfGeneral
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QConfGeneral extends EntityPathBase<ConfGeneral> {

    private static final long serialVersionUID = 1743575936L;

    public static final QConfGeneral confGeneral = new QConfGeneral("confGeneral");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final NumberPath<Integer> dayOfPatron = createNumber("dayOfPatron", Integer.class);

    public final StringPath emailToContact = createString("emailToContact");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DatePath<org.joda.time.LocalDate> initUseProgram = createDate("initUseProgram", org.joda.time.LocalDate.class);

    public final StringPath instituteName = createString("instituteName");

    public final NumberPath<Integer> mealTimeEndHour = createNumber("mealTimeEndHour", Integer.class);

    public final NumberPath<Integer> mealTimeEndMinute = createNumber("mealTimeEndMinute", Integer.class);

    public final NumberPath<Integer> mealTimeStartHour = createNumber("mealTimeStartHour", Integer.class);

    public final NumberPath<Integer> mealTimeStartMinute = createNumber("mealTimeStartMinute", Integer.class);

    public final NumberPath<Integer> monthOfPatron = createNumber("monthOfPatron", Integer.class);

    public final NumberPath<Integer> numberOfViewingCoupleColumn = createNumber("numberOfViewingCoupleColumn", Integer.class);

    public final StringPath passwordToPresence = createString("passwordToPresence");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> seatCode = createNumber("seatCode", Integer.class);

    public final StringPath urlToPresence = createString("urlToPresence");

    public final StringPath userToPresence = createString("userToPresence");

    public final BooleanPath webStampingAllowed = createBoolean("webStampingAllowed");

    public QConfGeneral(String variable) {
        super(ConfGeneral.class, forVariable(variable));
    }

    public QConfGeneral(Path<? extends ConfGeneral> path) {
        super(path.getType(), path.getMetadata());
    }

    public QConfGeneral(PathMetadata<?> metadata) {
        super(ConfGeneral.class, metadata);
    }

}

