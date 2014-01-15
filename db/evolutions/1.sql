# --- !Ups

CREATE SEQUENCE seq_absence_type_groups
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

CREATE TABLE absence_type_groups (
    id bigint DEFAULT nextval('seq_absence_type_groups'::regclass) NOT NULL,
    accumulationbehaviour character varying(255),
    accumulation_type character varying(255),
    label character varying(255),
    limit_in_minute integer,
    minutes_excess boolean,
    replacing_absence_type_id bigint
);



CREATE TABLE absence_type_groups_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    accumulationbehaviour character varying(255),
    accumulation_type character varying(255),
    label character varying(255),
    limit_in_minute integer,
    minutes_excess boolean,
    replacing_absence_type_id bigint
);



CREATE SEQUENCE seq_absence_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE absence_types (
    id bigint DEFAULT nextval('seq_absence_types'::regclass) NOT NULL,
    certification_code character varying(255),
    code character varying(255),
    compensatory_rest boolean,
    considered_week_end boolean,
    description character varying(255),
    ignore_stamping boolean,
    internal_use boolean,
    justified_time_at_work character varying(255),
    meal_ticket_calculation boolean,
    multiple_use boolean,
    replacing_absence boolean,
    valid_from date,
    valid_to date,
    absence_type_group_id bigint
);



CREATE TABLE absence_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    certification_code character varying(255),
    code character varying(255),
    compensatory_rest boolean,
    considered_week_end boolean,
    description character varying(255),
    ignore_stamping boolean,
    internal_use boolean,
    justified_time_at_work character varying(255),
    meal_ticket_calculation boolean,
    multiple_use boolean,
    replacing_absence boolean,
    valid_from date,
    valid_to date,
    absence_type_group_id bigint
);



CREATE TABLE absence_types_qualifications (
    absencetypes_id bigint NOT NULL,
    qualifications_id bigint NOT NULL
);



CREATE TABLE absence_types_qualifications_history (
    _revision integer NOT NULL,
    absencetypes_id bigint NOT NULL,
    qualifications_id bigint NOT NULL,
    _revision_type smallint
);



CREATE SEQUENCE seq_absences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE absences (
    id bigint DEFAULT nextval('seq_absences'::regclass) NOT NULL,
    absencerequest character varying(255),
    absence_type_id bigint,
    personday_id bigint NOT NULL
);



CREATE TABLE absences_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    absencerequest character varying(255),
    absence_type_id bigint,
    personday_id bigint
);



CREATE SEQUENCE seq_auth_users
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE auth_users (
    id bigint DEFAULT nextval('seq_auth_users'::regclass) NOT NULL,
    authip character varying(255),
    authmod character varying(255),
    authred character varying(255),
    autsys character varying(255),
    datacpas timestamp without time zone,
    password character varying(255),
    passwordmd5 character varying(255),
    scadenzapassword smallint,
    ultimamodifica timestamp without time zone,
    username character varying(255)
);



CREATE SEQUENCE seq_badge_readers
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE badge_readers (
    id bigint DEFAULT nextval('seq_badge_readers'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255),
    enabled boolean NOT NULL,
    location character varying(255)
);



CREATE TABLE badge_readers_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255),
    enabled boolean,
    location character varying(255)
);



CREATE SEQUENCE seq_competence_codes
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE competence_codes (
    id bigint DEFAULT nextval('seq_competence_codes'::regclass) NOT NULL,
    code character varying(255),
    codetopresence character varying(255),
    description character varying(255),
    inactive boolean NOT NULL
);



CREATE SEQUENCE seq_competences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE competences (
    id bigint DEFAULT nextval('seq_competences'::regclass) NOT NULL,
    month integer NOT NULL,
    reason character varying(1000),
    valueapproved integer NOT NULL,
    valuerequest integer NOT NULL,
    year integer NOT NULL,
    competence_code_id bigint NOT NULL,
    person_id bigint
);



CREATE SEQUENCE seq_configurations
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE configurations (
    id bigint DEFAULT nextval('seq_configurations'::regclass) NOT NULL,
    addworkingtimeinexcess boolean NOT NULL,
    begin_date timestamp without time zone,
    calculateintervaltimewithoutreturnfromintevaltime boolean NOT NULL,
    caninsertmoreabsencecodeinday boolean NOT NULL,
    canpeopleautodeclareabsences boolean NOT NULL,
    canpeopleautodeclareworkingtime boolean NOT NULL,
    canpeopleusewebstamping boolean NOT NULL,
    capacityfoureight integer,
    capacityonethree integer,
    dayexpiryvacationpastyear integer,
    day_of_patron integer,
    email_to_contact character varying(255),
    end_date timestamp without time zone,
    holydaysandvacationsoverpermitted boolean NOT NULL,
    hourmaxtocalculateworktime integer,
    ignoreworkingtimewithabsencecode boolean NOT NULL,
    in_use boolean,
    init_use_program timestamp without time zone,
    insertandmodifyworkingtimewithplustoreduceatrealworkingtime boolean NOT NULL,
    institute_name character varying(255),
    isfirstorlastmissiondayaholiday boolean NOT NULL,
    isholidayinmissionaworkingday boolean NOT NULL,
    isintervaltimecutfromworkingtime boolean NOT NULL,
    islastdaybeforeeasterentire boolean NOT NULL,
    islastdaybeforexmasentire boolean NOT NULL,
    islastdayoftheyearentire boolean NOT NULL,
    ismealtimeshorterthanminimum boolean NOT NULL,
    maxrecoverydaysfournine integer,
    maxrecoverydaysonethree integer,
    maximumovertimehours integer,
    mealticketassignedwithmealtimereal boolean NOT NULL,
    mealticketassignedwithreasonmealtime boolean NOT NULL,
    mealtime integer,
    minimumremainingtimetohaverecoveryday integer,
    monthexpirerecoverydaysfournine integer,
    monthexpirerecoverydaysonethree integer,
    monthexpiryvacationpastyear integer,
    month_of_patron integer,
    numberofviewingcouplecolumn integer NOT NULL,
    password_to_presence character varying(255),
    path_to_save_presence_situation character varying(255),
    residual integer,
    seat_code integer,
    textformonthlysituation character varying(255),
    url_to_presence character varying(255),
    user_to_presence character varying(255),
    workingtime integer,
    workingtimetohavemealticket integer,
    mealtimestarthour integer,
    mealtimestartminute integer,
    mealtimeendminute integer,
    mealtimeendhour integer
);



CREATE TABLE configurations_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    addworkingtimeinexcess boolean,
    begin_date timestamp without time zone,
    calculateintervaltimewithoutreturnfromintevaltime boolean,
    caninsertmoreabsencecodeinday boolean,
    canpeopleautodeclareabsences boolean,
    canpeopleautodeclareworkingtime boolean,
    canpeopleusewebstamping boolean,
    capacityfoureight integer,
    capacityonethree integer,
    dayexpiryvacationpastyear integer,
    day_of_patron integer,
    email_to_contact character varying(255),
    end_date timestamp without time zone,
    holydaysandvacationsoverpermitted boolean,
    hourmaxtocalculateworktime integer,
    ignoreworkingtimewithabsencecode boolean,
    in_use boolean,
    init_use_program timestamp without time zone,
    insertandmodifyworkingtimewithplustoreduceatrealworkingtime boolean,
    institute_name character varying(255),
    isfirstorlastmissiondayaholiday boolean,
    isholidayinmissionaworkingday boolean,
    isintervaltimecutfromworkingtime boolean,
    islastdaybeforeeasterentire boolean,
    islastdaybeforexmasentire boolean,
    islastdayoftheyearentire boolean,
    ismealtimeshorterthanminimum boolean,
    maxrecoverydaysfournine integer,
    maxrecoverydaysonethree integer,
    maximumovertimehours integer,
    mealticketassignedwithmealtimereal boolean,
    mealticketassignedwithreasonmealtime boolean,
    mealtime integer,
    minimumremainingtimetohaverecoveryday integer,
    monthexpirerecoverydaysfournine integer,
    monthexpirerecoverydaysonethree integer,
    monthexpiryvacationpastyear integer,
    month_of_patron integer,
    numberofviewingcouplecolumn integer,
    password_to_presence character varying(255),
    path_to_save_presence_situation character varying(255),
    residual integer,
    seat_code integer,
    textformonthlysituation character varying(255),
    url_to_presence character varying(255),
    user_to_presence character varying(255),
    workingtime integer,
    workingtimetohavemealticket integer,
    mealtimeendhour integer,
    mealtimeendminute integer,
    mealtimestarthour integer,
    mealtimestartminute integer
);



CREATE SEQUENCE seq_contact_data
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE contact_data (
    id bigint DEFAULT nextval('seq_contact_data'::regclass) NOT NULL,
    email character varying(255),
    fax character varying(255),
    mobile character varying(255),
    telephone character varying(255),
    person_id bigint
);



CREATE TABLE contact_data_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    email character varying(255),
    fax character varying(255),
    mobile character varying(255),
    telephone character varying(255),
    person_id bigint
);



CREATE SEQUENCE seq_contracts
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE contracts (
    id bigint DEFAULT nextval('seq_contracts'::regclass) NOT NULL,
    begin_contract date,
    end_contract date,
    expire_contract date,
    oncertificate boolean NOT NULL,
    person_id bigint
);



CREATE SEQUENCE seq_groups
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE groups (
    id bigint DEFAULT nextval('seq_groups'::regclass) NOT NULL,
    description character varying(255)
);



CREATE TABLE groups_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);



CREATE SEQUENCE seq_initialization_absences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE initialization_absences (
    id bigint DEFAULT nextval('seq_initialization_absences'::regclass) NOT NULL,
    absencedays integer,
    date date,
    recovery_days integer,
    absencetype_id bigint NOT NULL,
    person_id bigint NOT NULL
);



CREATE TABLE initialization_absences_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    absencedays integer,
    date date,
    recovery_days integer,
    absencetype_id bigint,
    person_id bigint
);



CREATE SEQUENCE seq_initialization_times
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE initialization_times (
    id bigint DEFAULT nextval('seq_initialization_times'::regclass) NOT NULL,
    date date,
    residualminutescurrentyear integer,
    residualminutespastyear integer,
    person_id bigint NOT NULL
);



CREATE TABLE initialization_times_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    residualminutescurrentyear integer,
    residualminutespastyear integer,
    person_id bigint
);



CREATE SEQUENCE seq_locations
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE locations (
    id bigint DEFAULT nextval('seq_locations'::regclass) NOT NULL,
    department character varying(255),
    headoffice character varying(255),
    room character varying(255),
    person_id bigint
);



CREATE SEQUENCE seq_options
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE options (
    id bigint DEFAULT nextval('seq_options'::regclass) NOT NULL,
    easterchristmas boolean,
    adjustrange boolean,
    adjustrangeday boolean,
    autorange boolean,
    date timestamp without time zone,
    expiredvacationday boolean,
    expiredvacationmonth boolean,
    otherheadoffice character varying(255),
    patronday boolean,
    patronmonth boolean,
    recoveryap character varying(255),
    recoverymonth boolean,
    tipo_ferie_gen character varying(255),
    tipo_permieg character varying(255),
    vacationtype character varying(255),
    vacationtypep character varying(255)
);



CREATE SEQUENCE seq_permissions
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE permissions (
    id bigint DEFAULT nextval('seq_permissions'::regclass) NOT NULL,
    description character varying(255)
);



CREATE TABLE permissions_groups (
    permissions_id bigint NOT NULL,
    groups_id bigint NOT NULL
);



CREATE TABLE permissions_groups_history (
    _revision integer NOT NULL,
    permissions_id bigint NOT NULL,
    groups_id bigint NOT NULL,
    _revision_type smallint
);



CREATE TABLE permissions_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);



CREATE SEQUENCE seq_person_children
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_children (
    id bigint DEFAULT nextval('seq_person_children'::regclass) NOT NULL,
    borndate bytea,
    name character varying(255),
    surname character varying(255),
    person_id bigint
);



CREATE TABLE person_children_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    borndate bytea,
    name character varying(255),
    surname character varying(255),
    person_id bigint
);



CREATE SEQUENCE seq_person_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_days (
    id bigint DEFAULT nextval('seq_person_days'::regclass) NOT NULL,
    date date,
    difference integer,
    is_ticket_available boolean,
    is_ticket_forced_by_admin boolean,
    is_time_at_work_auto_certificated boolean,
    is_working_in_another_place boolean,
    modification_type character varying(255),
    progressive integer,
    time_at_work integer,
    person_id bigint NOT NULL
);



CREATE TABLE person_days_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    difference integer,
    is_ticket_available boolean,
    is_ticket_forced_by_admin boolean,
    is_time_at_work_auto_certificated boolean,
    is_working_in_another_place boolean,
    modification_type character varying(255),
    progressive integer,
    time_at_work integer,
    person_id bigint
);



CREATE SEQUENCE seq_person_days_in_trouble
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_days_in_trouble (
    id bigint DEFAULT nextval('seq_person_days_in_trouble'::regclass) NOT NULL,
    cause character varying(255),
    emailsent boolean NOT NULL,
    fixed boolean NOT NULL,
    personday_id bigint NOT NULL
);



CREATE TABLE person_days_in_trouble_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    cause character varying(255),
    emailsent boolean,
    fixed boolean,
    personday_id bigint
);



CREATE SEQUENCE seq_person_hour_for_overtime
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_hour_for_overtime (
    id bigint DEFAULT nextval('seq_person_hour_for_overtime'::regclass) NOT NULL,
    numberofhourforovertime integer,
    person_id bigint
);



CREATE SEQUENCE seq_person_months
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_months (
    id bigint DEFAULT nextval('seq_person_months'::regclass) NOT NULL,
    compensatory_rest_in_minutes integer,
    month integer,
    progressiveatendofmonthinminutes integer,
    recuperi_ore_da_anno_precedente integer,
    remaining_minute_past_year_taken integer,
    residual_past_year integer,
    riposi_compensativi_da_anno_corrente integer,
    riposi_compensativi_da_anno_precedente integer,
    riposi_compensativi_da_inizializzazione integer,
    straordinari integer,
    total_remaining_minutes integer,
    year integer,
    person_id bigint NOT NULL
);



CREATE TABLE person_months_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    compensatory_rest_in_minutes integer,
    month integer,
    progressiveatendofmonthinminutes integer,
    recuperi_ore_da_anno_precedente integer,
    remaining_minute_past_year_taken integer,
    residual_past_year integer,
    riposi_compensativi_da_anno_corrente integer,
    riposi_compensativi_da_anno_precedente integer,
    riposi_compensativi_da_inizializzazione integer,
    straordinari integer,
    total_remaining_minutes integer,
    year integer,
    person_id bigint
);



CREATE TABLE person_reperibility (
    id bigint NOT NULL,
    end_date date,
    note character varying(255),
    start_date date,
    person_id bigint,
    person_reperibility_type_id bigint
);



CREATE TABLE person_reperibility_days (
    id bigint NOT NULL,
    date date,
    holiday_day boolean,
    person_reperibility_id bigint NOT NULL,
    reperibility_type bigint
);



CREATE TABLE person_reperibility_days_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    holiday_day boolean,
    person_reperibility_id bigint,
    reperibility_type bigint
);



CREATE TABLE person_reperibility_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    end_date date,
    note character varying(255),
    start_date date,
    person_id bigint,
    person_reperibility_type_id bigint
);



CREATE TABLE person_reperibility_types (
    id bigint NOT NULL,
    description character varying(255)
);



CREATE TABLE person_reperibility_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);



CREATE TABLE person_shift (
    id bigint NOT NULL,
    description character varying(255),
    jolly boolean NOT NULL,
    person_id bigint NOT NULL
);



CREATE TABLE person_shift_days (
    id bigint NOT NULL,
    date date,
    person_shift_id bigint NOT NULL,
    shift_time_table_id bigint,
    shift_type_id bigint
);



CREATE TABLE person_shift_shift_type (
    id bigint NOT NULL,
    begin_date date,
    end_date date,
    personshifts_id bigint,
    shifttypes_id bigint
);



CREATE SEQUENCE seq_person_years
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE person_years (
    id bigint DEFAULT nextval('seq_person_years'::regclass) NOT NULL,
    remaining_minutes integer,
    remaining_vacation_days integer,
    year integer,
    person_id bigint NOT NULL
);



CREATE TABLE person_years_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    remaining_minutes integer,
    remaining_vacation_days integer,
    year integer,
    person_id bigint
);



CREATE SEQUENCE seq_persons
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE persons (
    id bigint DEFAULT nextval('seq_persons'::regclass) NOT NULL,
    badgenumber character varying(255),
    born_date timestamp without time zone,
    email character varying(255),
    name character varying(255),
    number integer,
    oldid bigint,
    other_surnames character varying(255),
    password character varying(255),
    surname character varying(255),
    username character varying(255),
    version integer,
    qualification_id bigint,
    working_time_type_id bigint
);



CREATE TABLE persons_competence_codes (
    persons_id bigint NOT NULL,
    competencecode_id bigint NOT NULL
);



CREATE TABLE persons_groups (
    persons_id bigint NOT NULL,
    groups_id bigint NOT NULL
);



CREATE TABLE persons_groups_history (
    _revision integer NOT NULL,
    persons_id bigint NOT NULL,
    groups_id bigint NOT NULL,
    _revision_type smallint
);



CREATE TABLE persons_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    badgenumber character varying(255),
    born_date timestamp without time zone,
    email character varying(255),
    name character varying(255),
    number integer,
    oldid bigint,
    other_surnames character varying(255),
    password character varying(255),
    surname character varying(255),
    username character varying(255),
    qualification_id bigint,
    working_time_type_id bigint
);



CREATE TABLE persons_permissions (
    users_id bigint NOT NULL,
    permissions_id bigint NOT NULL
);



CREATE TABLE persons_permissions_history (
    _revision integer NOT NULL,
    users_id bigint NOT NULL,
    permissions_id bigint NOT NULL,
    _revision_type smallint
);



CREATE SEQUENCE seq_qualifications
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE qualifications (
    id bigint DEFAULT nextval('seq_qualifications'::regclass) NOT NULL,
    description character varying(255),
    qualification integer NOT NULL
);



CREATE TABLE qualifications_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    qualification integer
);



CREATE SEQUENCE seq_revinfo
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE revinfo (
    rev integer DEFAULT nextval('seq_revinfo'::regclass) NOT NULL,
    revtstmp bigint
);



CREATE SEQUENCE seq_person_reperibility
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_person_reperibility_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_person_reperibility_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_person_shift
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_person_shift_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_person_shift_shift_type
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_shift_cancelled
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_shift_time_table
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_shift_type
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_stamp_modification_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_stamp_profiles
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_stamp_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_stampings
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_total_overtime
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_vacation_codes
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_vacation_periods
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_valuable_competences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_web_stamping_address
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_working_time_type_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_working_time_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE SEQUENCE seq_year_recaps
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE shift_cancelled (
    id bigint NOT NULL,
    date date,
    shift_type_id bigint NOT NULL
);



CREATE TABLE shift_time_table (
    id bigint NOT NULL,
    description character varying(255),
    endshift timestamp without time zone,
    startshift timestamp without time zone
);



CREATE TABLE shift_type (
    id bigint NOT NULL,
    description character varying(255),
    type character varying(255)
);



CREATE TABLE stamp_modification_types (
    id bigint DEFAULT nextval('seq_stamp_modification_types'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255)
);



CREATE TABLE stamp_modification_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255)
);



CREATE TABLE stamp_profiles (
    id bigint DEFAULT nextval('seq_stamp_profiles'::regclass) NOT NULL,
    end_to date,
    fixedworkingtime boolean NOT NULL,
    start_from date,
    person_id bigint NOT NULL
);



CREATE TABLE stamp_types (
    id bigint DEFAULT nextval('seq_stamp_types'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255),
    identifier character varying(255)
);



CREATE TABLE stamp_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255),
    identifier character varying(255)
);



CREATE TABLE stampings (
    id bigint DEFAULT nextval('seq_stampings'::regclass) NOT NULL,
    date timestamp without time zone,
    marked_by_admin boolean,
    note character varying(255),
    way character varying(255),
    badge_reader_id bigint,
    personday_id bigint NOT NULL,
    stamp_modification_type_id bigint,
    stamp_type_id bigint
);



CREATE TABLE stampings_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date timestamp without time zone,
    marked_by_admin boolean,
    note character varying(255),
    way character varying(255),
    badge_reader_id bigint,
    personday_id bigint,
    stamp_modification_type_id bigint,
    stamp_type_id bigint
);



CREATE TABLE total_overtime (
    id bigint DEFAULT nextval('seq_total_overtime'::regclass) NOT NULL,
    date date,
    numberofhours integer,
    year integer
);



CREATE TABLE vacation_codes (
    id bigint DEFAULT nextval('seq_vacation_codes'::regclass) NOT NULL,
    description character varying(255),
    permission_days integer,
    vacation_days integer
);



CREATE TABLE vacation_codes_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    permission_days integer,
    vacation_days integer
);



CREATE TABLE vacation_periods (
    id bigint DEFAULT nextval('seq_vacation_periods'::regclass) NOT NULL,
    begin_from date,
    end_to date,
    contract_id bigint NOT NULL,
    vacation_codes_id bigint NOT NULL
);



CREATE TABLE vacation_periods_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    begin_from date,
    end_to date,
    contract_id bigint,
    vacation_codes_id bigint
);



CREATE TABLE valuable_competences (
    id bigint DEFAULT nextval('seq_valuable_competences'::regclass) NOT NULL,
    codicecomp character varying(255),
    descrizione character varying(255),
    person_id bigint
);



CREATE TABLE web_stamping_address (
    id bigint DEFAULT nextval('seq_web_stamping_address'::regclass) NOT NULL,
    webaddresstype character varying(255),
    confparameters_id bigint
);



CREATE TABLE web_stamping_address_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    webaddresstype character varying(255),
    confparameters_id bigint
);



CREATE TABLE working_time_type_days (
    id bigint DEFAULT nextval('seq_working_time_type_days'::regclass) NOT NULL,
    breaktickettime integer,
    dayofweek integer NOT NULL,
    holiday boolean NOT NULL,
    mealtickettime integer,
    timemealfrom integer,
    timemealto integer,
    timeslotentrancefrom integer,
    timeslotentranceto integer,
    timeslotexitfrom integer,
    timeslotexitto integer,
    workingtime integer,
    working_time_type_id bigint NOT NULL
);



CREATE TABLE working_time_type_days_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    breaktickettime integer,
    dayofweek integer,
    holiday boolean,
    mealtickettime integer,
    timemealfrom integer,
    timemealto integer,
    timeslotentrancefrom integer,
    timeslotentranceto integer,
    timeslotexitfrom integer,
    timeslotexitto integer,
    workingtime integer,
    working_time_type_id bigint
);



CREATE TABLE working_time_types (
    id bigint DEFAULT nextval('seq_working_time_types'::regclass) NOT NULL,
    description character varying(255) NOT NULL,
    shift boolean NOT NULL
);



CREATE TABLE working_time_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    shift boolean
);



CREATE TABLE year_recaps (
    id bigint DEFAULT nextval('seq_year_recaps'::regclass) NOT NULL,
    lastmodified timestamp without time zone,
    overtime integer,
    overtimeap integer,
    recg integer,
    recgap integer,
    recguap integer,
    recm integer,
    remaining integer,
    remainingap integer,
    year smallint,
    person_id bigint
);



ALTER TABLE ONLY absence_type_groups_history
    ADD CONSTRAINT absence_type_groups_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY absence_type_groups
    ADD CONSTRAINT absence_type_groups_pkey PRIMARY KEY (id);



ALTER TABLE ONLY absence_types_history
    ADD CONSTRAINT absence_types_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY absence_types
    ADD CONSTRAINT absence_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY absence_types_qualifications_history
    ADD CONSTRAINT absence_types_qualifications_history_pkey PRIMARY KEY (_revision, absencetypes_id, qualifications_id);



ALTER TABLE ONLY absences_history
    ADD CONSTRAINT absences_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY absences
    ADD CONSTRAINT absences_pkey PRIMARY KEY (id);



ALTER TABLE ONLY auth_users
    ADD CONSTRAINT auth_users_pkey PRIMARY KEY (id);



ALTER TABLE ONLY badge_readers_history
    ADD CONSTRAINT badge_readers_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY badge_readers
    ADD CONSTRAINT badge_readers_pkey PRIMARY KEY (id);



ALTER TABLE ONLY competence_codes
    ADD CONSTRAINT competence_codes_pkey PRIMARY KEY (id);



ALTER TABLE ONLY competences
    ADD CONSTRAINT competences_pkey PRIMARY KEY (id);



ALTER TABLE ONLY configurations_history
    ADD CONSTRAINT configurations_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY configurations
    ADD CONSTRAINT configurations_pkey PRIMARY KEY (id);



ALTER TABLE ONLY contact_data_history
    ADD CONSTRAINT contact_data_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY contact_data
    ADD CONSTRAINT contact_data_pkey PRIMARY KEY (id);



ALTER TABLE ONLY contracts
    ADD CONSTRAINT contracts_pkey PRIMARY KEY (id);



ALTER TABLE ONLY groups_history
    ADD CONSTRAINT groups_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);



ALTER TABLE ONLY initialization_absences_history
    ADD CONSTRAINT initialization_absences_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT initialization_absences_pkey PRIMARY KEY (id);



ALTER TABLE ONLY initialization_times_history
    ADD CONSTRAINT initialization_times_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY initialization_times
    ADD CONSTRAINT initialization_times_pkey PRIMARY KEY (id);



ALTER TABLE ONLY locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);



ALTER TABLE ONLY options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);



ALTER TABLE ONLY permissions_groups_history
    ADD CONSTRAINT permissions_groups_history_pkey PRIMARY KEY (_revision, permissions_id, groups_id);



ALTER TABLE ONLY permissions_history
    ADD CONSTRAINT permissions_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_children_history
    ADD CONSTRAINT person_children_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_children
    ADD CONSTRAINT person_children_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_days_history
    ADD CONSTRAINT person_days_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_days_in_trouble_history
    ADD CONSTRAINT person_days_in_trouble_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_days_in_trouble
    ADD CONSTRAINT person_days_in_trouble_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_days
    ADD CONSTRAINT person_days_person_id_date_key UNIQUE (person_id, date);



ALTER TABLE ONLY person_days
    ADD CONSTRAINT person_days_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_hour_for_overtime
    ADD CONSTRAINT person_hour_for_overtime_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_months_history
    ADD CONSTRAINT person_months_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_months
    ADD CONSTRAINT person_months_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_reperibility_days_history
    ADD CONSTRAINT person_reperibility_days_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT person_reperibility_days_person_reperibility_id_date_key UNIQUE (person_reperibility_id, date);



ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT person_reperibility_days_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_reperibility_history
    ADD CONSTRAINT person_reperibility_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT person_reperibility_person_id_key UNIQUE (person_id);



ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT person_reperibility_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_reperibility_types_history
    ADD CONSTRAINT person_reperibility_types_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_reperibility_types
    ADD CONSTRAINT person_reperibility_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT person_shift_days_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_shift
    ADD CONSTRAINT person_shift_person_id_key UNIQUE (person_id);



ALTER TABLE ONLY person_shift
    ADD CONSTRAINT person_shift_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT person_shift_shift_type_pkey PRIMARY KEY (id);



ALTER TABLE ONLY person_years_history
    ADD CONSTRAINT person_years_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY person_years
    ADD CONSTRAINT person_years_pkey PRIMARY KEY (id);



ALTER TABLE ONLY persons_groups_history
    ADD CONSTRAINT persons_groups_history_pkey PRIMARY KEY (_revision, persons_id, groups_id);



ALTER TABLE ONLY persons_history
    ADD CONSTRAINT persons_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY persons_permissions_history
    ADD CONSTRAINT persons_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id);



ALTER TABLE ONLY persons
    ADD CONSTRAINT persons_pkey PRIMARY KEY (id);




ALTER TABLE ONLY qualifications_history
    ADD CONSTRAINT qualifications_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY qualifications
    ADD CONSTRAINT qualifications_pkey PRIMARY KEY (id);



ALTER TABLE ONLY revinfo
    ADD CONSTRAINT revinfo_pkey PRIMARY KEY (rev);



ALTER TABLE ONLY shift_cancelled
    ADD CONSTRAINT shift_cancelled_pkey PRIMARY KEY (id);



ALTER TABLE ONLY shift_time_table
    ADD CONSTRAINT shift_time_table_pkey PRIMARY KEY (id);



ALTER TABLE ONLY shift_type
    ADD CONSTRAINT shift_type_pkey PRIMARY KEY (id);



ALTER TABLE ONLY stamp_modification_types_history
    ADD CONSTRAINT stamp_modification_types_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY stamp_modification_types
    ADD CONSTRAINT stamp_modification_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY stamp_profiles
    ADD CONSTRAINT stamp_profiles_pkey PRIMARY KEY (id);



ALTER TABLE ONLY stamp_types_history
    ADD CONSTRAINT stamp_types_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY stamp_types
    ADD CONSTRAINT stamp_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY stampings_history
    ADD CONSTRAINT stampings_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY stampings
    ADD CONSTRAINT stampings_pkey PRIMARY KEY (id);



ALTER TABLE ONLY total_overtime
    ADD CONSTRAINT total_overtime_pkey PRIMARY KEY (id);



ALTER TABLE ONLY vacation_codes_history
    ADD CONSTRAINT vacation_codes_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY vacation_codes
    ADD CONSTRAINT vacation_codes_pkey PRIMARY KEY (id);



ALTER TABLE ONLY vacation_periods_history
    ADD CONSTRAINT vacation_periods_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT vacation_periods_pkey PRIMARY KEY (id);



ALTER TABLE ONLY valuable_competences
    ADD CONSTRAINT valuable_competences_pkey PRIMARY KEY (id);



ALTER TABLE ONLY web_stamping_address_history
    ADD CONSTRAINT web_stamping_address_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY web_stamping_address
    ADD CONSTRAINT web_stamping_address_pkey PRIMARY KEY (id);



ALTER TABLE ONLY working_time_type_days_history
    ADD CONSTRAINT working_time_type_days_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY working_time_type_days
    ADD CONSTRAINT working_time_type_days_pkey PRIMARY KEY (id);



ALTER TABLE ONLY working_time_types_history
    ADD CONSTRAINT working_time_types_history_pkey PRIMARY KEY (id, _revision);



ALTER TABLE ONLY working_time_types
    ADD CONSTRAINT working_time_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY year_recaps
    ADD CONSTRAINT year_recaps_pkey PRIMARY KEY (id);



ALTER TABLE ONLY absences_history
    ADD CONSTRAINT fk10e41b2bd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_reperibility_history
    ADD CONSTRAINT fk160d77e7d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY absence_types_qualifications
    ADD CONSTRAINT fk16765acbce1b821 FOREIGN KEY (qualifications_id) REFERENCES qualifications(id);



ALTER TABLE ONLY absence_types_qualifications
    ADD CONSTRAINT fk16765acd966a951 FOREIGN KEY (absencetypes_id) REFERENCES absence_types(id);



ALTER TABLE ONLY working_time_type_days_history
    ADD CONSTRAINT fk18bdcd6dd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY vacation_periods_history
    ADD CONSTRAINT fk1fe1fac5d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_hour_for_overtime
    ADD CONSTRAINT fk20975c68e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY persons_competence_codes
    ADD CONSTRAINT fk218a96913c1ea3e FOREIGN KEY (competencecode_id) REFERENCES competence_codes(id);



ALTER TABLE ONLY persons_competence_codes
    ADD CONSTRAINT fk218a9691dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);



ALTER TABLE ONLY permissions_history
    ADD CONSTRAINT fk277bbd9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY vacation_codes_history
    ADD CONSTRAINT fk27e84399d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT fk3075811e3df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);



ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT fk3075811e86585224 FOREIGN KEY (shift_time_table_id) REFERENCES shift_time_table(id);



ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT fk3075811eda784c2b FOREIGN KEY (person_shift_id) REFERENCES person_shift(id);



ALTER TABLE ONLY shift_cancelled
    ADD CONSTRAINT fk3f7a55d43df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);



ALTER TABLE ONLY initialization_times
    ADD CONSTRAINT fk4094eae7e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY person_days_in_trouble
    ADD CONSTRAINT fk4b732cdbfbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);



ALTER TABLE ONLY contact_data
    ADD CONSTRAINT fk4c241869e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY permissions_groups_history
    ADD CONSTRAINT fk4d19b244d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT fk5170df704649fe84 FOREIGN KEY (vacation_codes_id) REFERENCES vacation_codes(id);



ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT fk5170df70e7a7b1be FOREIGN KEY (contract_id) REFERENCES contracts(id);



ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT fk53ee32958c3d68d6 FOREIGN KEY (absencetype_id) REFERENCES absence_types(id);



ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT fk53ee3295e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY valuable_competences
    ADD CONSTRAINT fk5842d3d9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY stamp_modification_types_history
    ADD CONSTRAINT fk5c3f2f67d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY absences
    ADD CONSTRAINT fk6674c5d65b5f15b1 FOREIGN KEY (absence_type_id) REFERENCES absence_types(id);



ALTER TABLE ONLY absences
    ADD CONSTRAINT fk6674c5d6fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);



ALTER TABLE ONLY qualifications_history
    ADD CONSTRAINT fk68e007b9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_children
    ADD CONSTRAINT fk6ace3a9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY working_time_type_days
    ADD CONSTRAINT fk74fdda1835555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);



ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT fk7757fc5e29b090bd FOREIGN KEY (personshifts_id) REFERENCES person_shift(id);



ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT fk7757fc5ebbfca55b FOREIGN KEY (shifttypes_id) REFERENCES shift_type(id);



ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f1435175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types(id);



ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f148868391d FOREIGN KEY (badge_reader_id) REFERENCES badge_readers(id);



ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f14932966bd FOREIGN KEY (stamp_type_id) REFERENCES stamp_types(id);



ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f14fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);



ALTER TABLE ONLY persons_permissions_history
    ADD CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT fk7ab49e924e498a6e FOREIGN KEY (person_reperibility_type_id) REFERENCES person_reperibility_types(id);



ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT fk7ab49e92e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY permissions_groups
    ADD CONSTRAINT fk7abb85ef34428ea9 FOREIGN KEY (permissions_id) REFERENCES permissions(id);



ALTER TABLE ONLY permissions_groups
    ADD CONSTRAINT fk7abb85ef522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);



ALTER TABLE ONLY absence_type_groups_history
    ADD CONSTRAINT fk82ea23ccd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY stamp_profiles
    ADD CONSTRAINT fk8379a4e6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY web_stamping_address
    ADD CONSTRAINT fk84df567fac97433e FOREIGN KEY (confparameters_id) REFERENCES configurations(id);



ALTER TABLE ONLY web_stamping_address_history
    ADD CONSTRAINT fk84ebf2d4d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_days_in_trouble_history
    ADD CONSTRAINT fk85e1ad30d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_months_history
    ADD CONSTRAINT fk863472d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY initialization_times_history
    ADD CONSTRAINT fk879a9f3cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_days_history
    ADD CONSTRAINT fk8fa3d556d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY working_time_types_history
    ADD CONSTRAINT fk94fa58aad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY year_recaps
    ADD CONSTRAINT fk9afc5dd6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY persons_groups_history
    ADD CONSTRAINT fka577bbcad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_reperibility_days_history
    ADD CONSTRAINT fka8bd39d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT fkb20b55e41df6de9 FOREIGN KEY (person_reperibility_id) REFERENCES person_reperibility(id);



ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT fkb20b55e47d5fd20c FOREIGN KEY (reperibility_type) REFERENCES person_reperibility_types(id);



ALTER TABLE ONLY groups_history
    ADD CONSTRAINT fkb7d05129d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY locations
    ADD CONSTRAINT fkb8a4575ee7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY person_months
    ADD CONSTRAINT fkbb6c161de7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY competences
    ADD CONSTRAINT fkbe7a61ca62728bb1 FOREIGN KEY (competence_code_id) REFERENCES competence_codes(id);



ALTER TABLE ONLY competences
    ADD CONSTRAINT fkbe7a61cae7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY absence_types_qualifications_history
    ADD CONSTRAINT fkbfc8501d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_reperibility_types_history
    ADD CONSTRAINT fkc5a3e3e1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY stamp_types_history
    ADD CONSTRAINT fkcc549f52d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY stampings_history
    ADD CONSTRAINT fkcd87c669d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_years_history
    ADD CONSTRAINT fkd30e49c1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe35555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);



ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe786a4ab6 FOREIGN KEY (qualification_id) REFERENCES qualifications(id);



ALTER TABLE ONLY configurations_history
    ADD CONSTRAINT fkda8f2892d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY persons_groups
    ADD CONSTRAINT fkdda67575522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);



ALTER TABLE ONLY persons_groups
    ADD CONSTRAINT fkdda67575dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);



ALTER TABLE ONLY contact_data_history
    ADD CONSTRAINT fke66d2abed54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY person_days
    ADD CONSTRAINT fke69adb01e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY contracts
    ADD CONSTRAINT fke86d11a1e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY person_shift
    ADD CONSTRAINT fked96d718e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY person_years
    ADD CONSTRAINT fkede9ea6ce7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);



ALTER TABLE ONLY person_children_history
    ADD CONSTRAINT fkee16b5fed54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY persons_permissions
    ADD CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id) REFERENCES permissions(id);



ALTER TABLE ONLY persons_permissions
    ADD CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id) REFERENCES persons(id);



ALTER TABLE ONLY badge_readers_history
    ADD CONSTRAINT fkf30286c9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY initialization_absences_history
    ADD CONSTRAINT fkf34058ead54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY persons_history
    ADD CONSTRAINT fkfceabd13d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);



ALTER TABLE ONLY absence_types
    ADD CONSTRAINT fkfe65dbf7ca0a1c8a FOREIGN KEY (absence_type_group_id) REFERENCES absence_type_groups(id);






