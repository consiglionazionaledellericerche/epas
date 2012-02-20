
    create table absence_type_groups (
        id int8 not null,
        buildUp int4,
        buildUpEdgeBehaviour int4,
        buildUpLimit int4,
        equivalentCode varchar(255),
        label varchar(255),
        minutesExcess bool,
        primary key (id)
    );

    create table absence_types (
        id int8 not null,
        certificateCode varchar(255),
        code varchar(255),
        description varchar(255),
        ignoreStamping bool not null,
        internalUse bool not null,
        isDailyAbsence bool not null,
        isHourlyAbsence bool not null,
        justifiedWorkTime int4 not null,
        mealTicketCalculation bool not null,
        multipleUse bool not null,
        validFrom timestamp,
        validTo timestamp,
        absenceTypeGroup_id int8,
        primary key (id)
    );

    create table absences (
        id int8 not null,
        date date,
        absenceType_id int8,
        person_id int8,
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

    create table build_up (
        id int8 not null,
        label varchar(255),
        primary key (id)
    );

    create table build_up_edge_behaviours (
        id int8 not null,
        label varchar(255),
        primary key (id)
    );

    create table codes (
        id int8 not null,
        code varchar(255),
        code_att varchar(255),
        codiceSost varchar(255),
        description varchar(255),
        descriptionValue varchar(255),
        fromDate timestamp,
        gestLim int2 not null,
        groupOf varchar(255),
        ignoreStamping bool not null,
        inactive bool not null,
        internal bool not null,
        limitOf int4 not null,
        minutesOver bool,
        qualification varchar(255),
        quantGiust int2 not null,
        quantMin bool not null,
        recoverable bool not null,
        storage int2 not null,
        tempoBuono bool not null,
        toDate timestamp,
        usoMulti bool not null,
        value int4 not null,
        primary key (id)
    );

    create table competence_codes (
        id int8 not null,
        description varchar(255),
        inactive bool not null,
        primary key (id)
    );

    create table competence_profiles (
        id int8 not null,
        certificateCode varchar(255),
        code varchar(255),
        description varchar(255),
        inactive bool not null,
        primary key (id)
    );

    create table competences (
        id int8 not null,
        code varchar(255),
        month int4 not null,
        value int4 not null,
        year int4 not null,
        competence_code_id int8,
        person_id int8,
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

    create table contract (
        id int8 not null,
        beginContract timestamp,
        endContract timestamp,
        isContinued bool not null,
        workSaturday bool not null,
        workSunday bool not null,
        person_id int8,
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

    create table month_recaps (
        id int8 not null,
        additionalHours int4 not null,
        beginWork int2 not null,
        daysWorked int2 not null,
        endNegative int4 not null,
        endRecoveries int2 not null,
        endWork int2 not null,
        extraTimeAdmin int4 not null,
        giorniLavorativiLav int2 not null,
        holidaySop int2 not null,
        justifiedAbsence int2 not null,
        lastModified timestamp,
        month int4 not null,
        nadditionalHours bool not null,
        negative int4 not null,
        overtime int4 not null,
        persistent bool not null,
        progressive varchar(255),
        recoveries int4 not null,
        recoveriesAp int2 not null,
        recoveriesG int2 not null,
        recoveriesGap int2 not null,
        remaining int4 not null,
        residualApUsed int4 not null,
        residualFine int4 not null,
        timeHourVisit int4 not null,
        vacationAc int2 not null,
        vacationAp int2 not null,
        workTime int4 not null,
        workingDays int2 not null,
        year int4 not null,
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

    create table person_vacations (
        id int8 not null,
        vacation_day timestamp,
        person_id int8 not null,
        primary key (id)
    );

    create table persons (
        id int8 not null,
        born_date timestamp,
        email varchar(255),
        name varchar(255),
        number int4,
        othersSurnames varchar(255),
        password varchar(255),
        surname varchar(255),
        username varchar(255),
        contact_data_id int8,
        location_id int8,
        vacation_period_id int8,
        workingTimeType_id int8,
        primary key (id)
    );

    create table stamp_profile_competence_profile (
        competence_profile_id int8 not null,
        stamp_profile_id int8 not null,
        primary key (competence_profile_id, stamp_profile_id)
    );

    create table stamp_profiles (
        id int8 not null,
        fixedWorkTime bool,
        onCertificate bool not null,
        person_id int8 not null,
        primary key (id)
    );

    create table stamp_types (
        id int8 not null,
        description varchar(255),
        primary key (id)
    );

    create table stampings (
        id int8 not null,
        date timestamp,
        isMarkedByAdmin bool not null,
        isServiceExit bool not null,
        notes varchar(255),
        way varchar(255),
        person_id int8 not null,
        stamp_type_id int8,
        primary key (id)
    );

    create table vacation_codes (
        id int8 not null,
        description varchar(255),
        permissionDays int4 not null,
        vacationDays int4 not null,
        primary key (id)
    );

    create table vacation_periods (
        id int8 not null,
        beginFrom timestamp,
        endsTo timestamp,
        person_id int8,
        vacation_codes_id int8,
        primary key (id)
    );

    create table working_time_type_days (
        id int8 not null,
        breakTicketTime int4 not null,
        dayOfWeek int4 not null,
        holiday bool not null,
        mealTicketTime int4 not null,
        timeMealFrom int4 not null,
        timeMealTo int4 not null,
        timeSlotEntranceFrom int4 not null,
        timeSlotEntranceTo int4 not null,
        timeSlotExitFrom int4 not null,
        timeSlotExitTo int4 not null,
        workingTime int4 not null,
        working_time_type_id int8,
        primary key (id)
    );

    create table working_time_types (
        id int8 not null,
        description varchar(255),
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

    alter table absence_types 
        add constraint FKFE65DBF7AFBA979E 
        foreign key (absenceTypeGroup_id) 
        references absence_type_groups;

    alter table absences 
        add constraint FK6674C5D6E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table absences 
        add constraint FK6674C5D68C3D68D6 
        foreign key (absenceType_id) 
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

    alter table contract 
        add constraint FKDE351112E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table locations 
        add constraint FKB8A4575EE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table month_recaps 
        add constraint FK998E4233E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table person_vacations 
        add constraint FKA98E68CCE7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table persons 
        add constraint FKD78FCFBEEF106F7 
        foreign key (contact_data_id) 
        references contact_data;

    alter table persons 
        add constraint FKD78FCFBE2DB65CBE 
        foreign key (location_id) 
        references locations;

    alter table persons 
        add constraint FKD78FCFBE55C5BB56 
        foreign key (workingTimeType_id) 
        references working_time_types;

    alter table persons 
        add constraint FKD78FCFBE646FC899 
        foreign key (vacation_period_id) 
        references vacation_periods;

    alter table stamp_profile_competence_profile 
        add constraint FKD187C3857E90C2F7 
        foreign key (stamp_profile_id) 
        references stamp_profiles;

    alter table stamp_profile_competence_profile 
        add constraint FKD187C3856927CA3 
        foreign key (competence_profile_id) 
        references competence_profiles;

    alter table stamp_profiles 
        add constraint FK8379A4E6E7A7B1BE 
        foreign key (person_id) 
        references persons;

    alter table stampings 
        add constraint FK785E8F14E7A7B1BE 
        foreign key (person_id) 
        references persons;

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

    create sequence seq_build_up;

    create sequence seq_build_up_edge_behaviours;

    create sequence seq_codes;

    create sequence seq_competence_codes;

    create sequence seq_competence_profiles;

    create sequence seq_competences;

    create sequence seq_contact_data;

    create sequence seq_contract;

    create sequence seq_locations;

    create sequence seq_month_recaps;

    create sequence seq_options;

    create sequence seq_person_vacations;

    create sequence seq_persons;

    create sequence seq_stamp_profiles;

    create sequence seq_stamp_types;

    create sequence seq_stampings;

    create sequence seq_vacation_codes;

    create sequence seq_vacation_periods;

    create sequence seq_working_time_type_days;

    create sequence seq_working_time_types;

    create sequence seq_year_recaps;
