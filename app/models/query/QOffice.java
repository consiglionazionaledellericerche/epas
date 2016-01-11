package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Office;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QOffice is a Querydsl query type for Office
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QOffice extends EntityPathBase<Office> {

    private static final long serialVersionUID = -1289700640L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOffice office = new QOffice("office");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath address = createString("address");

    public final ListPath<models.BadgeReader, QBadgeReader> badgeReaders = this.<models.BadgeReader, QBadgeReader>createList("badgeReaders", models.BadgeReader.class, QBadgeReader.class, PathInits.DIRECT2);

    public final ListPath<models.BadgeSystem, QBadgeSystem> badgeSystems = this.<models.BadgeSystem, QBadgeSystem>createList("badgeSystems", models.BadgeSystem.class, QBadgeSystem.class, PathInits.DIRECT2);

    public final StringPath code = createString("code");

    public final StringPath codeId = createString("codeId");

    public final ListPath<models.ConfGeneral, QConfGeneral> confGeneral = this.<models.ConfGeneral, QConfGeneral>createList("confGeneral", models.ConfGeneral.class, QConfGeneral.class, PathInits.DIRECT2);

    public final ListPath<models.ConfYear, QConfYear> confYear = this.<models.ConfYear, QConfYear>createList("confYear", models.ConfYear.class, QConfYear.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath headQuarter = createBoolean("headQuarter");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QInstitute institute;

    public final DatePath<org.joda.time.LocalDate> joiningDate = createDate("joiningDate", org.joda.time.LocalDate.class);

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final ListPath<models.TotalOvertime, QTotalOvertime> totalOvertimes = this.<models.TotalOvertime, QTotalOvertime>createList("totalOvertimes", models.TotalOvertime.class, QTotalOvertime.class, PathInits.DIRECT2);

    public final ListPath<models.UsersRolesOffices, QUsersRolesOffices> usersRolesOffices = this.<models.UsersRolesOffices, QUsersRolesOffices>createList("usersRolesOffices", models.UsersRolesOffices.class, QUsersRolesOffices.class, PathInits.DIRECT2);

    public final ListPath<models.WorkingTimeType, QWorkingTimeType> workingTimeType = this.<models.WorkingTimeType, QWorkingTimeType>createList("workingTimeType", models.WorkingTimeType.class, QWorkingTimeType.class, PathInits.DIRECT2);

    public QOffice(String variable) {
        this(Office.class, forVariable(variable), INITS);
    }

    public QOffice(Path<? extends Office> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QOffice(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QOffice(PathMetadata<?> metadata, PathInits inits) {
        this(Office.class, metadata, inits);
    }

    public QOffice(Class<? extends Office> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.institute = inits.isInitialized("institute") ? new QInstitute(forProperty("institute")) : null;
    }

}

