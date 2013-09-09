
    create table absence_type_groups (
        id int8 not null,
        accumulationBehaviour varchar(255),
        accumulation_type varchar(255),
        label varchar(255),
        limit_in_minute int4,
        minutes_excess bool,
        replacing_absence_type_id int8,
        primary key (id)
    );

    create table absence_types (
        id int8 not null,
        certification_code varchar(255),
        code varchar(255),
        compensatory_rest bool,
        description varchar(255),
        ignore_stamping bool,
        internal_use bool,
        justified_time_at_work varchar(255),
        meal_ticket_calculation bool,
        multiple_use bool,
        replacing_absence bool,
        valid_from date,
        valid_to date,
        absence_type_group_id int8,
        primary key (id)
    );

    create table absence_types_qualifications (
        absenceTypes_id int8 not null,
        qualifications_id int8 not null
    );

    create table absences (
        id int8 not null,
        absenceRequest varchar(255),
        absence_type_id int8,
        personDay_id int8 not null,
        primary key (id)
    );

    create table auth_users (
        id int8 not null,
        authIp varchar(255),
        authmod varchar(255),
        authred varchar(255),
        autsys varchar(255),
        dataCpas timestamp,
        password varchar(255),
        passwordMD5 varchar(255),
        scadenzaPassword int2,
        ultimaModifica timestamp,
        username varchar(255),
        primary key (id)
    );

    create table badge_readers (
        id int8 not null,
        code varchar(255),
        description varchar(255),
        enabled bool not null,
        location varchar(255),
        primary key (id)
    );

    create table competence_codes (
        id int8 not null,
        code varchar(255),
        codeToPresence varchar(255),
        description varchar(255),
        inactive bool not null,
        primary key (id)
    );

    create table competences (
        id int8 not null,
        month int4 not null,
        reason varchar(255),
        valueApproved int4 not null,
        valueRequest int4 not null,
        year int4 not null,
        competence_code_id int8 not null,
        person_id int8,
        primary key (id)
    );

    create table configurations (
        id int8 not null,
        addWorkingTimeInExcess bool not null,
        begin_date timestamp,
        calculateIntervalTimeWithoutReturnFromIntevalTime bool not null,
        canInsertMoreAbsenceCodeInDay bool not null,
        canPeopleAutoDeclareAbsences bool not null,
        canPeopleAutoDeclareWorkingTime bool not null,
        canPeopleUseWebStamping bool not null,
        capacityFourEight int4,
        capacityOneThree int4,
        dayExpiryVacationPastYear int4,
        day_of_patron int4,
        email_to_contact varchar(255),
        end_date timestamp,
        holydaysAndVacationsOverPermitted bool not null,
        hourMaxToCalculateWorkTime int4,
        ignoreWorkingTimeWithAbsenceCode bool not null,
        in_use bool,
        init_use_program timestamp,
        insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime bool not null,
        institute_name varchar(255),
        isFirstOrLastMissionDayAHoliday bool not null,
        isHolidayInMissionAWorkingDay bool not null,
        isIntervalTimeCutFromWorkingTime bool not null,
        isLastDayBeforeEasterEntire bool not null,
        isLastDayBeforeXmasEntire bool not null,
        isLastDayOfTheYearEntire bool not null,
        isMealTimeShorterThanMinimum bool not null,
        maxRecoveryDaysFourNine int4,
        maxRecoveryDaysOneThree int4,
        maximumOvertimeHours int4,
        mealTicketAssignedWithMealTimeReal bool not null,
        mealTicketAssignedWithReasonMealTime bool not null,
        mealTime int4,
        minimumRemainingTimeToHaveRecoveryDay int4,
        monthExpireRecoveryDaysFourNine int4,
        monthExpireRecoveryDaysOneThree int4,
        monthExpiryVacationPastYear int4,
        month_of_patron int4,
        numberOfViewingCoupleColumn int4 not null,
        password_to_presence varchar(255),
        path_to_save_presence_situation varchar(255),
        residual int4,
        seat_code int4,
        textForMonthlySituation varchar(255),
        url_to_presence varchar(255),
        user_to_presence varchar(255),
        workingTime int4,
        workingTimeToHaveMealTicket int4,
        primary key (id)
    );

    create table contact_data (
        id int8 not null,
        email varchar(255),
        fax varchar(255),
        mobile varchar(255),
        telephone varchar(255),
        person_id int8,
        primary key (id)
    );

    create table contracts (
        id int8 not null,
        begin_contract date,
        end_contract date,
        expire_contract date,
        onCertificate bool not null,
        person_id int8,
        primary key (id)
    );

    create table groups (
        id int8 not null,
        description varchar(255),
        primary key (id)
    );

    create table initialization_absences (
        id int8 not null,
        absenceDays int4,
        date date,
        recovery_days int4,
        absenceType_id int8 not null,
        person_id int8 not null,
        primary key (id)
    );

    create table initialization_times (
        id int8 not null,
        date date,
        residualMinutesCurrentYear int4,
        residualMinutesPastYear int4,
        person_id int8 not null,
        primary key (id)
    );

    create table locations (
        id int8 not null,
        department varchar(255),
        headOffice varchar(255),
        room varchar(255),
        person_id int8,
        primary key (id)
    );

    create table options (
        id int8 not null,
        EasterChristmas bool,
        adjustRange bool,
        adjustRangeDay bool,
        autoRange bool,
        date timestamp,
        expiredVacationDay bool,
        expiredVacationMonth bool,
        otherHeadOffice varchar(255),
        patronDay bool,
        patronMonth bool,
        recoveryAp varchar(255),
        recoveryMonth bool,
        tipo_ferie_gen varchar(255),
        tipo_permieg varchar(255),
        vacationType varchar(255),
        vacationTypeP varchar(255),
        primary key (id)
    );

    create table permissions (
        id int8 not null,
        description varchar(255),
        primary key (id)
    );

    create table permissions_groups (
        permissions_id int8 not null,
        groups_id int8 not null
    );

    create table person_children (
        id int8 not null,
        bornDate bytea,
        name varchar(255),
        surname varchar(255),
        person_id int8,
        primary key (id)
    );

    create table person_days (
        id int8 not null,
        date date,
        difference int4,
        is_ticket_available bool,
        is_ticket_forced_by_admin bool,
        is_time_at_work_auto_certificated bool,
        is_working_in_another_place bool,
        modification_type varchar(255),
        progressive int4,
        time_at_work int4,
        person_id int8 not null,
        primary key (id),
        unique (person_id, date)
    );

    create table person_hour_for_overtime (
        id int8 not null,
        numberOfHourForOvertime int4,
        person_id int8,
        primary key (id)
    );

    create table person_months (
        id int8 not null,
        compensatory_rest_in_minutes int4,
        month int4,
        progressiveAtEndOfMonthInMinutes int4,
        recuperi_ore_da_anno_precedente int4,
        remaining_minute_past_year_taken int4,
        residual_past_year int4,
        riposi_compensativi_da_anno_corrente int4,
        riposi_compensativi_da_anno_precedente int4,
        riposi_compensativi_da_inizializzazione int4,
        straordinari int4,
        total_remaining_minutes int4,
        year int4,
        person_id int8 not null,
        primary key (id)
    );

    create table person_reperibility (
        id int8 not null,
        end_date date,
        note varchar(255),
        start_date date,
        person_id int8 unique,
        person_reperibility_type_id int8,
        primary key (id)
    );

    create table person_reperibility_days (
        id int8 not null,
        date date,
        holiday_day bool,
        person_reperibility_id int8 not null,
        reperibility_type int8,
        primary key (id),
        unique (person_reperibility_id, date)
    );

    create table person_reperibility_types (
        id int8 not null,
        description varchar(255),
        primary key (id)
    );

    create table person_shift (
        id int8 not null,
        description varchar(255),
        jolly bool not null,
        person_id int8 not null unique,
        primary key (id)
    );

    create table person_shift_days (
        id int8 not null,
        date date,
        person_shift_id int8 not null,
        shift_time_table_id int8,
        shift_type_id int8,
        primary key (id)
    );

    create table person_shift_shift_type (
        id int8 not null,
        begin_date date,
        end_date date,
        personshifts_id int8,
        shifttypes_id int8,
        primary key (id)
    );

    create table person_years (
        id int8 not null,
        remaining_minutes int4,
        remaining_vacation_days int4,
        year int4,
        person_id int8 not null,
        primary key (id)
    );

    create table persons (
        id int8 not null,
        badgeNumber varchar(255),
        born_date timestamp,
        email varchar(255),
        name varchar(255),
        number int4,
        oldId int8,
        other_surnames varchar(255),
        password varchar(255),
        surname varchar(255),
        username varchar(255),
        version int4,
        qualification_id int8,
        working_time_type_id int8,
        primary key (id)
    );

    create table persons_competence_codes (
        persons_id int8 not null,
        competenceCode_id int8 not null
    );

    create table persons_groups (
        persons_id int8 not null,
        groups_id int8 not null
    );

    create table persons_permissions (
        users_id int8 not null,
        permissions_id int8 not null
    );

    create table qualifications (
        id int8 not null,
        description varchar(255),
        qualification int4 not null,
        primary key (id)
    );

    create table shift_cancelled (
        id int8 not null,
        date date,
        shift_type_id int8 not null,
        primary key (id)
    );

    create table shift_time_table (
        id int8 not null,
        description varchar(255),
        endShift timestamp,
        startShift timestamp,
        primary key (id)
    );

    create table shift_type (
        id int8 not null,
        description varchar(255),
        type varchar(255),
        primary key (id)
    );

    create table stamp_modification_types (
        id int8 not null,
        code varchar(255),
        description varchar(255),
        primary key (id)
    );

    create table stamp_profiles (
        id int8 not null,
        end_to date,
        fixedWorkingTime bool not null,
        start_from date,
        person_id int8 not null,
        primary key (id)
    );

    create table stamp_types (
        id int8 not null,
        code varchar(255),
        description varchar(255),
        identifier varchar(255),
        primary key (id)
    );

    create table stampings (
        id int8 not null,
        date timestamp,
        marked_by_admin bool,
        note varchar(255),
        way varchar(255),
        badge_reader_id int8,
        personDay_id int8 not null,
        stamp_modification_type_id int8,
        stamp_type_id int8,
        primary key (id)
    );

    create table total_overtime (
        id int8 not null,
        date date,
        numberOfHours int4,
        year int4,
        primary key (id)
    );

    create table vacation_codes (
        id int8 not null,
        description varchar(255),
        permission_days int4,
        vacation_days int4,
        primary key (id)
    );

    create table vacation_periods (
        id int8 not null,
        begin_from date,
        end_to date,
        person_id int8 not null unique,
        vacation_codes_id int8 not null,
        primary key (id)
    );

    create table valuable_competences (
        id int8 not null,
        codicecomp varchar(255),
        descrizione varchar(255),
        person_id int8,
        primary key (id)
    );

    create table web_stamping_address (
        id int8 not null,
        webAddressType varchar(255),
        confParameters_id int8,
        primary key (id)
    );

    create table working_time_type_days (
        id int8 not null,
        breakTicketTime int4,
        dayOfWeek int4 not null,
        holiday bool not null,
        mealTicketTime int4,
        timeMealFrom int4,
        timeMealTo int4,
        timeSlotEntranceFrom int4,
        timeSlotEntranceTo int4,
        timeSlotExitFrom int4,
        timeSlotExitTo int4,
        workingTime int4,
        working_time_type_id int8 not null,
        primary key (id)
    );

    create table working_time_types (
        id int8 not null,
        description varchar(255) not null,
        shift bool not null,
        primary key (id)
    );

    create table year_recaps (
        id int8 not null,
        lastModified timestamp,
        overtime int4,
        overtimeAp int4,
        recg int4,
        recgap int4,
        recguap int4,
        recm int4,
        remaining int4,
        remainingAp int4,
        year int2,
        person_id int8,
        primary key (id)
    );

    alter table absence_type_groups 
        add constraint FK71B9FF7730325BE3 
        foreign key (replacing_absence_type_id) 
        references absence_types;

    alter table absence_types 
        add constraint FKFE65DBF7CA0A1C8A 
        foreign key (absence_type_group_id) 
        references absence_type_groups;

    alter table absence_types_qualifications 
        add constraint FK16765ACBCE1B821 
        foreign key (qualifications_id) 
        references qualifications;

    alter table absence_types_qualifications 
        add constraint FK16765ACD966A951 
        foreign key (absenceTypes_id) 
        references absence_types;

    alter table absences 
        add constraint FK6674C5D6FBE89596 
        foreign key (personDay_id) 
        references person_days;

    alter table absences 
        add constraint FK6674C5D65B5F15B1 
        foreign key (absence_type_id) 
        references absence_types;

    alter table competences 
        add constraint FKBE7A61CA62728BB1 
        foreign key (competence_code_id) 
        references competence_codes;

    alter table competences 
        add constraint FKBE7A61CAE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table contact_data 
        add constraint FK4C241869E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table contracts 
        add constraint FKE86D11A1E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table initialization_absences 
        add constraint FK53EE3295E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table initialization_absences 
        add constraint FK53EE32958C3D68D6 
        foreign key (absenceType_id) 
        references absence_types;

    alter table initialization_times 
        add constraint FK4094EAE7E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table locations 
        add constraint FKB8A4575EE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table permissions_groups 
        add constraint FK7ABB85EF522EBD41 
        foreign key (groups_id) 
        references groups;

    alter table permissions_groups 
        add constraint FK7ABB85EF34428EA9 
        foreign key (permissions_id) 
        references permissions;

    alter table person_children 
        add constraint FK6ACE3A9E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_days 
        add constraint FKE69ADB01E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_hour_for_overtime 
        add constraint FK20975C68E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_months 
        add constraint FKBB6C161DE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_reperibility 
        add constraint FK7AB49E924E498A6E 
        foreign key (person_reperibility_type_id) 
        references person_reperibility_types;

    alter table person_reperibility 
        add constraint FK7AB49E92E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_reperibility_days 
        add constraint FKB20B55E47D5FD20C 
        foreign key (reperibility_type) 
        references person_reperibility_types;

    alter table person_reperibility_days 
        add constraint FKB20B55E41DF6DE9 
        foreign key (person_reperibility_id) 
        references person_reperibility;

    alter table person_shift 
        add constraint FKED96D718E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_shift_days 
        add constraint FK3075811E86585224 
        foreign key (shift_time_table_id) 
        references shift_time_table;

    alter table person_shift_days 
        add constraint FK3075811EDA784C2B 
        foreign key (person_shift_id) 
        references person_shift;

    alter table person_shift_days 
        add constraint FK3075811E3DF511BB 
        foreign key (shift_type_id) 
        references shift_type;

    alter table person_shift_shift_type 
        add constraint FK7757FC5EBBFCA55B 
        foreign key (shifttypes_id) 
        references shift_type;

    alter table person_shift_shift_type 
        add constraint FK7757FC5E29B090BD 
        foreign key (personshifts_id) 
        references person_shift;

    alter table person_years 
        add constraint FKEDE9EA6CE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table persons 
        add constraint FKD78FCFBE786A4AB6 
        foreign key (qualification_id) 
        references qualifications;

    alter table persons 
        add constraint FKD78FCFBE35555570 
        foreign key (working_time_type_id) 
        references working_time_types;

    alter table persons_competence_codes 
        add constraint FK218A9691DD4EB8B5 
        foreign key (persons_id) 
        references persons;

    alter table persons_competence_codes 
        add constraint FK218A96913C1EA3E 
        foreign key (competenceCode_id) 
        references competence_codes;

    alter table persons_groups 
        add constraint FKDDA67575522EBD41 
        foreign key (groups_id) 
        references groups;

    alter table persons_groups 
        add constraint FKDDA67575DD4EB8B5 
        foreign key (persons_id) 
        references persons;

    alter table persons_permissions 
        add constraint FKF0F7583A4F8DE2B 
        foreign key (users_id) 
        references persons;

    alter table persons_permissions 
        add constraint FKF0F758334428EA9 
        foreign key (permissions_id) 
        references permissions;

    alter table shift_cancelled 
        add constraint FK3F7A55D43DF511BB 
        foreign key (shift_type_id) 
        references shift_type;

    alter table stamp_profiles 
        add constraint FK8379A4E6E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table stampings 
        add constraint FK785E8F14FBE89596 
        foreign key (personDay_id) 
        references person_days;

    alter table stampings 
        add constraint FK785E8F148868391D 
        foreign key (badge_reader_id) 
        references badge_readers;

    alter table stampings 
        add constraint FK785E8F1435175BCE 
        foreign key (stamp_modification_type_id) 
        references stamp_modification_types;

    alter table stampings 
        add constraint FK785E8F14932966BD 
        foreign key (stamp_type_id) 
        references stamp_types;

    alter table vacation_periods 
        add constraint FK5170DF704649FE84 
        foreign key (vacation_codes_id) 
        references vacation_codes;

    alter table vacation_periods 
        add constraint FK5170DF70E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table valuable_competences 
        add constraint FK5842D3D9E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table web_stamping_address 
        add constraint FK84DF567FAC97433E 
        foreign key (confParameters_id) 
        references configurations;

    alter table working_time_type_days 
        add constraint FK74FDDA1835555570 
        foreign key (working_time_type_id) 
        references working_time_types;

    alter table year_recaps 
        add constraint FK9AFC5DD6E7A7B1BE 
        foreign key (person_id) 
        references persons;

    create sequence seq_absence_type_groups;

    create sequence seq_absence_types;

    create sequence seq_absences;

    create sequence seq_auth_users;

    create sequence seq_badge_readers;

    create sequence seq_competence_codes;

    create sequence seq_competences;

    create sequence seq_configurations;

    create sequence seq_contact_data;

    create sequence seq_contracts;

    create sequence seq_groups;

    create sequence seq_initialization_absences;

    create sequence seq_initialization_times;

    create sequence seq_locations;

    create sequence seq_options;

    create sequence seq_permissions;

    create sequence seq_person_children;

    create sequence seq_person_days;

    create sequence seq_person_hour_for_overtime;

    create sequence seq_person_months;

    create sequence seq_person_reperibility;

    create sequence seq_person_reperibility_days;

    create sequence seq_person_reperibility_types;

    create sequence seq_person_shift;

    create sequence seq_person_shift_days;

    create sequence seq_person_shift_shift_type;

    create sequence seq_person_years;

    create sequence seq_persons;

    create sequence seq_qualifications;

    create sequence seq_shift_cancelled;

    create sequence seq_shift_time_table;

    create sequence seq_shift_type;

    create sequence seq_stamp_modification_types;

    create sequence seq_stamp_profiles;

    create sequence seq_stamp_types;

    create sequence seq_stampings;

    create sequence seq_total_overtime;

    create sequence seq_vacation_codes;

    create sequence seq_vacation_periods;

    create sequence seq_valuable_competences;

    create sequence seq_web_stamping_address;

    create sequence seq_working_time_type_days;

    create sequence seq_working_time_types;

    create sequence seq_year_recaps;
