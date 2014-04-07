package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ShiftTimeTable;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QShiftTimeTable is a Querydsl query type for ShiftTimeTable
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QShiftTimeTable extends EntityPathBase<ShiftTimeTable> {

    private static final long serialVersionUID = -144019773L;

    public static final QShiftTimeTable shiftTimeTable = new QShiftTimeTable("shiftTimeTable");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath description = createString("description");

    public final DateTimePath<org.joda.time.LocalDateTime> endShift = createDateTime("endShift", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonShiftDay, QPersonShiftDay> personShiftDay = this.<models.PersonShiftDay, QPersonShiftDay>createList("personShiftDay", models.PersonShiftDay.class, QPersonShiftDay.class, PathInits.DIRECT2);

    public final DateTimePath<org.joda.time.LocalDateTime> startShift = createDateTime("startShift", org.joda.time.LocalDateTime.class);

    public QShiftTimeTable(String variable) {
        super(ShiftTimeTable.class, forVariable(variable));
    }

    public QShiftTimeTable(Path<? extends ShiftTimeTable> path) {
        super(path.getType(), path.getMetadata());
    }

    public QShiftTimeTable(PathMetadata<?> metadata) {
        super(ShiftTimeTable.class, metadata);
    }

}

