package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Office;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOffice is a Querydsl query type for Office
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QOffice extends EntityPathBase<Office> {

    private static final long serialVersionUID = -1289700640L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOffice office = new QOffice("office");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    public final StringPath address = createString("address");

    public final ListPath<models.Attachment, QAttachment> attachments = this.<models.Attachment, QAttachment>createList("attachments", models.Attachment.class, QAttachment.class, PathInits.DIRECT2);

    public final ListPath<models.BadgeSystem, QBadgeSystem> badgeSystems = this.<models.BadgeSystem, QBadgeSystem>createList("badgeSystems", models.BadgeSystem.class, QBadgeSystem.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final StringPath code = createString("code");

    public final StringPath codeId = createString("codeId");

    public final ListPath<models.Configuration, QConfiguration> configurations = this.<models.Configuration, QConfiguration>createList("configurations", models.Configuration.class, QConfiguration.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.flows.Group, models.flows.query.QGroup> groups = this.<models.flows.Group, models.flows.query.QGroup>createList("groups", models.flows.Group.class, models.flows.query.QGroup.class, PathInits.DIRECT2);

    public final BooleanPath headQuarter = createBoolean("headQuarter");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QInstitute institute;

    public final DatePath<org.joda.time.LocalDate> joiningDate = createDate("joiningDate", org.joda.time.LocalDate.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> perseoId = createNumber("perseoId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonReperibilityType, QPersonReperibilityType> personReperibilityTypes = this.<models.PersonReperibilityType, QPersonReperibilityType>createList("personReperibilityTypes", models.PersonReperibilityType.class, QPersonReperibilityType.class, PathInits.DIRECT2);

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftCategories, QShiftCategories> shiftCategories = this.<models.ShiftCategories, QShiftCategories>createList("shiftCategories", models.ShiftCategories.class, QShiftCategories.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftTimeTable, QShiftTimeTable> shiftTimeTable = this.<models.ShiftTimeTable, QShiftTimeTable>createList("shiftTimeTable", models.ShiftTimeTable.class, QShiftTimeTable.class, PathInits.DIRECT2);

    public final ListPath<models.MealTicket, QMealTicket> tickets = this.<models.MealTicket, QMealTicket>createList("tickets", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final ListPath<models.TimeSlot, QTimeSlot> timeSlots = this.<models.TimeSlot, QTimeSlot>createList("timeSlots", models.TimeSlot.class, QTimeSlot.class, PathInits.DIRECT2);

    public final ListPath<models.TotalOvertime, QTotalOvertime> totalOvertimes = this.<models.TotalOvertime, QTotalOvertime>createList("totalOvertimes", models.TotalOvertime.class, QTotalOvertime.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final ListPath<models.User, QUser> users = this.<models.User, QUser>createList("users", models.User.class, QUser.class, PathInits.DIRECT2);

    public final ListPath<models.UsersRolesOffices, QUsersRolesOffices> usersRolesOffices = this.<models.UsersRolesOffices, QUsersRolesOffices>createList("usersRolesOffices", models.UsersRolesOffices.class, QUsersRolesOffices.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final ListPath<models.WorkingTimeType, QWorkingTimeType> workingTimeType = this.<models.WorkingTimeType, QWorkingTimeType>createList("workingTimeType", models.WorkingTimeType.class, QWorkingTimeType.class, PathInits.DIRECT2);

    public QOffice(String variable) {
        this(Office.class, forVariable(variable), INITS);
    }

    public QOffice(Path<? extends Office> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOffice(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOffice(PathMetadata metadata, PathInits inits) {
        this(Office.class, metadata, inits);
    }

    public QOffice(Class<? extends Office> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.institute = inits.isInitialized("institute") ? new QInstitute(forProperty("institute")) : null;
    }

}

