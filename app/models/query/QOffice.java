package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.Office;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QOffice is a Querydsl query type for Office
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QOffice extends EntityPathBase<Office> {

    private static final long serialVersionUID = -1289700640L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOffice office1 = new QOffice("office1");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath address = createString("address");

    public final NumberPath<Integer> code = createNumber("code", Integer.class);

    public final ListPath<models.ConfGeneral, QConfGeneral> confGeneral = this.<models.ConfGeneral, QConfGeneral>createList("confGeneral", models.ConfGeneral.class, QConfGeneral.class, PathInits.DIRECT2);

    public final ListPath<models.ConfYear, QConfYear> confYear = this.<models.ConfYear, QConfYear>createList("confYear", models.ConfYear.class, QConfYear.class, PathInits.DIRECT2);

    public final StringPath contraction = createString("contraction");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DatePath<org.joda.time.LocalDate> joiningDate = createDate("joiningDate", org.joda.time.LocalDate.class);

    public final StringPath label = createString("label");

    public final StringPath name = createString("name");

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final ListPath<Office, QOffice> subOffices = this.<Office, QOffice>createList("subOffices", Office.class, QOffice.class, PathInits.DIRECT2);

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
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

