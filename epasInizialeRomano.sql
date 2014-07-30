--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: seq_absence_type_groups; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_absence_type_groups
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_absence_type_groups OWNER TO epas;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: absence_type_groups; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE absence_type_groups (
    id bigint DEFAULT nextval('seq_absence_type_groups'::regclass) NOT NULL,
    accumulationbehaviour character varying(255),
    accumulation_type character varying(255),
    label character varying(255),
    limit_in_minute integer,
    minutes_excess boolean,
    replacing_absence_type_id bigint
);


ALTER TABLE public.absence_type_groups OWNER TO epas;

--
-- Name: absence_type_groups_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.absence_type_groups_history OWNER TO epas;

--
-- Name: seq_absence_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_absence_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_absence_types OWNER TO epas;

--
-- Name: absence_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.absence_types OWNER TO epas;

--
-- Name: absence_types_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.absence_types_history OWNER TO epas;

--
-- Name: absence_types_qualifications; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE absence_types_qualifications (
    absencetypes_id bigint NOT NULL,
    qualifications_id bigint NOT NULL
);


ALTER TABLE public.absence_types_qualifications OWNER TO epas;

--
-- Name: absence_types_qualifications_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE absence_types_qualifications_history (
    _revision integer NOT NULL,
    absencetypes_id bigint NOT NULL,
    qualifications_id bigint NOT NULL,
    _revision_type smallint
);


ALTER TABLE public.absence_types_qualifications_history OWNER TO epas;

--
-- Name: seq_absences; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_absences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_absences OWNER TO epas;

--
-- Name: absences; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE absences (
    id bigint DEFAULT nextval('seq_absences'::regclass) NOT NULL,
    absence_file character varying(255),
    absence_type_id bigint,
    personday_id bigint NOT NULL
);


ALTER TABLE public.absences OWNER TO epas;

--
-- Name: absences_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE absences_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    absence_file character varying(255),
    absence_type_id bigint,
    personday_id bigint
);


ALTER TABLE public.absences_history OWNER TO epas;

--
-- Name: seq_auth_users; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_auth_users
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_auth_users OWNER TO epas;

--
-- Name: auth_users; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.auth_users OWNER TO epas;

--
-- Name: seq_badge_readers; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_badge_readers
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_badge_readers OWNER TO epas;

--
-- Name: badge_readers; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE badge_readers (
    id bigint DEFAULT nextval('seq_badge_readers'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255),
    enabled boolean NOT NULL,
    location character varying(255)
);


ALTER TABLE public.badge_readers OWNER TO epas;

--
-- Name: badge_readers_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE badge_readers_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255),
    enabled boolean,
    location character varying(255)
);


ALTER TABLE public.badge_readers_history OWNER TO epas;

--
-- Name: seq_certificated_data; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_certificated_data
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_certificated_data OWNER TO epas;

--
-- Name: certificated_data; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE certificated_data (
    id bigint DEFAULT nextval('seq_certificated_data'::regclass) NOT NULL,
    absences_sent character varying(255),
    cognome_nome character varying(255),
    competences_sent character varying(255),
    is_ok boolean,
    matricola character varying(255),
    mealticket_sent integer,
    month integer NOT NULL,
    problems character varying(255),
    year integer NOT NULL,
    person_id bigint NOT NULL,
    traininghours_sent character varying(255)
);


ALTER TABLE public.certificated_data OWNER TO epas;

--
-- Name: certificated_data_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE certificated_data_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    absences_sent character varying(255),
    cognome_nome character varying(255),
    competences_sent character varying(255),
    is_ok boolean,
    matricola character varying(255),
    mealticket_sent integer,
    month integer,
    problems character varying(255),
    year integer,
    person_id bigint,
    traininghours_sent character varying(255)
);


ALTER TABLE public.certificated_data_history OWNER TO epas;

--
-- Name: seq_competence_codes; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_competence_codes
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_competence_codes OWNER TO epas;

--
-- Name: competence_codes; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE competence_codes (
    id bigint DEFAULT nextval('seq_competence_codes'::regclass) NOT NULL,
    code character varying(255),
    codetopresence character varying(255),
    description character varying(255),
    inactive boolean NOT NULL
);


ALTER TABLE public.competence_codes OWNER TO epas;

--
-- Name: seq_competences; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_competences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_competences OWNER TO epas;

--
-- Name: competences; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE competences (
    id bigint DEFAULT nextval('seq_competences'::regclass) NOT NULL,
    month integer NOT NULL,
    reason character varying(1000),
    valueapproved integer NOT NULL,
    valuerequested numeric(4,1) NOT NULL,
    year integer NOT NULL,
    competence_code_id bigint NOT NULL,
    person_id bigint
);


ALTER TABLE public.competences OWNER TO epas;

--
-- Name: seq_conf_general; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_conf_general
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_conf_general OWNER TO epas;

--
-- Name: conf_general; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE conf_general (
    id bigint DEFAULT nextval('seq_conf_general'::regclass) NOT NULL,
    field text,
    field_value text,
    office_id bigint NOT NULL
);


ALTER TABLE public.conf_general OWNER TO epas;

--
-- Name: conf_general_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE conf_general_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    field text,
    field_value text,
    office_id bigint NOT NULL
);


ALTER TABLE public.conf_general_history OWNER TO epas;

--
-- Name: seq_conf_year; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_conf_year
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_conf_year OWNER TO epas;

--
-- Name: conf_year; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE conf_year (
    id bigint DEFAULT nextval('seq_conf_year'::regclass) NOT NULL,
    field text,
    field_value text,
    year integer,
    office_id bigint NOT NULL
);


ALTER TABLE public.conf_year OWNER TO epas;

--
-- Name: conf_year_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE conf_year_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    field text,
    field_value text,
    year integer,
    office_id bigint NOT NULL
);


ALTER TABLE public.conf_year_history OWNER TO epas;

--
-- Name: seq_configurations; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_configurations
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_configurations OWNER TO epas;

--
-- Name: configurations; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.configurations OWNER TO epas;

--
-- Name: configurations_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.configurations_history OWNER TO epas;

--
-- Name: contract_stamp_profiles; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE contract_stamp_profiles (
    id bigint NOT NULL,
    fixed_working_time boolean,
    start_from date,
    end_to date,
    contract_id bigint NOT NULL
);


ALTER TABLE public.contract_stamp_profiles OWNER TO epas;

--
-- Name: contract_stamp_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE contract_stamp_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.contract_stamp_profiles_id_seq OWNER TO epas;

--
-- Name: contract_stamp_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: epas
--

ALTER SEQUENCE contract_stamp_profiles_id_seq OWNED BY contract_stamp_profiles.id;


--
-- Name: seq_contract_year_recap; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_contract_year_recap
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_contract_year_recap OWNER TO epas;

--
-- Name: contract_year_recap; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE contract_year_recap (
    id bigint DEFAULT nextval('seq_contract_year_recap'::regclass) NOT NULL,
    has_source boolean,
    permission_used integer,
    recovery_day_used integer,
    remaining_minutes_current_year integer,
    remaining_minutes_last_year integer,
    vacation_current_year_used integer,
    vacation_last_year_used integer,
    year integer,
    contract_id bigint NOT NULL
);


ALTER TABLE public.contract_year_recap OWNER TO epas;

--
-- Name: seq_contracts; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_contracts
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_contracts OWNER TO epas;

--
-- Name: contracts; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE contracts (
    id bigint DEFAULT nextval('seq_contracts'::regclass) NOT NULL,
    begin_contract date,
    end_contract date,
    expire_contract date,
    oncertificate boolean NOT NULL,
    person_id bigint,
    source_date date,
    source_permission_used integer,
    source_recovery_day_used integer,
    source_remaining_minutes_current_year integer,
    source_remaining_minutes_last_year integer,
    source_vacation_current_year_used integer,
    source_vacation_last_year_used integer
);


ALTER TABLE public.contracts OWNER TO epas;

--
-- Name: seq_contracts_working_time_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_contracts_working_time_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_contracts_working_time_types OWNER TO epas;

--
-- Name: contracts_working_time_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE contracts_working_time_types (
    id bigint DEFAULT nextval('seq_contracts_working_time_types'::regclass) NOT NULL,
    begin_date date,
    end_date date,
    contract_id bigint,
    working_time_type_id bigint
);


ALTER TABLE public.contracts_working_time_types OWNER TO epas;

--
-- Name: seq_groups; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_groups
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_groups OWNER TO epas;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE groups (
    id bigint DEFAULT nextval('seq_groups'::regclass) NOT NULL,
    description character varying(255)
);


ALTER TABLE public.groups OWNER TO epas;

--
-- Name: groups_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE groups_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);


ALTER TABLE public.groups_history OWNER TO epas;

--
-- Name: seq_initialization_absences; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_initialization_absences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_initialization_absences OWNER TO epas;

--
-- Name: initialization_absences; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE initialization_absences (
    id bigint DEFAULT nextval('seq_initialization_absences'::regclass) NOT NULL,
    absencedays integer,
    date date,
    recovery_days integer,
    absencetype_id bigint NOT NULL,
    person_id bigint NOT NULL
);


ALTER TABLE public.initialization_absences OWNER TO epas;

--
-- Name: initialization_absences_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.initialization_absences_history OWNER TO epas;

--
-- Name: seq_initialization_times; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_initialization_times
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_initialization_times OWNER TO epas;

--
-- Name: initialization_times; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE initialization_times (
    id bigint DEFAULT nextval('seq_initialization_times'::regclass) NOT NULL,
    date date,
    residualminutescurrentyear integer,
    residualminutespastyear integer,
    person_id bigint NOT NULL,
    permissionused integer,
    recoverydayused integer,
    vacationcurrentyearused integer,
    vacationlastyearused integer
);


ALTER TABLE public.initialization_times OWNER TO epas;

--
-- Name: initialization_times_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE initialization_times_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    residualminutescurrentyear integer,
    residualminutespastyear integer,
    person_id bigint,
    permissionused integer,
    recoverydayused integer,
    vacationcurrentyearused integer,
    vacationlastyearused integer
);


ALTER TABLE public.initialization_times_history OWNER TO epas;

--
-- Name: meal_ticket; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE meal_ticket (
    id bigint NOT NULL,
    code text,
    block integer,
    number integer,
    year integer,
    quarter integer,
    date date,
    person_id bigint NOT NULL,
    admin_id bigint NOT NULL
);


ALTER TABLE public.meal_ticket OWNER TO epas;

--
-- Name: meal_ticket_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE meal_ticket_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code text,
    block integer,
    number integer,
    year integer,
    quarter integer,
    date date,
    person_id bigint,
    admin_id bigint
);


ALTER TABLE public.meal_ticket_history OWNER TO epas;

--
-- Name: meal_ticket_id_seq; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE meal_ticket_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.meal_ticket_id_seq OWNER TO epas;

--
-- Name: meal_ticket_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: epas
--

ALTER SEQUENCE meal_ticket_id_seq OWNED BY meal_ticket.id;


--
-- Name: seq_office; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_office
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_office OWNER TO epas;

--
-- Name: office; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE office (
    id bigint DEFAULT nextval('seq_office'::regclass) NOT NULL,
    name character varying(255),
    address character varying(255),
    code integer,
    joining_date date,
    office_id bigint,
    contraction text
);


ALTER TABLE public.office OWNER TO epas;

--
-- Name: office_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE office_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    name character varying(255),
    address character varying(255),
    code integer,
    joining_date date,
    office_id bigint,
    contraction text
);


ALTER TABLE public.office_history OWNER TO epas;

--
-- Name: seq_options; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_options
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_options OWNER TO epas;

--
-- Name: options; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.options OWNER TO epas;

--
-- Name: seq_permissions; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_permissions
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_permissions OWNER TO epas;

--
-- Name: permissions; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE permissions (
    id bigint DEFAULT nextval('seq_permissions'::regclass) NOT NULL,
    description character varying(255)
);


ALTER TABLE public.permissions OWNER TO epas;

--
-- Name: permissions_groups; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE permissions_groups (
    permissions_id bigint NOT NULL,
    groups_id bigint NOT NULL
);


ALTER TABLE public.permissions_groups OWNER TO epas;

--
-- Name: permissions_groups_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE permissions_groups_history (
    _revision integer NOT NULL,
    permissions_id bigint NOT NULL,
    groups_id bigint NOT NULL,
    _revision_type smallint
);


ALTER TABLE public.permissions_groups_history OWNER TO epas;

--
-- Name: permissions_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE permissions_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);


ALTER TABLE public.permissions_history OWNER TO epas;

--
-- Name: seq_person_children; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_children
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_children OWNER TO epas;

--
-- Name: person_children; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_children (
    id bigint DEFAULT nextval('seq_person_children'::regclass) NOT NULL,
    borndate date,
    name character varying(255),
    surname character varying(255),
    person_id bigint
);


ALTER TABLE public.person_children OWNER TO epas;

--
-- Name: person_children_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_children_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    borndate date,
    name character varying(255),
    surname character varying(255),
    person_id bigint
);


ALTER TABLE public.person_children_history OWNER TO epas;

--
-- Name: seq_person_days; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_days OWNER TO epas;

--
-- Name: person_days; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_days (
    id bigint DEFAULT nextval('seq_person_days'::regclass) NOT NULL,
    date date,
    difference integer,
    is_ticket_available boolean,
    is_ticket_forced_by_admin boolean,
    is_working_in_another_place boolean,
    progressive integer,
    time_at_work integer,
    person_id bigint NOT NULL,
    stamp_modification_type_id bigint
);


ALTER TABLE public.person_days OWNER TO epas;

--
-- Name: person_days_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_days_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    difference integer,
    is_ticket_available boolean,
    is_ticket_forced_by_admin boolean,
    is_working_in_another_place boolean,
    progressive integer,
    time_at_work integer,
    person_id bigint,
    stamp_modification_type_id bigint
);


ALTER TABLE public.person_days_history OWNER TO epas;

--
-- Name: seq_person_days_in_trouble; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_days_in_trouble
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_days_in_trouble OWNER TO epas;

--
-- Name: person_days_in_trouble; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_days_in_trouble (
    id bigint DEFAULT nextval('seq_person_days_in_trouble'::regclass) NOT NULL,
    cause character varying(255),
    emailsent boolean NOT NULL,
    fixed boolean NOT NULL,
    personday_id bigint NOT NULL
);


ALTER TABLE public.person_days_in_trouble OWNER TO epas;

--
-- Name: seq_person_hour_for_overtime; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_hour_for_overtime
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_hour_for_overtime OWNER TO epas;

--
-- Name: person_hour_for_overtime; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_hour_for_overtime (
    id bigint DEFAULT nextval('seq_person_hour_for_overtime'::regclass) NOT NULL,
    numberofhourforovertime integer,
    person_id bigint
);


ALTER TABLE public.person_hour_for_overtime OWNER TO epas;

--
-- Name: seq_person_months_recap; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_months_recap
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_months_recap OWNER TO epas;

--
-- Name: person_months_recap; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_months_recap (
    id bigint DEFAULT nextval('seq_person_months_recap'::regclass) NOT NULL,
    year integer,
    month integer,
    training_hours integer,
    hours_approved boolean,
    person_id bigint NOT NULL,
    fromdate date,
    todate date
);


ALTER TABLE public.person_months_recap OWNER TO epas;

--
-- Name: person_months_recap_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_months_recap_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    year integer,
    month integer,
    training_hours integer,
    hours_approved boolean,
    person_id bigint NOT NULL
);


ALTER TABLE public.person_months_recap_history OWNER TO epas;

--
-- Name: person_reperibility; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_reperibility (
    id bigint NOT NULL,
    end_date date,
    note character varying(255),
    start_date date,
    person_id bigint,
    person_reperibility_type_id bigint
);


ALTER TABLE public.person_reperibility OWNER TO epas;

--
-- Name: person_reperibility_days; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_reperibility_days (
    id bigint NOT NULL,
    date date,
    holiday_day boolean,
    person_reperibility_id bigint NOT NULL,
    reperibility_type bigint
);


ALTER TABLE public.person_reperibility_days OWNER TO epas;

--
-- Name: person_reperibility_days_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_reperibility_days_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    date date,
    holiday_day boolean,
    person_reperibility_id bigint,
    reperibility_type bigint
);


ALTER TABLE public.person_reperibility_days_history OWNER TO epas;

--
-- Name: person_reperibility_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.person_reperibility_history OWNER TO epas;

--
-- Name: person_reperibility_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_reperibility_types (
    id bigint NOT NULL,
    description character varying(255)
);


ALTER TABLE public.person_reperibility_types OWNER TO epas;

--
-- Name: person_reperibility_types_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_reperibility_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255)
);


ALTER TABLE public.person_reperibility_types_history OWNER TO epas;

--
-- Name: person_shift; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_shift (
    id bigint NOT NULL,
    description character varying(255),
    jolly boolean NOT NULL,
    person_id bigint NOT NULL
);


ALTER TABLE public.person_shift OWNER TO epas;

--
-- Name: person_shift_days; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_shift_days (
    id bigint NOT NULL,
    date date,
    person_shift_id bigint NOT NULL,
    shift_type_id bigint,
    shift_slot character varying(64)
);


ALTER TABLE public.person_shift_days OWNER TO epas;

--
-- Name: person_shift_shift_type; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_shift_shift_type (
    id bigint NOT NULL,
    begin_date date,
    end_date date,
    personshifts_id bigint,
    shifttypes_id bigint
);


ALTER TABLE public.person_shift_shift_type OWNER TO epas;

--
-- Name: seq_person_years; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_years
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_years OWNER TO epas;

--
-- Name: person_years; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_years (
    id bigint DEFAULT nextval('seq_person_years'::regclass) NOT NULL,
    remaining_minutes integer,
    remaining_vacation_days integer,
    year integer,
    person_id bigint NOT NULL
);


ALTER TABLE public.person_years OWNER TO epas;

--
-- Name: person_years_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE person_years_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    remaining_minutes integer,
    remaining_vacation_days integer,
    year integer,
    person_id bigint
);


ALTER TABLE public.person_years_history OWNER TO epas;

--
-- Name: seq_persons; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_persons
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_persons OWNER TO epas;

--
-- Name: persons; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE persons (
    id bigint DEFAULT nextval('seq_persons'::regclass) NOT NULL,
    badgenumber character varying(255),
    born_date timestamp without time zone,
    email character varying(255),
    name character varying(255),
    number integer,
    oldid bigint,
    other_surnames character varying(255),
    surname character varying(255),
    version integer,
    qualification_id bigint,
    office_id bigint,
    user_id bigint,
    cnr_email character varying(255),
    fax character varying(255),
    mobile character varying(255),
    telephone character varying(255),
    department character varying(255),
    head_office character varying(255),
    room character varying(255),
    want_email boolean,
    birthday date
);


ALTER TABLE public.persons OWNER TO epas;

--
-- Name: persons_competence_codes; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE persons_competence_codes (
    persons_id bigint NOT NULL,
    competencecode_id bigint NOT NULL
);


ALTER TABLE public.persons_competence_codes OWNER TO epas;

--
-- Name: persons_groups; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE persons_groups (
    persons_id bigint NOT NULL,
    groups_id bigint NOT NULL
);


ALTER TABLE public.persons_groups OWNER TO epas;

--
-- Name: persons_groups_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE persons_groups_history (
    _revision integer NOT NULL,
    persons_id bigint NOT NULL,
    groups_id bigint NOT NULL,
    _revision_type smallint
);


ALTER TABLE public.persons_groups_history OWNER TO epas;

--
-- Name: persons_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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
    surname character varying(255),
    qualification_id bigint,
    working_time_type_id bigint,
    office_id bigint,
    user_id bigint,
    cnr_email character varying(255),
    fax character varying(255),
    mobile character varying(255),
    telephone character varying(255),
    department character varying(255),
    head_office character varying(255),
    room character varying(255),
    want_email boolean,
    birthday date
);


ALTER TABLE public.persons_history OWNER TO epas;

--
-- Name: seq_persons_working_time_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_persons_working_time_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_persons_working_time_types OWNER TO epas;

--
-- Name: persons_working_time_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE persons_working_time_types (
    id bigint DEFAULT nextval('seq_persons_working_time_types'::regclass) NOT NULL,
    begin_date date,
    end_date date,
    person_id bigint,
    working_time_type_id bigint
);


ALTER TABLE public.persons_working_time_types OWNER TO epas;

--
-- Name: play_evolutions; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE play_evolutions (
    id integer NOT NULL,
    hash character varying(255) NOT NULL,
    applied_at timestamp without time zone NOT NULL,
    apply_script text,
    revert_script text,
    state character varying(255),
    last_problem text
);


ALTER TABLE public.play_evolutions OWNER TO epas;

--
-- Name: seq_qualifications; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_qualifications
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_qualifications OWNER TO epas;

--
-- Name: qualifications; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE qualifications (
    id bigint DEFAULT nextval('seq_qualifications'::regclass) NOT NULL,
    description character varying(255),
    qualification integer NOT NULL
);


ALTER TABLE public.qualifications OWNER TO epas;

--
-- Name: qualifications_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE qualifications_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    qualification integer
);


ALTER TABLE public.qualifications_history OWNER TO epas;

--
-- Name: seq_revinfo; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_revinfo
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_revinfo OWNER TO epas;

--
-- Name: revinfo; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE revinfo (
    rev integer DEFAULT nextval('seq_revinfo'::regclass) NOT NULL,
    revtstmp bigint
);


ALTER TABLE public.revinfo OWNER TO epas;

--
-- Name: roles; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE roles (
    id bigint NOT NULL,
    name text
);


ALTER TABLE public.roles OWNER TO epas;

--
-- Name: roles_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE roles_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    name text
);


ALTER TABLE public.roles_history OWNER TO epas;

--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.roles_id_seq OWNER TO epas;

--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: epas
--

ALTER SEQUENCE roles_id_seq OWNED BY roles.id;


--
-- Name: roles_permissions; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE roles_permissions (
    roles_id bigint NOT NULL,
    permissions_id bigint NOT NULL
);


ALTER TABLE public.roles_permissions OWNER TO epas;

--
-- Name: roles_permissions_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE roles_permissions_history (
    roles_id bigint,
    permissions_id bigint,
    _revision integer NOT NULL,
    _revision_type smallint
);


ALTER TABLE public.roles_permissions_history OWNER TO epas;

--
-- Name: seq_person_reperibility; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_reperibility
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_reperibility OWNER TO epas;

--
-- Name: seq_person_reperibility_days; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_reperibility_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_reperibility_days OWNER TO epas;

--
-- Name: seq_person_reperibility_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_reperibility_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_reperibility_types OWNER TO epas;

--
-- Name: seq_person_shift; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_shift
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_shift OWNER TO epas;

--
-- Name: seq_person_shift_days; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_shift_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_shift_days OWNER TO epas;

--
-- Name: seq_person_shift_shift_type; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_person_shift_shift_type
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_person_shift_shift_type OWNER TO epas;

--
-- Name: seq_shift_cancelled; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_shift_cancelled
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_shift_cancelled OWNER TO epas;

--
-- Name: seq_shift_time_table; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_shift_time_table
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_shift_time_table OWNER TO epas;

--
-- Name: seq_shift_type; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_shift_type
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_shift_type OWNER TO epas;

--
-- Name: seq_stamp_modification_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_stamp_modification_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_stamp_modification_types OWNER TO epas;

--
-- Name: seq_stamp_profiles; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_stamp_profiles
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_stamp_profiles OWNER TO epas;

--
-- Name: seq_stamp_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_stamp_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_stamp_types OWNER TO epas;

--
-- Name: seq_stampings; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_stampings
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_stampings OWNER TO epas;

--
-- Name: seq_total_overtime; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_total_overtime
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_total_overtime OWNER TO epas;

--
-- Name: seq_users; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_users
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_users OWNER TO epas;

--
-- Name: seq_users_permissions_offices; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_users_permissions_offices
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_users_permissions_offices OWNER TO epas;

--
-- Name: seq_vacation_codes; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_vacation_codes
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_vacation_codes OWNER TO epas;

--
-- Name: seq_vacation_periods; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_vacation_periods
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_vacation_periods OWNER TO epas;

--
-- Name: seq_valuable_competences; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_valuable_competences
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_valuable_competences OWNER TO epas;

--
-- Name: seq_web_stamping_address; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_web_stamping_address
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_web_stamping_address OWNER TO epas;

--
-- Name: seq_working_time_type_days; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_working_time_type_days
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_working_time_type_days OWNER TO epas;

--
-- Name: seq_working_time_types; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_working_time_types
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_working_time_types OWNER TO epas;

--
-- Name: seq_year_recaps; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE seq_year_recaps
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_year_recaps OWNER TO epas;

--
-- Name: shift_cancelled; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE shift_cancelled (
    id bigint NOT NULL,
    date date,
    shift_type_id bigint NOT NULL
);


ALTER TABLE public.shift_cancelled OWNER TO epas;

--
-- Name: shift_time_table; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE shift_time_table (
    id bigint DEFAULT nextval('seq_shift_time_table'::regclass) NOT NULL,
    start_morning character varying(64),
    end_morning character varying(64),
    start_afternoon character varying(64),
    end_afternoon character varying(64),
    start_morning_lunch_time character varying(64),
    end_morning_lunch_time character varying(64),
    start_afternoon_lunch_time character varying(64),
    end_afternoon_lunch_time character varying(64),
    total_working_minutes integer,
    paid_minutes integer
);


ALTER TABLE public.shift_time_table OWNER TO epas;

--
-- Name: shift_type; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE shift_type (
    id bigint NOT NULL,
    description character varying(255),
    type character varying(255),
    shift_time_table_id bigint,
    supervisor bigint
);


ALTER TABLE public.shift_type OWNER TO epas;

--
-- Name: shift_type_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE shift_type_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    type character varying(255),
    supervisor bigint
);


ALTER TABLE public.shift_type_history OWNER TO epas;

--
-- Name: stamp_modification_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE stamp_modification_types (
    id bigint DEFAULT nextval('seq_stamp_modification_types'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255)
);


ALTER TABLE public.stamp_modification_types OWNER TO epas;

--
-- Name: stamp_modification_types_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE stamp_modification_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255)
);


ALTER TABLE public.stamp_modification_types_history OWNER TO epas;

--
-- Name: stamp_profiles; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE stamp_profiles (
    id bigint DEFAULT nextval('seq_stamp_profiles'::regclass) NOT NULL,
    end_to date,
    fixedworkingtime boolean NOT NULL,
    start_from date,
    person_id bigint NOT NULL
);


ALTER TABLE public.stamp_profiles OWNER TO epas;

--
-- Name: stamp_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE stamp_types (
    id bigint DEFAULT nextval('seq_stamp_types'::regclass) NOT NULL,
    code character varying(255),
    description character varying(255),
    identifier character varying(255)
);


ALTER TABLE public.stamp_types OWNER TO epas;

--
-- Name: stamp_types_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE stamp_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    code character varying(255),
    description character varying(255),
    identifier character varying(255)
);


ALTER TABLE public.stamp_types_history OWNER TO epas;

--
-- Name: stampings; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.stampings OWNER TO epas;

--
-- Name: stampings_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.stampings_history OWNER TO epas;

--
-- Name: total_overtime; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE total_overtime (
    id bigint DEFAULT nextval('seq_total_overtime'::regclass) NOT NULL,
    date date,
    numberofhours integer,
    year integer,
    office_id bigint
);


ALTER TABLE public.total_overtime OWNER TO epas;

--
-- Name: users; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE users (
    id bigint DEFAULT nextval('seq_users'::regclass) NOT NULL,
    expire_recovery_token date,
    password character varying(255),
    recovery_token character varying(255),
    username character varying(255)
);


ALTER TABLE public.users OWNER TO epas;

--
-- Name: users_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE users_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    expire_recovery_token date,
    password character varying(255),
    recovery_token character varying(255),
    username character varying(255)
);


ALTER TABLE public.users_history OWNER TO epas;

--
-- Name: users_roles_offices; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE users_roles_offices (
    id bigint NOT NULL,
    office_id bigint,
    role_id bigint,
    user_id bigint
);


ALTER TABLE public.users_roles_offices OWNER TO epas;

--
-- Name: users_roles_offices_id_seq; Type: SEQUENCE; Schema: public; Owner: epas
--

CREATE SEQUENCE users_roles_offices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_roles_offices_id_seq OWNER TO epas;

--
-- Name: users_roles_offices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: epas
--

ALTER SEQUENCE users_roles_offices_id_seq OWNED BY users_roles_offices.id;


--
-- Name: vacation_codes; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE vacation_codes (
    id bigint DEFAULT nextval('seq_vacation_codes'::regclass) NOT NULL,
    description character varying(255),
    permission_days integer,
    vacation_days integer
);


ALTER TABLE public.vacation_codes OWNER TO epas;

--
-- Name: vacation_codes_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE vacation_codes_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    permission_days integer,
    vacation_days integer
);


ALTER TABLE public.vacation_codes_history OWNER TO epas;

--
-- Name: vacation_periods; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE vacation_periods (
    id bigint DEFAULT nextval('seq_vacation_periods'::regclass) NOT NULL,
    begin_from date,
    end_to date,
    contract_id bigint NOT NULL,
    vacation_codes_id bigint NOT NULL
);


ALTER TABLE public.vacation_periods OWNER TO epas;

--
-- Name: vacation_periods_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE vacation_periods_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    begin_from date,
    end_to date,
    contract_id bigint,
    vacation_codes_id bigint
);


ALTER TABLE public.vacation_periods_history OWNER TO epas;

--
-- Name: valuable_competences; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE valuable_competences (
    id bigint DEFAULT nextval('seq_valuable_competences'::regclass) NOT NULL,
    codicecomp character varying(255),
    descrizione character varying(255),
    person_id bigint
);


ALTER TABLE public.valuable_competences OWNER TO epas;

--
-- Name: web_stamping_address; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE web_stamping_address (
    id bigint DEFAULT nextval('seq_web_stamping_address'::regclass) NOT NULL,
    webaddresstype character varying(255),
    confparameters_id bigint
);


ALTER TABLE public.web_stamping_address OWNER TO epas;

--
-- Name: web_stamping_address_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE web_stamping_address_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    webaddresstype character varying(255),
    confparameters_id bigint
);


ALTER TABLE public.web_stamping_address_history OWNER TO epas;

--
-- Name: working_time_type_days; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.working_time_type_days OWNER TO epas;

--
-- Name: working_time_type_days_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.working_time_type_days_history OWNER TO epas;

--
-- Name: working_time_types; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE working_time_types (
    id bigint DEFAULT nextval('seq_working_time_types'::regclass) NOT NULL,
    description character varying(255) NOT NULL,
    shift boolean NOT NULL,
    meal_ticket_enabled boolean,
    office_id bigint,
    disabled boolean
);


ALTER TABLE public.working_time_types OWNER TO epas;

--
-- Name: working_time_types_history; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

CREATE TABLE working_time_types_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
    _revision_type smallint,
    description character varying(255),
    shift boolean,
    meal_ticket_enabled boolean,
    office_id bigint,
    disabled boolean
);


ALTER TABLE public.working_time_types_history OWNER TO epas;

--
-- Name: year_recaps; Type: TABLE; Schema: public; Owner: epas; Tablespace: 
--

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


ALTER TABLE public.year_recaps OWNER TO epas;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contract_stamp_profiles ALTER COLUMN id SET DEFAULT nextval('contract_stamp_profiles_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: epas
--

ALTER TABLE ONLY meal_ticket ALTER COLUMN id SET DEFAULT nextval('meal_ticket_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: epas
--

ALTER TABLE ONLY roles ALTER COLUMN id SET DEFAULT nextval('roles_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: epas
--

ALTER TABLE ONLY users_roles_offices ALTER COLUMN id SET DEFAULT nextval('users_roles_offices_id_seq'::regclass);


--
-- Data for Name: absence_type_groups; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_type_groups (id, accumulationbehaviour, accumulation_type, label, limit_in_minute, minutes_excess, replacing_absence_type_id) FROM stdin;
\.


--
-- Data for Name: absence_type_groups_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_type_groups_history (id, _revision, _revision_type, accumulationbehaviour, accumulation_type, label, limit_in_minute, minutes_excess, replacing_absence_type_id) FROM stdin;
\.


--
-- Data for Name: absence_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_types (id, certification_code, code, compensatory_rest, considered_week_end, description, ignore_stamping, internal_use, justified_time_at_work, meal_ticket_calculation, multiple_use, replacing_absence, valid_from, valid_to, absence_type_group_id) FROM stdin;
\.


--
-- Data for Name: absence_types_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_types_history (id, _revision, _revision_type, certification_code, code, compensatory_rest, considered_week_end, description, ignore_stamping, internal_use, justified_time_at_work, meal_ticket_calculation, multiple_use, replacing_absence, valid_from, valid_to, absence_type_group_id) FROM stdin;
\.


--
-- Data for Name: absence_types_qualifications; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_types_qualifications (absencetypes_id, qualifications_id) FROM stdin;
\.


--
-- Data for Name: absence_types_qualifications_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absence_types_qualifications_history (_revision, absencetypes_id, qualifications_id, _revision_type) FROM stdin;
\.


--
-- Data for Name: absences; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absences (id, absence_file, absence_type_id, personday_id) FROM stdin;
\.


--
-- Data for Name: absences_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY absences_history (id, _revision, _revision_type, absence_file, absence_type_id, personday_id) FROM stdin;
\.


--
-- Data for Name: auth_users; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY auth_users (id, authip, authmod, authred, autsys, datacpas, password, passwordmd5, scadenzapassword, ultimamodifica, username) FROM stdin;
\.


--
-- Data for Name: badge_readers; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY badge_readers (id, code, description, enabled, location) FROM stdin;
\.


--
-- Data for Name: badge_readers_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY badge_readers_history (id, _revision, _revision_type, code, description, enabled, location) FROM stdin;
\.


--
-- Data for Name: certificated_data; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY certificated_data (id, absences_sent, cognome_nome, competences_sent, is_ok, matricola, mealticket_sent, month, problems, year, person_id, traininghours_sent) FROM stdin;
\.


--
-- Data for Name: certificated_data_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY certificated_data_history (id, _revision, _revision_type, absences_sent, cognome_nome, competences_sent, is_ok, matricola, mealticket_sent, month, problems, year, person_id, traininghours_sent) FROM stdin;
\.


--
-- Data for Name: competence_codes; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY competence_codes (id, code, codetopresence, description, inactive) FROM stdin;
\.


--
-- Data for Name: competences; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY competences (id, month, reason, valueapproved, valuerequested, year, competence_code_id, person_id) FROM stdin;
\.


--
-- Data for Name: conf_general; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY conf_general (id, field, field_value, office_id) FROM stdin;
1	init_use_program	\N	1
2	institute_name	\N	1
3	email_to_contact	\N	1
4	seat_code	\N	1
5	url_to_presence	\N	1
6	user_to_presence	\N	1
7	password_to_presence	\N	1
8	number_of_viewing_couple	\N	1
9	month_of_patron	\N	1
10	day_of_patron	\N	1
11	web_stamping_allowed	\N	1
12	meal_time_start_hour	\N	1
13	meal_time_start_minute	\N	1
14	meal_time_end_hour	\N	1
15	meal_time_end_minute	\N	1
\.


--
-- Data for Name: conf_general_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY conf_general_history (id, _revision, _revision_type, field, field_value, office_id) FROM stdin;
\.


--
-- Data for Name: conf_year; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY conf_year (id, field, field_value, year, office_id) FROM stdin;
1	month_expiry_vacation_past_year	\N	2014	1
2	day_expiry_vacation_past_year	\N	2014	1
3	month_expire_recovery_days_13	\N	2014	1
4	month_expire_recovery_days_49	\N	2014	1
5	max_recovery_days_13	\N	2014	1
6	max_recovery_days_49	\N	2014	1
7	hour_max_to_calculate_worktime	\N	2014	1
8	month_expiry_vacation_past_year	\N	2013	1
9	day_expiry_vacation_past_year	\N	2013	1
10	month_expire_recovery_days_13	\N	2013	1
11	month_expire_recovery_days_49	\N	2013	1
12	max_recovery_days_13	\N	2013	1
13	max_recovery_days_49	\N	2013	1
14	hour_max_to_calculate_worktime	\N	2013	1
\.


--
-- Data for Name: conf_year_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY conf_year_history (id, _revision, _revision_type, field, field_value, year, office_id) FROM stdin;
\.


--
-- Data for Name: configurations; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY configurations (id, addworkingtimeinexcess, begin_date, calculateintervaltimewithoutreturnfromintevaltime, caninsertmoreabsencecodeinday, canpeopleautodeclareabsences, canpeopleautodeclareworkingtime, canpeopleusewebstamping, capacityfoureight, capacityonethree, dayexpiryvacationpastyear, day_of_patron, email_to_contact, end_date, holydaysandvacationsoverpermitted, hourmaxtocalculateworktime, ignoreworkingtimewithabsencecode, in_use, init_use_program, insertandmodifyworkingtimewithplustoreduceatrealworkingtime, institute_name, isfirstorlastmissiondayaholiday, isholidayinmissionaworkingday, isintervaltimecutfromworkingtime, islastdaybeforeeasterentire, islastdaybeforexmasentire, islastdayoftheyearentire, ismealtimeshorterthanminimum, maxrecoverydaysfournine, maxrecoverydaysonethree, maximumovertimehours, mealticketassignedwithmealtimereal, mealticketassignedwithreasonmealtime, mealtime, minimumremainingtimetohaverecoveryday, monthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, month_of_patron, numberofviewingcouplecolumn, password_to_presence, path_to_save_presence_situation, residual, seat_code, textformonthlysituation, url_to_presence, user_to_presence, workingtime, workingtimetohavemealticket, mealtimestarthour, mealtimestartminute, mealtimeendminute, mealtimeendhour) FROM stdin;
\.


--
-- Data for Name: configurations_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY configurations_history (id, _revision, _revision_type, addworkingtimeinexcess, begin_date, calculateintervaltimewithoutreturnfromintevaltime, caninsertmoreabsencecodeinday, canpeopleautodeclareabsences, canpeopleautodeclareworkingtime, canpeopleusewebstamping, capacityfoureight, capacityonethree, dayexpiryvacationpastyear, day_of_patron, email_to_contact, end_date, holydaysandvacationsoverpermitted, hourmaxtocalculateworktime, ignoreworkingtimewithabsencecode, in_use, init_use_program, insertandmodifyworkingtimewithplustoreduceatrealworkingtime, institute_name, isfirstorlastmissiondayaholiday, isholidayinmissionaworkingday, isintervaltimecutfromworkingtime, islastdaybeforeeasterentire, islastdaybeforexmasentire, islastdayoftheyearentire, ismealtimeshorterthanminimum, maxrecoverydaysfournine, maxrecoverydaysonethree, maximumovertimehours, mealticketassignedwithmealtimereal, mealticketassignedwithreasonmealtime, mealtime, minimumremainingtimetohaverecoveryday, monthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, month_of_patron, numberofviewingcouplecolumn, password_to_presence, path_to_save_presence_situation, residual, seat_code, textformonthlysituation, url_to_presence, user_to_presence, workingtime, workingtimetohavemealticket, mealtimeendhour, mealtimeendminute, mealtimestarthour, mealtimestartminute) FROM stdin;
\.


--
-- Data for Name: contract_stamp_profiles; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY contract_stamp_profiles (id, fixed_working_time, start_from, end_to, contract_id) FROM stdin;
\.


--
-- Name: contract_stamp_profiles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('contract_stamp_profiles_id_seq', 1, false);


--
-- Data for Name: contract_year_recap; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY contract_year_recap (id, has_source, permission_used, recovery_day_used, remaining_minutes_current_year, remaining_minutes_last_year, vacation_current_year_used, vacation_last_year_used, year, contract_id) FROM stdin;
\.


--
-- Data for Name: contracts; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY contracts (id, begin_contract, end_contract, expire_contract, oncertificate, person_id, source_date, source_permission_used, source_recovery_day_used, source_remaining_minutes_current_year, source_remaining_minutes_last_year, source_vacation_current_year_used, source_vacation_last_year_used) FROM stdin;
\.


--
-- Data for Name: contracts_working_time_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY contracts_working_time_types (id, begin_date, end_date, contract_id, working_time_type_id) FROM stdin;
\.


--
-- Data for Name: groups; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY groups (id, description) FROM stdin;
\.


--
-- Data for Name: groups_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY groups_history (id, _revision, _revision_type, description) FROM stdin;
\.


--
-- Data for Name: initialization_absences; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY initialization_absences (id, absencedays, date, recovery_days, absencetype_id, person_id) FROM stdin;
\.


--
-- Data for Name: initialization_absences_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY initialization_absences_history (id, _revision, _revision_type, absencedays, date, recovery_days, absencetype_id, person_id) FROM stdin;
\.


--
-- Data for Name: initialization_times; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY initialization_times (id, date, residualminutescurrentyear, residualminutespastyear, person_id, permissionused, recoverydayused, vacationcurrentyearused, vacationlastyearused) FROM stdin;
\.


--
-- Data for Name: initialization_times_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY initialization_times_history (id, _revision, _revision_type, date, residualminutescurrentyear, residualminutespastyear, person_id, permissionused, recoverydayused, vacationcurrentyearused, vacationlastyearused) FROM stdin;
\.


--
-- Data for Name: meal_ticket; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY meal_ticket (id, code, block, number, year, quarter, date, person_id, admin_id) FROM stdin;
\.


--
-- Data for Name: meal_ticket_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY meal_ticket_history (id, _revision, _revision_type, code, block, number, year, quarter, date, person_id, admin_id) FROM stdin;
\.


--
-- Name: meal_ticket_id_seq; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('meal_ticket_id_seq', 1, false);


--
-- Data for Name: office; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY office (id, name, address, code, joining_date, office_id, contraction) FROM stdin;
1	NOME-DA-DEFINIRE		0	\N	\N	\N
\.


--
-- Data for Name: office_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY office_history (id, _revision, _revision_type, name, address, code, joining_date, office_id, contraction) FROM stdin;
\.


--
-- Data for Name: options; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY options (id, easterchristmas, adjustrange, adjustrangeday, autorange, date, expiredvacationday, expiredvacationmonth, otherheadoffice, patronday, patronmonth, recoveryap, recoverymonth, tipo_ferie_gen, tipo_permieg, vacationtype, vacationtypep) FROM stdin;
\.


--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY permissions (id, description) FROM stdin;
1	insertAndUpdateOffices
\.


--
-- Data for Name: permissions_groups; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY permissions_groups (permissions_id, groups_id) FROM stdin;
\.


--
-- Data for Name: permissions_groups_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY permissions_groups_history (_revision, permissions_id, groups_id, _revision_type) FROM stdin;
\.


--
-- Data for Name: permissions_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY permissions_history (id, _revision, _revision_type, description) FROM stdin;
\.


--
-- Data for Name: person_children; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_children (id, borndate, name, surname, person_id) FROM stdin;
\.


--
-- Data for Name: person_children_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_children_history (id, _revision, _revision_type, borndate, name, surname, person_id) FROM stdin;
\.


--
-- Data for Name: person_days; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_days (id, date, difference, is_ticket_available, is_ticket_forced_by_admin, is_working_in_another_place, progressive, time_at_work, person_id, stamp_modification_type_id) FROM stdin;
\.


--
-- Data for Name: person_days_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_days_history (id, _revision, _revision_type, date, difference, is_ticket_available, is_ticket_forced_by_admin, is_working_in_another_place, progressive, time_at_work, person_id, stamp_modification_type_id) FROM stdin;
\.


--
-- Data for Name: person_days_in_trouble; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_days_in_trouble (id, cause, emailsent, fixed, personday_id) FROM stdin;
\.


--
-- Data for Name: person_hour_for_overtime; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_hour_for_overtime (id, numberofhourforovertime, person_id) FROM stdin;
\.


--
-- Data for Name: person_months_recap; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_months_recap (id, year, month, training_hours, hours_approved, person_id, fromdate, todate) FROM stdin;
\.


--
-- Data for Name: person_months_recap_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_months_recap_history (id, _revision, _revision_type, year, month, training_hours, hours_approved, person_id) FROM stdin;
\.


--
-- Data for Name: person_reperibility; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility (id, end_date, note, start_date, person_id, person_reperibility_type_id) FROM stdin;
\.


--
-- Data for Name: person_reperibility_days; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility_days (id, date, holiday_day, person_reperibility_id, reperibility_type) FROM stdin;
\.


--
-- Data for Name: person_reperibility_days_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility_days_history (id, _revision, _revision_type, date, holiday_day, person_reperibility_id, reperibility_type) FROM stdin;
\.


--
-- Data for Name: person_reperibility_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility_history (id, _revision, _revision_type, end_date, note, start_date, person_id, person_reperibility_type_id) FROM stdin;
\.


--
-- Data for Name: person_reperibility_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility_types (id, description) FROM stdin;
\.


--
-- Data for Name: person_reperibility_types_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_reperibility_types_history (id, _revision, _revision_type, description) FROM stdin;
\.


--
-- Data for Name: person_shift; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_shift (id, description, jolly, person_id) FROM stdin;
\.


--
-- Data for Name: person_shift_days; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_shift_days (id, date, person_shift_id, shift_type_id, shift_slot) FROM stdin;
\.


--
-- Data for Name: person_shift_shift_type; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_shift_shift_type (id, begin_date, end_date, personshifts_id, shifttypes_id) FROM stdin;
\.


--
-- Data for Name: person_years; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_years (id, remaining_minutes, remaining_vacation_days, year, person_id) FROM stdin;
\.


--
-- Data for Name: person_years_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY person_years_history (id, _revision, _revision_type, remaining_minutes, remaining_vacation_days, year, person_id) FROM stdin;
\.


--
-- Data for Name: persons; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons (id, badgenumber, born_date, email, name, number, oldid, other_surnames, surname, version, qualification_id, office_id, user_id, cnr_email, fax, mobile, telephone, department, head_office, room, want_email, birthday) FROM stdin;
\.


--
-- Data for Name: persons_competence_codes; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons_competence_codes (persons_id, competencecode_id) FROM stdin;
\.


--
-- Data for Name: persons_groups; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons_groups (persons_id, groups_id) FROM stdin;
\.


--
-- Data for Name: persons_groups_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons_groups_history (_revision, persons_id, groups_id, _revision_type) FROM stdin;
\.


--
-- Data for Name: persons_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons_history (id, _revision, _revision_type, badgenumber, born_date, email, name, number, oldid, other_surnames, surname, qualification_id, working_time_type_id, office_id, user_id, cnr_email, fax, mobile, telephone, department, head_office, room, want_email, birthday) FROM stdin;
\.


--
-- Data for Name: persons_working_time_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY persons_working_time_types (id, begin_date, end_date, person_id, working_time_type_id) FROM stdin;
\.


--
-- Data for Name: play_evolutions; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY play_evolutions (id, hash, applied_at, apply_script, revert_script, state, last_problem) FROM stdin;
1	afbc11a34c1e9c8358339af7f4e9ae7960d03970	2014-07-28 00:00:00	CREATE SEQUENCE seq_absence_type_groups\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE absence_type_groups (\n    id bigint DEFAULT nextval('seq_absence_type_groups'::regclass) NOT NULL,\n    accumulationbehaviour character varying(255),\n    accumulation_type character varying(255),\n    label character varying(255),\n    limit_in_minute integer,\n    minutes_excess boolean,\n    replacing_absence_type_id bigint\n);\nCREATE TABLE absence_type_groups_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    accumulationbehaviour character varying(255),\n    accumulation_type character varying(255),\n    label character varying(255),\n    limit_in_minute integer,\n    minutes_excess boolean,\n    replacing_absence_type_id bigint\n);\nCREATE SEQUENCE seq_absence_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE absence_types (\n    id bigint DEFAULT nextval('seq_absence_types'::regclass) NOT NULL,\n    certification_code character varying(255),\n    code character varying(255),\n    compensatory_rest boolean,\n    considered_week_end boolean,\n    description character varying(255),\n    ignore_stamping boolean,\n    internal_use boolean,\n    justified_time_at_work character varying(255),\n    meal_ticket_calculation boolean,\n    multiple_use boolean,\n    replacing_absence boolean,\n    valid_from date,\n    valid_to date,\n    absence_type_group_id bigint\n);\nCREATE TABLE absence_types_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    certification_code character varying(255),\n    code character varying(255),\n    compensatory_rest boolean,\n    considered_week_end boolean,\n    description character varying(255),\n    ignore_stamping boolean,\n    internal_use boolean,\n    justified_time_at_work character varying(255),\n    meal_ticket_calculation boolean,\n    multiple_use boolean,\n    replacing_absence boolean,\n    valid_from date,\n    valid_to date,\n    absence_type_group_id bigint\n);\nCREATE TABLE absence_types_qualifications (\n    absencetypes_id bigint NOT NULL,\n    qualifications_id bigint NOT NULL\n);\nCREATE TABLE absence_types_qualifications_history (\n    _revision integer NOT NULL,\n    absencetypes_id bigint NOT NULL,\n    qualifications_id bigint NOT NULL,\n    _revision_type smallint\n);\nCREATE SEQUENCE seq_absences\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE absences (\n    id bigint DEFAULT nextval('seq_absences'::regclass) NOT NULL,\n    absencerequest character varying(255),\n    absence_type_id bigint,\n    personday_id bigint NOT NULL\n);\nCREATE TABLE absences_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    absencerequest character varying(255),\n    absence_type_id bigint,\n    personday_id bigint\n);\nCREATE SEQUENCE seq_auth_users\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE auth_users (\n    id bigint DEFAULT nextval('seq_auth_users'::regclass) NOT NULL,\n    authip character varying(255),\n    authmod character varying(255),\n    authred character varying(255),\n    autsys character varying(255),\n    datacpas timestamp without time zone,\n    password character varying(255),\n    passwordmd5 character varying(255),\n    scadenzapassword smallint,\n    ultimamodifica timestamp without time zone,\n    username character varying(255)\n);\nCREATE SEQUENCE seq_badge_readers\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE badge_readers (\n    id bigint DEFAULT nextval('seq_badge_readers'::regclass) NOT NULL,\n    code character varying(255),\n    description character varying(255),\n    enabled boolean NOT NULL,\n    location character varying(255)\n);\nCREATE TABLE badge_readers_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    code character varying(255),\n    description character varying(255),\n    enabled boolean,\n    location character varying(255)\n);\nCREATE SEQUENCE seq_competence_codes\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE competence_codes (\n    id bigint DEFAULT nextval('seq_competence_codes'::regclass) NOT NULL,\n    code character varying(255),\n    codetopresence character varying(255),\n    description character varying(255),\n    inactive boolean NOT NULL\n);\nCREATE SEQUENCE seq_competences\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE competences (\n    id bigint DEFAULT nextval('seq_competences'::regclass) NOT NULL,\n    month integer NOT NULL,\n    reason character varying(1000),\n    valueapproved integer NOT NULL,\n    valuerequest integer NOT NULL,\n    year integer NOT NULL,\n    competence_code_id bigint NOT NULL,\n    person_id bigint\n);\nCREATE SEQUENCE seq_configurations\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE configurations (\n    id bigint DEFAULT nextval('seq_configurations'::regclass) NOT NULL,\n    addworkingtimeinexcess boolean NOT NULL,\n    begin_date timestamp without time zone,\n    calculateintervaltimewithoutreturnfromintevaltime boolean NOT NULL,\n    caninsertmoreabsencecodeinday boolean NOT NULL,\n    canpeopleautodeclareabsences boolean NOT NULL,\n    canpeopleautodeclareworkingtime boolean NOT NULL,\n    canpeopleusewebstamping boolean NOT NULL,\n    capacityfoureight integer,\n    capacityonethree integer,\n    dayexpiryvacationpastyear integer,\n    day_of_patron integer,\n    email_to_contact character varying(255),\n    end_date timestamp without time zone,\n    holydaysandvacationsoverpermitted boolean NOT NULL,\n    hourmaxtocalculateworktime integer,\n    ignoreworkingtimewithabsencecode boolean NOT NULL,\n    in_use boolean,\n    init_use_program timestamp without time zone,\n    insertandmodifyworkingtimewithplustoreduceatrealworkingtime boolean NOT NULL,\n    institute_name character varying(255),\n    isfirstorlastmissiondayaholiday boolean NOT NULL,\n    isholidayinmissionaworkingday boolean NOT NULL,\n    isintervaltimecutfromworkingtime boolean NOT NULL,\n    islastdaybeforeeasterentire boolean NOT NULL,\n    islastdaybeforexmasentire boolean NOT NULL,\n    islastdayoftheyearentire boolean NOT NULL,\n    ismealtimeshorterthanminimum boolean NOT NULL,\n    maxrecoverydaysfournine integer,\n    maxrecoverydaysonethree integer,\n    maximumovertimehours integer,\n    mealticketassignedwithmealtimereal boolean NOT NULL,\n    mealticketassignedwithreasonmealtime boolean NOT NULL,\n    mealtime integer,\n    minimumremainingtimetohaverecoveryday integer,\n    monthexpirerecoverydaysfournine integer,\n    monthexpirerecoverydaysonethree integer,\n    monthexpiryvacationpastyear integer,\n    month_of_patron integer,\n    numberofviewingcouplecolumn integer NOT NULL,\n    password_to_presence character varying(255),\n    path_to_save_presence_situation character varying(255),\n    residual integer,\n    seat_code integer,\n    textformonthlysituation character varying(255),\n    url_to_presence character varying(255),\n    user_to_presence character varying(255),\n    workingtime integer,\n    workingtimetohavemealticket integer,\n    mealtimestarthour integer,\n    mealtimestartminute integer,\n    mealtimeendminute integer,\n    mealtimeendhour integer\n);\nCREATE TABLE configurations_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    addworkingtimeinexcess boolean,\n    begin_date timestamp without time zone,\n    calculateintervaltimewithoutreturnfromintevaltime boolean,\n    caninsertmoreabsencecodeinday boolean,\n    canpeopleautodeclareabsences boolean,\n    canpeopleautodeclareworkingtime boolean,\n    canpeopleusewebstamping boolean,\n    capacityfoureight integer,\n    capacityonethree integer,\n    dayexpiryvacationpastyear integer,\n    day_of_patron integer,\n    email_to_contact character varying(255),\n    end_date timestamp without time zone,\n    holydaysandvacationsoverpermitted boolean,\n    hourmaxtocalculateworktime integer,\n    ignoreworkingtimewithabsencecode boolean,\n    in_use boolean,\n    init_use_program timestamp without time zone,\n    insertandmodifyworkingtimewithplustoreduceatrealworkingtime boolean,\n    institute_name character varying(255),\n    isfirstorlastmissiondayaholiday boolean,\n    isholidayinmissionaworkingday boolean,\n    isintervaltimecutfromworkingtime boolean,\n    islastdaybeforeeasterentire boolean,\n    islastdaybeforexmasentire boolean,\n    islastdayoftheyearentire boolean,\n    ismealtimeshorterthanminimum boolean,\n    maxrecoverydaysfournine integer,\n    maxrecoverydaysonethree integer,\n    maximumovertimehours integer,\n    mealticketassignedwithmealtimereal boolean,\n    mealticketassignedwithreasonmealtime boolean,\n    mealtime integer,\n    minimumremainingtimetohaverecoveryday integer,\n    monthexpirerecoverydaysfournine integer,\n    monthexpirerecoverydaysonethree integer,\n    monthexpiryvacationpastyear integer,\n    month_of_patron integer,\n    numberofviewingcouplecolumn integer,\n    password_to_presence character varying(255),\n    path_to_save_presence_situation character varying(255),\n    residual integer,\n    seat_code integer,\n    textformonthlysituation character varying(255),\n    url_to_presence character varying(255),\n    user_to_presence character varying(255),\n    workingtime integer,\n    workingtimetohavemealticket integer,\n    mealtimeendhour integer,\n    mealtimeendminute integer,\n    mealtimestarthour integer,\n    mealtimestartminute integer\n);\nCREATE SEQUENCE seq_contact_data\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE contact_data (\n    id bigint DEFAULT nextval('seq_contact_data'::regclass) NOT NULL,\n    email character varying(255),\n    fax character varying(255),\n    mobile character varying(255),\n    telephone character varying(255),\n    person_id bigint\n);\nCREATE TABLE contact_data_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    email character varying(255),\n    fax character varying(255),\n    mobile character varying(255),\n    telephone character varying(255),\n    person_id bigint\n);\nCREATE SEQUENCE seq_contracts\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE contracts (\n    id bigint DEFAULT nextval('seq_contracts'::regclass) NOT NULL,\n    begin_contract date,\n    end_contract date,\n    expire_contract date,\n    oncertificate boolean NOT NULL,\n    person_id bigint\n);\nCREATE SEQUENCE seq_groups\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE groups (\n    id bigint DEFAULT nextval('seq_groups'::regclass) NOT NULL,\n    description character varying(255)\n);\nCREATE TABLE groups_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255)\n);\nCREATE SEQUENCE seq_initialization_absences\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE initialization_absences (\n    id bigint DEFAULT nextval('seq_initialization_absences'::regclass) NOT NULL,\n    absencedays integer,\n    date date,\n    recovery_days integer,\n    absencetype_id bigint NOT NULL,\n    person_id bigint NOT NULL\n);\nCREATE TABLE initialization_absences_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    absencedays integer,\n    date date,\n    recovery_days integer,\n    absencetype_id bigint,\n    person_id bigint\n);\nCREATE SEQUENCE seq_initialization_times\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE initialization_times (\n    id bigint DEFAULT nextval('seq_initialization_times'::regclass) NOT NULL,\n    date date,\n    residualminutescurrentyear integer,\n    residualminutespastyear integer,\n    person_id bigint NOT NULL\n);\nCREATE TABLE initialization_times_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    date date,\n    residualminutescurrentyear integer,\n    residualminutespastyear integer,\n    person_id bigint\n);\nCREATE SEQUENCE seq_locations\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE locations (\n    id bigint DEFAULT nextval('seq_locations'::regclass) NOT NULL,\n    department character varying(255),\n    headoffice character varying(255),\n    room character varying(255),\n    person_id bigint\n);\nCREATE SEQUENCE seq_options\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE options (\n    id bigint DEFAULT nextval('seq_options'::regclass) NOT NULL,\n    easterchristmas boolean,\n    adjustrange boolean,\n    adjustrangeday boolean,\n    autorange boolean,\n    date timestamp without time zone,\n    expiredvacationday boolean,\n    expiredvacationmonth boolean,\n    otherheadoffice character varying(255),\n    patronday boolean,\n    patronmonth boolean,\n    recoveryap character varying(255),\n    recoverymonth boolean,\n    tipo_ferie_gen character varying(255),\n    tipo_permieg character varying(255),\n    vacationtype character varying(255),\n    vacationtypep character varying(255)\n);\nCREATE SEQUENCE seq_permissions\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE permissions (\n    id bigint DEFAULT nextval('seq_permissions'::regclass) NOT NULL,\n    description character varying(255)\n);\nCREATE TABLE permissions_groups (\n    permissions_id bigint NOT NULL,\n    groups_id bigint NOT NULL\n);\nCREATE TABLE permissions_groups_history (\n    _revision integer NOT NULL,\n    permissions_id bigint NOT NULL,\n    groups_id bigint NOT NULL,\n    _revision_type smallint\n);\nCREATE TABLE permissions_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255)\n);\nCREATE SEQUENCE seq_person_children\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_children (\n    id bigint DEFAULT nextval('seq_person_children'::regclass) NOT NULL,\n    borndate bytea,\n    name character varying(255),\n    surname character varying(255),\n    person_id bigint\n);\nCREATE TABLE person_children_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    borndate bytea,\n    name character varying(255),\n    surname character varying(255),\n    person_id bigint\n);\nCREATE SEQUENCE seq_person_days\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_days (\n    id bigint DEFAULT nextval('seq_person_days'::regclass) NOT NULL,\n    date date,\n    difference integer,\n    is_ticket_available boolean,\n    is_ticket_forced_by_admin boolean,\n    is_time_at_work_auto_certificated boolean,\n    is_working_in_another_place boolean,\n    modification_type character varying(255),\n    progressive integer,\n    time_at_work integer,\n    person_id bigint NOT NULL\n);\nCREATE TABLE person_days_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    date date,\n    difference integer,\n    is_ticket_available boolean,\n    is_ticket_forced_by_admin boolean,\n    is_time_at_work_auto_certificated boolean,\n    is_working_in_another_place boolean,\n    modification_type character varying(255),\n    progressive integer,\n    time_at_work integer,\n    person_id bigint\n);\nCREATE SEQUENCE seq_person_days_in_trouble\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_days_in_trouble (\n    id bigint DEFAULT nextval('seq_person_days_in_trouble'::regclass) NOT NULL,\n    cause character varying(255),\n    emailsent boolean NOT NULL,\n    fixed boolean NOT NULL,\n    personday_id bigint NOT NULL\n);\nCREATE TABLE person_days_in_trouble_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    cause character varying(255),\n    emailsent boolean,\n    fixed boolean,\n    personday_id bigint\n);\nCREATE SEQUENCE seq_person_hour_for_overtime\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_hour_for_overtime (\n    id bigint DEFAULT nextval('seq_person_hour_for_overtime'::regclass) NOT NULL,\n    numberofhourforovertime integer,\n    person_id bigint\n);\nCREATE SEQUENCE seq_person_months\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_months (\n    id bigint DEFAULT nextval('seq_person_months'::regclass) NOT NULL,\n    compensatory_rest_in_minutes integer,\n    month integer,\n    progressiveatendofmonthinminutes integer,\n    recuperi_ore_da_anno_precedente integer,\n    remaining_minute_past_year_taken integer,\n    residual_past_year integer,\n    riposi_compensativi_da_anno_corrente integer,\n    riposi_compensativi_da_anno_precedente integer,\n    riposi_compensativi_da_inizializzazione integer,\n    straordinari integer,\n    total_remaining_minutes integer,\n    year integer,\n    person_id bigint NOT NULL\n);\nCREATE TABLE person_months_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    compensatory_rest_in_minutes integer,\n    month integer,\n    progressiveatendofmonthinminutes integer,\n    recuperi_ore_da_anno_precedente integer,\n    remaining_minute_past_year_taken integer,\n    residual_past_year integer,\n    riposi_compensativi_da_anno_corrente integer,\n    riposi_compensativi_da_anno_precedente integer,\n    riposi_compensativi_da_inizializzazione integer,\n    straordinari integer,\n    total_remaining_minutes integer,\n    year integer,\n    person_id bigint\n);\nCREATE TABLE person_reperibility (\n    id bigint NOT NULL,\n    end_date date,\n    note character varying(255),\n    start_date date,\n    person_id bigint,\n    person_reperibility_type_id bigint\n);\nCREATE TABLE person_reperibility_days (\n    id bigint NOT NULL,\n    date date,\n    holiday_day boolean,\n    person_reperibility_id bigint NOT NULL,\n    reperibility_type bigint\n);\nCREATE TABLE person_reperibility_days_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    date date,\n    holiday_day boolean,\n    person_reperibility_id bigint,\n    reperibility_type bigint\n);\nCREATE TABLE person_reperibility_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    end_date date,\n    note character varying(255),\n    start_date date,\n    person_id bigint,\n    person_reperibility_type_id bigint\n);\nCREATE TABLE person_reperibility_types (\n    id bigint NOT NULL,\n    description character varying(255)\n);\nCREATE TABLE person_reperibility_types_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255)\n);\nCREATE TABLE person_shift (\n    id bigint NOT NULL,\n    description character varying(255),\n    jolly boolean NOT NULL,\n    person_id bigint NOT NULL\n);\nCREATE TABLE person_shift_days (\n    id bigint NOT NULL,\n    date date,\n    person_shift_id bigint NOT NULL,\n    shift_time_table_id bigint,\n    shift_type_id bigint\n);\nCREATE TABLE person_shift_shift_type (\n    id bigint NOT NULL,\n    begin_date date,\n    end_date date,\n    personshifts_id bigint,\n    shifttypes_id bigint\n);\nCREATE SEQUENCE seq_person_years\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE person_years (\n    id bigint DEFAULT nextval('seq_person_years'::regclass) NOT NULL,\n    remaining_minutes integer,\n    remaining_vacation_days integer,\n    year integer,\n    person_id bigint NOT NULL\n);\nCREATE TABLE person_years_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    remaining_minutes integer,\n    remaining_vacation_days integer,\n    year integer,\n    person_id bigint\n);\nCREATE SEQUENCE seq_persons\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE persons (\n    id bigint DEFAULT nextval('seq_persons'::regclass) NOT NULL,\n    badgenumber character varying(255),\n    born_date timestamp without time zone,\n    email character varying(255),\n    name character varying(255),\n    number integer,\n    oldid bigint,\n    other_surnames character varying(255),\n    password character varying(255),\n    surname character varying(255),\n    username character varying(255),\n    version integer,\n    qualification_id bigint,\n    working_time_type_id bigint\n);\nCREATE TABLE persons_competence_codes (\n    persons_id bigint NOT NULL,\n    competencecode_id bigint NOT NULL\n);\nCREATE TABLE persons_groups (\n    persons_id bigint NOT NULL,\n    groups_id bigint NOT NULL\n);\nCREATE TABLE persons_groups_history (\n    _revision integer NOT NULL,\n    persons_id bigint NOT NULL,\n    groups_id bigint NOT NULL,\n    _revision_type smallint\n);\nCREATE TABLE persons_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    badgenumber character varying(255),\n    born_date timestamp without time zone,\n    email character varying(255),\n    name character varying(255),\n    number integer,\n    oldid bigint,\n    other_surnames character varying(255),\n    password character varying(255),\n    surname character varying(255),\n    username character varying(255),\n    qualification_id bigint,\n    working_time_type_id bigint\n);\nCREATE TABLE persons_permissions (\n    users_id bigint NOT NULL,\n    permissions_id bigint NOT NULL\n);\nCREATE TABLE persons_permissions_history (\n    _revision integer NOT NULL,\n    users_id bigint NOT NULL,\n    permissions_id bigint NOT NULL,\n    _revision_type smallint\n);\nCREATE SEQUENCE seq_qualifications\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE qualifications (\n    id bigint DEFAULT nextval('seq_qualifications'::regclass) NOT NULL,\n    description character varying(255),\n    qualification integer NOT NULL\n);\nCREATE TABLE qualifications_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255),\n    qualification integer\n);\nCREATE SEQUENCE seq_revinfo\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE revinfo (\n    rev integer DEFAULT nextval('seq_revinfo'::regclass) NOT NULL,\n    revtstmp bigint\n);\nCREATE SEQUENCE seq_person_reperibility\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_person_reperibility_days\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_person_reperibility_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_person_shift\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_person_shift_days\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_person_shift_shift_type\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_shift_cancelled\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_shift_time_table\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_shift_type\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_stamp_modification_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_stamp_profiles\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_stamp_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_stampings\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_total_overtime\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_vacation_codes\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_vacation_periods\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_valuable_competences\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_web_stamping_address\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_working_time_type_days\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_working_time_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE SEQUENCE seq_year_recaps\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE\n    CACHE 1;\nCREATE TABLE shift_cancelled (\n    id bigint NOT NULL,\n    date date,\n    shift_type_id bigint NOT NULL\n);\nCREATE TABLE shift_time_table (\n    id bigint NOT NULL,\n    description character varying(255),\n    endshift timestamp without time zone,\n    startshift timestamp without time zone\n);\nCREATE TABLE shift_type (\n    id bigint NOT NULL,\n    description character varying(255),\n    type character varying(255)\n);\nCREATE TABLE stamp_modification_types (\n    id bigint DEFAULT nextval('seq_stamp_modification_types'::regclass) NOT NULL,\n    code character varying(255),\n    description character varying(255)\n);\nCREATE TABLE stamp_modification_types_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    code character varying(255),\n    description character varying(255)\n);\nCREATE TABLE stamp_profiles (\n    id bigint DEFAULT nextval('seq_stamp_profiles'::regclass) NOT NULL,\n    end_to date,\n    fixedworkingtime boolean NOT NULL,\n    start_from date,\n    person_id bigint NOT NULL\n);\nCREATE TABLE stamp_types (\n    id bigint DEFAULT nextval('seq_stamp_types'::regclass) NOT NULL,\n    code character varying(255),\n    description character varying(255),\n    identifier character varying(255)\n);\nCREATE TABLE stamp_types_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    code character varying(255),\n    description character varying(255),\n    identifier character varying(255)\n);\nCREATE TABLE stampings (\n    id bigint DEFAULT nextval('seq_stampings'::regclass) NOT NULL,\n    date timestamp without time zone,\n    marked_by_admin boolean,\n    note character varying(255),\n    way character varying(255),\n    badge_reader_id bigint,\n    personday_id bigint NOT NULL,\n    stamp_modification_type_id bigint,\n    stamp_type_id bigint\n);\nCREATE TABLE stampings_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    date timestamp without time zone,\n    marked_by_admin boolean,\n    note character varying(255),\n    way character varying(255),\n    badge_reader_id bigint,\n    personday_id bigint,\n    stamp_modification_type_id bigint,\n    stamp_type_id bigint\n);\nCREATE TABLE total_overtime (\n    id bigint DEFAULT nextval('seq_total_overtime'::regclass) NOT NULL,\n    date date,\n    numberofhours integer,\n    year integer\n);\nCREATE TABLE vacation_codes (\n    id bigint DEFAULT nextval('seq_vacation_codes'::regclass) NOT NULL,\n    description character varying(255),\n    permission_days integer,\n    vacation_days integer\n);\nCREATE TABLE vacation_codes_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255),\n    permission_days integer,\n    vacation_days integer\n);\nCREATE TABLE vacation_periods (\n    id bigint DEFAULT nextval('seq_vacation_periods'::regclass) NOT NULL,\n    begin_from date,\n    end_to date,\n    contract_id bigint NOT NULL,\n    vacation_codes_id bigint NOT NULL\n);\nCREATE TABLE vacation_periods_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    begin_from date,\n    end_to date,\n    contract_id bigint,\n    vacation_codes_id bigint\n);\nCREATE TABLE valuable_competences (\n    id bigint DEFAULT nextval('seq_valuable_competences'::regclass) NOT NULL,\n    codicecomp character varying(255),\n    descrizione character varying(255),\n    person_id bigint\n);\nCREATE TABLE web_stamping_address (\n    id bigint DEFAULT nextval('seq_web_stamping_address'::regclass) NOT NULL,\n    webaddresstype character varying(255),\n    confparameters_id bigint\n);\nCREATE TABLE web_stamping_address_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    webaddresstype character varying(255),\n    confparameters_id bigint\n);\nCREATE TABLE working_time_type_days (\n    id bigint DEFAULT nextval('seq_working_time_type_days'::regclass) NOT NULL,\n    breaktickettime integer,\n    dayofweek integer NOT NULL,\n    holiday boolean NOT NULL,\n    mealtickettime integer,\n    timemealfrom integer,\n    timemealto integer,\n    timeslotentrancefrom integer,\n    timeslotentranceto integer,\n    timeslotexitfrom integer,\n    timeslotexitto integer,\n    workingtime integer,\n    working_time_type_id bigint NOT NULL\n);\nCREATE TABLE working_time_type_days_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    breaktickettime integer,\n    dayofweek integer,\n    holiday boolean,\n    mealtickettime integer,\n    timemealfrom integer,\n    timemealto integer,\n    timeslotentrancefrom integer,\n    timeslotentranceto integer,\n    timeslotexitfrom integer,\n    timeslotexitto integer,\n    workingtime integer,\n    working_time_type_id bigint\n);\nCREATE TABLE working_time_types (\n    id bigint DEFAULT nextval('seq_working_time_types'::regclass) NOT NULL,\n    description character varying(255) NOT NULL,\n    shift boolean NOT NULL\n);\nCREATE TABLE working_time_types_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    description character varying(255),\n    shift boolean\n);\nCREATE TABLE year_recaps (\n    id bigint DEFAULT nextval('seq_year_recaps'::regclass) NOT NULL,\n    lastmodified timestamp without time zone,\n    overtime integer,\n    overtimeap integer,\n    recg integer,\n    recgap integer,\n    recguap integer,\n    recm integer,\n    remaining integer,\n    remainingap integer,\n    year smallint,\n    person_id bigint\n);\nALTER TABLE ONLY absence_type_groups_history\n    ADD CONSTRAINT absence_type_groups_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY absence_type_groups\n    ADD CONSTRAINT absence_type_groups_pkey PRIMARY KEY (id);\nALTER TABLE ONLY absence_types_history\n    ADD CONSTRAINT absence_types_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY absence_types\n    ADD CONSTRAINT absence_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY absence_types_qualifications_history\n    ADD CONSTRAINT absence_types_qualifications_history_pkey PRIMARY KEY (_revision, absencetypes_id, qualifications_id);\nALTER TABLE ONLY absences_history\n    ADD CONSTRAINT absences_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY absences\n    ADD CONSTRAINT absences_pkey PRIMARY KEY (id);\nALTER TABLE ONLY auth_users\n    ADD CONSTRAINT auth_users_pkey PRIMARY KEY (id);\nALTER TABLE ONLY badge_readers_history\n    ADD CONSTRAINT badge_readers_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY badge_readers\n    ADD CONSTRAINT badge_readers_pkey PRIMARY KEY (id);\nALTER TABLE ONLY competence_codes\n    ADD CONSTRAINT competence_codes_pkey PRIMARY KEY (id);\nALTER TABLE ONLY competences\n    ADD CONSTRAINT competences_pkey PRIMARY KEY (id);\nALTER TABLE ONLY configurations_history\n    ADD CONSTRAINT configurations_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY configurations\n    ADD CONSTRAINT configurations_pkey PRIMARY KEY (id);\nALTER TABLE ONLY contact_data_history\n    ADD CONSTRAINT contact_data_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY contact_data\n    ADD CONSTRAINT contact_data_pkey PRIMARY KEY (id);\nALTER TABLE ONLY contracts\n    ADD CONSTRAINT contracts_pkey PRIMARY KEY (id);\nALTER TABLE ONLY groups_history\n    ADD CONSTRAINT groups_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY groups\n    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);\nALTER TABLE ONLY initialization_absences_history\n    ADD CONSTRAINT initialization_absences_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY initialization_absences\n    ADD CONSTRAINT initialization_absences_pkey PRIMARY KEY (id);\nALTER TABLE ONLY initialization_times_history\n    ADD CONSTRAINT initialization_times_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY initialization_times\n    ADD CONSTRAINT initialization_times_pkey PRIMARY KEY (id);\nALTER TABLE ONLY locations\n    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);\nALTER TABLE ONLY options\n    ADD CONSTRAINT options_pkey PRIMARY KEY (id);\nALTER TABLE ONLY permissions_groups_history\n    ADD CONSTRAINT permissions_groups_history_pkey PRIMARY KEY (_revision, permissions_id, groups_id);\nALTER TABLE ONLY permissions_history\n    ADD CONSTRAINT permissions_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY permissions\n    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_children_history\n    ADD CONSTRAINT person_children_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_children\n    ADD CONSTRAINT person_children_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_days_history\n    ADD CONSTRAINT person_days_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_days_in_trouble_history\n    ADD CONSTRAINT person_days_in_trouble_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_days_in_trouble\n    ADD CONSTRAINT person_days_in_trouble_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_days\n    ADD CONSTRAINT person_days_person_id_date_key UNIQUE (person_id, date);\nALTER TABLE ONLY person_days\n    ADD CONSTRAINT person_days_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_hour_for_overtime\n    ADD CONSTRAINT person_hour_for_overtime_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_months_history\n    ADD CONSTRAINT person_months_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_months\n    ADD CONSTRAINT person_months_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_reperibility_days_history\n    ADD CONSTRAINT person_reperibility_days_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_reperibility_days\n    ADD CONSTRAINT person_reperibility_days_person_reperibility_id_date_key UNIQUE (person_reperibility_id, date);\nALTER TABLE ONLY person_reperibility_days\n    ADD CONSTRAINT person_reperibility_days_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_reperibility_history\n    ADD CONSTRAINT person_reperibility_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_reperibility\n    ADD CONSTRAINT person_reperibility_person_id_key UNIQUE (person_id);\nALTER TABLE ONLY person_reperibility\n    ADD CONSTRAINT person_reperibility_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_reperibility_types_history\n    ADD CONSTRAINT person_reperibility_types_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_reperibility_types\n    ADD CONSTRAINT person_reperibility_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_shift_days\n    ADD CONSTRAINT person_shift_days_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_shift\n    ADD CONSTRAINT person_shift_person_id_key UNIQUE (person_id);\nALTER TABLE ONLY person_shift\n    ADD CONSTRAINT person_shift_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_shift_shift_type\n    ADD CONSTRAINT person_shift_shift_type_pkey PRIMARY KEY (id);\nALTER TABLE ONLY person_years_history\n    ADD CONSTRAINT person_years_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY person_years\n    ADD CONSTRAINT person_years_pkey PRIMARY KEY (id);\nALTER TABLE ONLY persons_groups_history\n    ADD CONSTRAINT persons_groups_history_pkey PRIMARY KEY (_revision, persons_id, groups_id);\nALTER TABLE ONLY persons_history\n    ADD CONSTRAINT persons_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY persons_permissions_history\n    ADD CONSTRAINT persons_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id);\nALTER TABLE ONLY persons\n    ADD CONSTRAINT persons_pkey PRIMARY KEY (id);\nALTER TABLE ONLY qualifications_history\n    ADD CONSTRAINT qualifications_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY qualifications\n    ADD CONSTRAINT qualifications_pkey PRIMARY KEY (id);\nALTER TABLE ONLY revinfo\n    ADD CONSTRAINT revinfo_pkey PRIMARY KEY (rev);\nALTER TABLE ONLY shift_cancelled\n    ADD CONSTRAINT shift_cancelled_pkey PRIMARY KEY (id);\nALTER TABLE ONLY shift_time_table\n    ADD CONSTRAINT shift_time_table_pkey PRIMARY KEY (id);\nALTER TABLE ONLY shift_type\n    ADD CONSTRAINT shift_type_pkey PRIMARY KEY (id);\nALTER TABLE ONLY stamp_modification_types_history\n    ADD CONSTRAINT stamp_modification_types_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY stamp_modification_types\n    ADD CONSTRAINT stamp_modification_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY stamp_profiles\n    ADD CONSTRAINT stamp_profiles_pkey PRIMARY KEY (id);\nALTER TABLE ONLY stamp_types_history\n    ADD CONSTRAINT stamp_types_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY stamp_types\n    ADD CONSTRAINT stamp_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY stampings_history\n    ADD CONSTRAINT stampings_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY stampings\n    ADD CONSTRAINT stampings_pkey PRIMARY KEY (id);\nALTER TABLE ONLY total_overtime\n    ADD CONSTRAINT total_overtime_pkey PRIMARY KEY (id);\nALTER TABLE ONLY vacation_codes_history\n    ADD CONSTRAINT vacation_codes_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY vacation_codes\n    ADD CONSTRAINT vacation_codes_pkey PRIMARY KEY (id);\nALTER TABLE ONLY vacation_periods_history\n    ADD CONSTRAINT vacation_periods_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY vacation_periods\n    ADD CONSTRAINT vacation_periods_pkey PRIMARY KEY (id);\nALTER TABLE ONLY valuable_competences\n    ADD CONSTRAINT valuable_competences_pkey PRIMARY KEY (id);\nALTER TABLE ONLY web_stamping_address_history\n    ADD CONSTRAINT web_stamping_address_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY web_stamping_address\n    ADD CONSTRAINT web_stamping_address_pkey PRIMARY KEY (id);\nALTER TABLE ONLY working_time_type_days_history\n    ADD CONSTRAINT working_time_type_days_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY working_time_type_days\n    ADD CONSTRAINT working_time_type_days_pkey PRIMARY KEY (id);\nALTER TABLE ONLY working_time_types_history\n    ADD CONSTRAINT working_time_types_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY working_time_types\n    ADD CONSTRAINT working_time_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY year_recaps\n    ADD CONSTRAINT year_recaps_pkey PRIMARY KEY (id);\nALTER TABLE ONLY absences_history\n    ADD CONSTRAINT fk10e41b2bd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_reperibility_history\n    ADD CONSTRAINT fk160d77e7d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY absence_types_qualifications\n    ADD CONSTRAINT fk16765acbce1b821 FOREIGN KEY (qualifications_id) REFERENCES qualifications(id);\nALTER TABLE ONLY absence_types_qualifications\n    ADD CONSTRAINT fk16765acd966a951 FOREIGN KEY (absencetypes_id) REFERENCES absence_types(id);\nALTER TABLE ONLY working_time_type_days_history\n    ADD CONSTRAINT fk18bdcd6dd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY vacation_periods_history\n    ADD CONSTRAINT fk1fe1fac5d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_hour_for_overtime\n    ADD CONSTRAINT fk20975c68e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY persons_competence_codes\n    ADD CONSTRAINT fk218a96913c1ea3e FOREIGN KEY (competencecode_id) REFERENCES competence_codes(id);\nALTER TABLE ONLY persons_competence_codes\n    ADD CONSTRAINT fk218a9691dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);\nALTER TABLE ONLY permissions_history\n    ADD CONSTRAINT fk277bbd9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY vacation_codes_history\n    ADD CONSTRAINT fk27e84399d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_shift_days\n    ADD CONSTRAINT fk3075811e3df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);\nALTER TABLE ONLY person_shift_days\n    ADD CONSTRAINT fk3075811e86585224 FOREIGN KEY (shift_time_table_id) REFERENCES shift_time_table(id);\nALTER TABLE ONLY person_shift_days\n    ADD CONSTRAINT fk3075811eda784c2b FOREIGN KEY (person_shift_id) REFERENCES person_shift(id);\nALTER TABLE ONLY shift_cancelled\n    ADD CONSTRAINT fk3f7a55d43df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);\nALTER TABLE ONLY initialization_times\n    ADD CONSTRAINT fk4094eae7e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY person_days_in_trouble\n    ADD CONSTRAINT fk4b732cdbfbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);\nALTER TABLE ONLY contact_data\n    ADD CONSTRAINT fk4c241869e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY permissions_groups_history\n    ADD CONSTRAINT fk4d19b244d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY vacation_periods\n    ADD CONSTRAINT fk5170df704649fe84 FOREIGN KEY (vacation_codes_id) REFERENCES vacation_codes(id);\nALTER TABLE ONLY vacation_periods\n    ADD CONSTRAINT fk5170df70e7a7b1be FOREIGN KEY (contract_id) REFERENCES contracts(id);\nALTER TABLE ONLY initialization_absences\n    ADD CONSTRAINT fk53ee32958c3d68d6 FOREIGN KEY (absencetype_id) REFERENCES absence_types(id);\nALTER TABLE ONLY initialization_absences\n    ADD CONSTRAINT fk53ee3295e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY valuable_competences\n    ADD CONSTRAINT fk5842d3d9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY stamp_modification_types_history\n    ADD CONSTRAINT fk5c3f2f67d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY absences\n    ADD CONSTRAINT fk6674c5d65b5f15b1 FOREIGN KEY (absence_type_id) REFERENCES absence_types(id);\nALTER TABLE ONLY absences\n    ADD CONSTRAINT fk6674c5d6fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);\nALTER TABLE ONLY qualifications_history\n    ADD CONSTRAINT fk68e007b9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_children\n    ADD CONSTRAINT fk6ace3a9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY working_time_type_days\n    ADD CONSTRAINT fk74fdda1835555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);\nALTER TABLE ONLY person_shift_shift_type\n    ADD CONSTRAINT fk7757fc5e29b090bd FOREIGN KEY (personshifts_id) REFERENCES person_shift(id);\nALTER TABLE ONLY person_shift_shift_type\n    ADD CONSTRAINT fk7757fc5ebbfca55b FOREIGN KEY (shifttypes_id) REFERENCES shift_type(id);\nALTER TABLE ONLY stampings\n    ADD CONSTRAINT fk785e8f1435175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types(id);\nALTER TABLE ONLY stampings\n    ADD CONSTRAINT fk785e8f148868391d FOREIGN KEY (badge_reader_id) REFERENCES badge_readers(id);\nALTER TABLE ONLY stampings\n    ADD CONSTRAINT fk785e8f14932966bd FOREIGN KEY (stamp_type_id) REFERENCES stamp_types(id);\nALTER TABLE ONLY stampings\n    ADD CONSTRAINT fk785e8f14fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);\nALTER TABLE ONLY persons_permissions_history\n    ADD CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_reperibility\n    ADD CONSTRAINT fk7ab49e924e498a6e FOREIGN KEY (person_reperibility_type_id) REFERENCES person_reperibility_types(id);\nALTER TABLE ONLY person_reperibility\n    ADD CONSTRAINT fk7ab49e92e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY permissions_groups\n    ADD CONSTRAINT fk7abb85ef34428ea9 FOREIGN KEY (permissions_id) REFERENCES permissions(id);\nALTER TABLE ONLY permissions_groups\n    ADD CONSTRAINT fk7abb85ef522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);\nALTER TABLE ONLY absence_type_groups_history\n    ADD CONSTRAINT fk82ea23ccd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY stamp_profiles\n    ADD CONSTRAINT fk8379a4e6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY web_stamping_address\n    ADD CONSTRAINT fk84df567fac97433e FOREIGN KEY (confparameters_id) REFERENCES configurations(id);\nALTER TABLE ONLY web_stamping_address_history\n    ADD CONSTRAINT fk84ebf2d4d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_days_in_trouble_history\n    ADD CONSTRAINT fk85e1ad30d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_months_history\n    ADD CONSTRAINT fk863472d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY initialization_times_history\n    ADD CONSTRAINT fk879a9f3cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_days_history\n    ADD CONSTRAINT fk8fa3d556d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY working_time_types_history\n    ADD CONSTRAINT fk94fa58aad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY year_recaps\n    ADD CONSTRAINT fk9afc5dd6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY persons_groups_history\n    ADD CONSTRAINT fka577bbcad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_reperibility_days_history\n    ADD CONSTRAINT fka8bd39d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_reperibility_days\n    ADD CONSTRAINT fkb20b55e41df6de9 FOREIGN KEY (person_reperibility_id) REFERENCES person_reperibility(id);\nALTER TABLE ONLY person_reperibility_days\n    ADD CONSTRAINT fkb20b55e47d5fd20c FOREIGN KEY (reperibility_type) REFERENCES person_reperibility_types(id);\nALTER TABLE ONLY groups_history\n    ADD CONSTRAINT fkb7d05129d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY locations\n    ADD CONSTRAINT fkb8a4575ee7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY person_months\n    ADD CONSTRAINT fkbb6c161de7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY competences\n    ADD CONSTRAINT fkbe7a61ca62728bb1 FOREIGN KEY (competence_code_id) REFERENCES competence_codes(id);\nALTER TABLE ONLY competences\n    ADD CONSTRAINT fkbe7a61cae7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY absence_types_qualifications_history\n    ADD CONSTRAINT fkbfc8501d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_reperibility_types_history\n    ADD CONSTRAINT fkc5a3e3e1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY stamp_types_history\n    ADD CONSTRAINT fkcc549f52d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY stampings_history\n    ADD CONSTRAINT fkcd87c669d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_years_history\n    ADD CONSTRAINT fkd30e49c1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY persons\n    ADD CONSTRAINT fkd78fcfbe35555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);\nALTER TABLE ONLY persons\n    ADD CONSTRAINT fkd78fcfbe786a4ab6 FOREIGN KEY (qualification_id) REFERENCES qualifications(id);\nALTER TABLE ONLY configurations_history\n    ADD CONSTRAINT fkda8f2892d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY persons_groups\n    ADD CONSTRAINT fkdda67575522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);\nALTER TABLE ONLY persons_groups\n    ADD CONSTRAINT fkdda67575dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);\nALTER TABLE ONLY contact_data_history\n    ADD CONSTRAINT fke66d2abed54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY person_days\n    ADD CONSTRAINT fke69adb01e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY contracts\n    ADD CONSTRAINT fke86d11a1e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY person_shift\n    ADD CONSTRAINT fked96d718e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY person_years\n    ADD CONSTRAINT fkede9ea6ce7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\nALTER TABLE ONLY person_children_history\n    ADD CONSTRAINT fkee16b5fed54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY persons_permissions\n    ADD CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id) REFERENCES permissions(id);\nALTER TABLE ONLY persons_permissions\n    ADD CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id) REFERENCES persons(id);\nALTER TABLE ONLY badge_readers_history\n    ADD CONSTRAINT fkf30286c9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY initialization_absences_history\n    ADD CONSTRAINT fkf34058ead54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY persons_history\n    ADD CONSTRAINT fkfceabd13d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY absence_types\n    ADD CONSTRAINT fkfe65dbf7ca0a1c8a FOREIGN KEY (absence_type_group_id) REFERENCES absence_type_groups(id);\n		applied	
2	d5b343b5f6914324a090d1489c727e747ca8516d	2014-07-28 00:00:00	ALTER TABLE ONLY absence_type_groups\n    ADD CONSTRAINT fk71b9ff7730325be3 FOREIGN KEY (replacing_absence_type_id) REFERENCES absence_types(id);\nALTER TABLE ONLY absence_types_history\n    ADD CONSTRAINT fk2631804cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\n		applied	
3	f8c1e8cdee090bbf90e12a3c7ed3c9a241a75b7c	2014-07-28 00:00:00	DROP TABLE person_days_in_trouble_history;\nDROP TABLE person_months_history;\n		applied	
4	75f3e40b82dc6b8f64efe35bbc47e091e7b4b415	2014-07-28 00:00:00	CREATE SEQUENCE seq_persons_working_time_types\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE persons_working_time_types (\n    id bigint DEFAULT nextval('seq_persons_working_time_types'::regclass) NOT NULL,\n    begin_date date,\n    end_date date,\n    person_id bigint,\n    working_time_type_id bigint\n);\nALTER TABLE ONLY persons_working_time_types\n    ADD CONSTRAINT persons_working_time_types_pkey PRIMARY KEY (id);\nALTER TABLE ONLY persons_working_time_types\n    ADD CONSTRAINT fkb943247635555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);\nALTER TABLE ONLY persons_working_time_types\n    ADD CONSTRAINT fkb9432476e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);\ninsert into persons_working_time_types select nextval('seq_persons_working_time_types'), '2013-01-01', null, id, working_time_type_id from persons;\n		applied	
5	b804c36fd64a01a7cf57da4c7861cbc2dc6dae0a	2014-07-28 00:00:00	create sequence seq_office\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE office (\n    id bigint DEFAULT nextval('seq_office'::regclass) NOT NULL,\n    discriminator character varying(31) NOT NULL,\n    name character varying(255),\n    address character varying(255),\n    code integer,\n    joining_date date,\n    office_id bigint\n);\nCREATE TABLE office_history (\n    id bigint NOT NULL,\n    discriminator character varying(31) NOT NULL,\n    _revision integer NOT NULL,\n    _revision_type smallint,\n    name character varying(255),\n    address character varying(255),\n    code integer,\n    joining_date date,\n    office_id bigint\n);\nALTER TABLE ONLY office_history\n    ADD CONSTRAINT office_history_pkey PRIMARY KEY (id, _revision);\nALTER TABLE ONLY office_history\n    ADD CONSTRAINT fkd52a4e11d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);\nALTER TABLE ONLY office\n    ADD CONSTRAINT office_pkey PRIMARY KEY (id);\nALTER TABLE ONLY persons\n    ADD COLUMN office_id bigint;\nALTER TABLE ONLY office\n    ADD CONSTRAINT fkc3373ebc2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);\nALTER TABLE ONLY persons\n    ADD CONSTRAINT fkd78fcfbe2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);\nINSERT INTO office (id, discriminator, name, address, code, joining_date, office_id)\nVALUES (1, 'O', 'IIT', 'Via Moruzzi 1, Pisa', 1000, NULL, NULL);\nupdate persons set office_id =  subquery.id \nfrom (select id from office where id =1) as subquery;\ninsert into permissions (id, description) values (14, 'insertAndUpdateOffices');\n		applied	
6	8a2bbee1e23808f1057ca072b934a688c743ad58	2014-07-28 00:00:00	ALTER TABLE ONLY persons_history\n\tADD COLUMN office_id bigint;\n		applied	
7	e41e4e39b68f2fc243e85b9227b0097d90917f30	2014-07-28 00:00:00	ALTER TABLE "person_children" ALTER COLUMN "borndate" TYPE date using ("borndate"::text::date);\n		applied	
8	63eceefba16fcfb4340a8b8368324d95cfd66284	2014-07-28 00:00:00	ALTER TABLE absences RENAME COLUMN absencerequest TO absence_file;\nALTER TABLE absences_history RENAME COLUMN absencerequest TO absence_file;\nALTER TABLE "person_children_history" ALTER COLUMN "borndate" TYPE date using ("borndate"::text::date);\n	ALTER TABLE absences RENAME COLUMN absence_file TO absencerequest;\nALTER TABLE absences_history RENAME COLUMN absence_file TO absencerequest;\n	applied	
9	c08da2f937687d71e2d9b6ccc754c8aedce52fd3	2014-07-28 00:00:00	DELETE FROM persons_permissions where permissions_id = 14;\nDELETE FROM permissions where id = 14 and description = 'insertAndUpdateOffices';\nINSERT INTO permissions (description) values ('insertAndUpdateOffices');\n	DELETE FROM permissions where description = 'insertAndUpdateOffices';\nINSERT INTO permissions (id, description) values (14, 'insertAndUpdateOffices');\n	applied	
10	0e691dd9ed92db85cdaaf271e78ad5dcbe60306e	2014-07-28 00:00:00	UPDATE office SET name = 'NOME-DA-DEFINIRE', address = '', code = 0 WHERE name = 'IIT';   \n		applied	
11	7948e02eb3b0b80967796a5eff1bf229201dba80	2014-07-28 00:00:00	CREATE sequence seq_certificated_data\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE certificated_data\n(\n  id bigint NOT NULL DEFAULT nextval('seq_certificated_data'::regclass),\n  absences_sent character varying(255),\n  cognome_nome character varying(255),\n  competences_sent character varying(255),\n  is_ok boolean,\n  matricola character varying(255),\n  mealticket_sent integer,\n  month integer NOT NULL,\n  ok boolean NOT NULL,\n  problems character varying(255),\n  year integer NOT NULL,\n  person_id bigint NOT NULL,\n  CONSTRAINT certificated_data_pkey PRIMARY KEY (id),\n  CONSTRAINT fkca05bafce7a7b1be FOREIGN KEY (person_id)\n      REFERENCES persons (id)\n);   \nCREATE TABLE certificated_data_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL,\n  _revision_type smallint,\n  absences_sent character varying(255),\n  cognome_nome character varying(255),\n  competences_sent character varying(255),\n  is_ok boolean,\n  matricola character varying(255),\n  mealticket_sent integer,\n  month integer,\n  ok boolean,\n  problems character varying(255),\n  year integer,\n  person_id bigint,\n  CONSTRAINT certificated_data_history_pkey PRIMARY KEY (id, _revision),\n  CONSTRAINT fk64d88a51d54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nCREATE sequence seq_conf_general\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE conf_general\n(\n  id bigint NOT NULL DEFAULT nextval('seq_conf_general'::regclass),\n  email_to_contact character varying(255),\n  init_use_program date,\n  institute_name character varying(255),\n  number_of_viewing_couple integer,\n  password_to_presence character varying(255),\n  seat_code integer,\n  url_to_presence character varying(255),\n  user_to_presence character varying(255),\n  day_of_patron integer,\n  month_of_patron integer,\n  web_stamping_allowed boolean,\n  meal_time_end_hour integer,\n  meal_time_end_minute integer,\n  meal_time_start_hour integer,\n  meal_time_start_minute integer,\n  CONSTRAINT conf_general_pkey PRIMARY KEY (id)\n);\nCREATE TABLE conf_general_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL,\n  _revision_type smallint,\n  email_to_contact character varying(255),\n  init_use_program date,\n  institute_name character varying(255),\n  number_of_viewing_couple integer,\n  password_to_presence character varying(255),\n  seat_code integer,\n  url_to_presence character varying(255),\n  user_to_presence character varying(255),\n  day_of_patron integer,\n  month_of_patron integer,\n  web_stamping_allowed boolean,\n  meal_time_end_hour integer,\n  meal_time_end_minute integer,\n  meal_time_start_hour integer,\n  meal_time_start_minute integer,\n  CONSTRAINT conf_general_history_pkey PRIMARY KEY (id, _revision),\n  CONSTRAINT fk77a6922d54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nCREATE sequence seq_conf_year\n    START WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE conf_year\n(\n  id bigint NOT NULL DEFAULT nextval('seq_conf_year'::regclass),\n  day_expiry_vacation_past_year integer,\n  hour_max_to_calculate_worktime integer,\n  max_recovery_days_49 integer,\n  max_recovery_days_13 integer,\n  month_expire_recovery_days_49 integer,\n  month_expire_recovery_days_13 integer,\n  month_expiry_vacation_past_year integer,\n  year integer,\n  CONSTRAINT conf_year_pkey PRIMARY KEY (id)\n);\nCREATE TABLE conf_year_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL,\n  _revision_type smallint,\n  day_expiry_vacation_past_year integer,\n  hour_max_to_calculate_worktime integer,\n  max_recovery_days_49 integer,\n  max_recovery_days_13 integer,\n  month_expire_recovery_days_49 integer,\n  month_expire_recovery_days_13 integer,\n  month_expiry_vacation_past_year integer,\n  year integer,\n  CONSTRAINT conf_year_history_pkey PRIMARY KEY (id, _revision),\n  CONSTRAINT fkd883fbcdd54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nINSERT INTO conf_general(\n            email_to_contact, init_use_program, institute_name, number_of_viewing_couple, \n            password_to_presence, seat_code, url_to_presence, user_to_presence, \n            day_of_patron, month_of_patron, web_stamping_allowed, meal_time_end_hour, \n            meal_time_end_minute, meal_time_start_hour, meal_time_start_minute)\n            SELECT  email_to_contact, init_use_program, institute_name, numberofviewingcouplecolumn,\n            password_to_presence, seat_code, url_to_presence, user_to_presence, day_of_patron, month_of_patron, false, \n            mealtimeendhour, mealtimeendminute, mealtimestarthour, mealtimestartminute  \n            FROM configurations;\nINSERT INTO conf_year(\n            day_expiry_vacation_past_year, hour_max_to_calculate_worktime, max_recovery_days_49, max_recovery_days_13, \n            month_expire_recovery_days_49, month_expire_recovery_days_13, \n            month_expiry_vacation_past_year, year)\n    \t\tSELECT  dayexpiryvacationpastyear, hourmaxtocalculateworktime, maxrecoverydaysfournine, maxrecoverydaysonethree, \n    \t\tmonthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, 2013\n            FROM configurations;            \nINSERT INTO conf_year(\n            day_expiry_vacation_past_year, hour_max_to_calculate_worktime, max_recovery_days_49, max_recovery_days_13, \n            month_expire_recovery_days_49, month_expire_recovery_days_13, \n            month_expiry_vacation_past_year, year)\n    \t\tSELECT  dayexpiryvacationpastyear, hourmaxtocalculateworktime, maxrecoverydaysfournine, maxrecoverydaysonethree, \n    \t\tmonthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, 2014\n            FROM configurations;\n		applied	
12	2df7706967ffe036819ff017e188f734effe1b2a	2014-07-28 00:00:00	drop table if exists person_months_history;\ndrop table person_months;\ndrop sequence seq_person_months;\ncreate sequence seq_person_months_recap\n\tSTART WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\ncreate table person_months_recap (\n\tid bigint NOT NULL DEFAULT nextval('seq_person_months_recap'::regclass),\n\tyear integer,\n\tmonth integer,\n\ttraining_hours integer,\n\thours_approved boolean,\n\tperson_id bigint NOT NULL,\n\tCONSTRAINT person_months_recap_pkey PRIMARY KEY (id),\n\tCONSTRAINT person_person_months_recap_fkey FOREIGN KEY (person_id)\n      REFERENCES persons (id)\n);\ncreate table person_months_recap_history (\n\tid bigint NOT NULL,\n  \t_revision integer NOT NULL,\n  \t_revision_type smallint,\n  \tyear integer,\n\tmonth integer,\n\ttraining_hours integer,\n\thours_approved boolean,\n\tperson_id bigint NOT NULL,\n\tCONSTRAINT person_months_recap_history_pkey PRIMARY KEY (id,  _revision),\n\tCONSTRAINT revinfo_person_months_recap_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\n	drop table person_months_recap_history;\ndrop table person_months_recap;\ndrop sequence seq_person_months_recap;\ncreate sequence seq_person_months\n\tSTART WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\ncreate table person_months (\n\tid bigint NOT NULL DEFAULT nextval('seq_person_months'::regclass),\n\tcompensatory_rest_in_minutes integer,\n\tmonth integer,\n\tprogressiveatendofmonthinminutes integer,\n\trecuperi_ore_da_anno_precedente integer,\n\tremaining_minute_past_year_taken integer,\n\tresidual_past_year integer,\n\triposi_compensativi_da_anno_corrente integer,\n\triposi_compensativi_da_anno_precedente integer,\n\triposi_compensativi_da_inizializzazione integer,\n\tstraordinari integer,\n\ttotal_remaining_minutes integer,\n\tyear integer,\n\tperson_id bigint NOT NULL,\n\tCONSTRAINT person_months_pkey PRIMARY KEY (id),\n\tCONSTRAINT fkbb6c161de7a7b1be FOREIGN KEY (person_id)\n      REFERENCES persons (id)\t\n);\ncreate table person_months_history\n(\n\tid bigint not null\n);\n	applied	
13	96573575dbea06184d0c7c1f99fa59136479f81d	2014-07-28 00:00:00	ALTER TABLE person_days \n  ADD COLUMN stamp_modification_type_id bigint,\n  ADD CONSTRAINT fke69adb0135175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types (id);\nUPDATE person_days SET stamp_modification_type_id = (select id from stamp_modification_types where code = modification_type);\nALTER TABLE person_days_history\n  ADD COLUMN stamp_modification_type_id bigint;\n	ALTER TABLE person_days\n  DROP CONSTRAINT fke69adb0135175bce,\n  DROP COLUMN stamp_modification_type_id;\nALTER TABLE person_days_history\n  DROP COLUMN stamp_modification_type_id;  \n	applied	
14	73388e80800c4823b9de41417851e916227b5220	2014-07-28 00:00:00	ALTER TABLE person_days\n  DROP COLUMN is_time_at_work_auto_certificated,\n  DROP COLUMN modification_type;\nALTER TABLE person_days_history\n  DROP COLUMN is_time_at_work_auto_certificated,\n  DROP COLUMN modification_type;\n	ALTER TABLE person_days\n  ADD COLUMN is_time_at_work_auto_certificated boolean,\n  ADD COLUMN Modification_type  character varying(255);\nALTER TABLE person_days_history\n  ADD COLUMN is_time_at_work_auto_certificated boolean,\n  ADD COLUMN Modification_type  character varying(255);\n	applied	
16	3f29fd27aadae5e8a26d0289677b7197f62c13e4	2014-07-28 00:00:00	ALTER TABLE persons\n  DROP COLUMN working_time_type_id;\n	ALTER TABLE persons\n  ADD COLUMN working_time_type_id bigint,\n  ADD CONSTRAINT fkd78fcfbe35555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types (id);\n	applied	
17	2db8a821e21297c85f36bf35758c0ed6a4ab9362	2014-07-28 00:00:00	create sequence seq_contracts_working_time_types\n\tSTART WITH 1\n    INCREMENT BY 1\n    NO MINVALUE\n    NO MAXVALUE;\nCREATE TABLE contracts_working_time_types\n(\n  id bigint NOT NULL DEFAULT nextval('seq_contracts_working_time_types'::regclass),\n  begin_date date,\n  end_date date,\n  contract_id bigint,\n  working_time_type_id bigint,\n  CONSTRAINT contracts_working_time_types_pkey PRIMARY KEY (id),\n  CONSTRAINT fk353559b335555570 FOREIGN KEY (working_time_type_id)\n      REFERENCES working_time_types (id),\n  CONSTRAINT fk353559b3fb1f039e FOREIGN KEY (contract_id)\n      REFERENCES contracts (id)\n);\ninsert into contracts_working_time_types \n  select nextval('seq_contracts_working_time_types'), begin_contract, end_contract, id, 1 \n  from contracts where end_contract is not null;\ninsert into contracts_working_time_types  \n  select nextval('seq_contracts_working_time_types'), begin_contract, expire_contract, id, 1 \n  from contracts where end_contract is null;\n	drop table contracts_working_time_types;\ndrop sequence seq_contracts_working_time_types;\n	applied	
18	ece738475feea0bf2b4c8bbf3a64027ae1a37892	2014-07-28 00:00:00	ALTER TABLE person_months_recap ADD COLUMN fromDate date;\nALTER TABLE\tperson_months_recap ADD COLUMN toDate date;\n	ALTER TABLE person_months_recap DROP COLUMN fromDate;\nALTER TABLE person_months_recap DROP COLUMN toDate;\n	applied	
19	43153a681389d92bdc6a2476776d6824e245c532	2014-07-28 00:00:00	ALTER TABLE certificated_data ADD COLUMN traininghours_sent varchar(255);\nALTER TABLE certificated_data_history ADD COLUMN traininghours_sent varchar(255);\nALTER TABLE certificated_data DROP COLUMN ok;\nALTER TABLE certificated_data_history DROP COLUMN ok;\n	ALTER TABLE certificated_data DROP COLUMN traininghours_sent;\nALTER TABLE certificated_data_history DROP COLUMN traininghours_sent;\nALTER TABLE certificated_data ADD COLUMN ok boolean;\nALTER TABLE certificated_data_history ADD COLUMN ok boolean;\n	applied	
20	e5841239315545dee869082064850c904d7e0076	2014-07-28 00:00:00	ALTER TABLE person_days DROP COLUMN IF EXISTS time_justified;\n	ALTER TABLE person_days ADD COLUMN time_justified integer;\n	applied	
21	c0b6697db9a03bff2e9091ce7e4018ebd3321977	2014-07-28 00:00:00	ALTER TABLE competences ADD CONSTRAINT unique_integrity_key UNIQUE (person_id, competence_code_id, year, month);\n	ALTER TABLE competences DROP CONSTRAINT unique_integrity_key;\n	applied	
25	cb40119b53ef64738771c80023939ea70b26949e	2014-07-28 00:00:00	CREATE SEQUENCE seq_users_permissions_offices\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE users_permissions_offices(\nid bigint not null DEFAULT nextval('seq_users_permissions_offices'::regclass),\nuser_id bigint not null,\npermission_id bigint not null,\noffice_id bigint,\nCONSTRAINT users_permissions_offices_pkey PRIMARY KEY (id),\nCONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES users (id),\nCONSTRAINT permission_id_fk FOREIGN KEY (permission_id) REFERENCES permissions (id)\n);\nINSERT INTO users_permissions_offices (user_id, permission_id) SELECT users_id, permissions_id from users_permissions; \nUPDATE users_permissions_offices set office_id = office.id from office where joining_date is null;\nALTER TABLE users_permissions_offices ALTER COLUMN office_id SET NOT NULL;\nALTER TABLE users_permissions_offices ADD CONSTRAINT office_id_fk FOREIGN KEY (office_id) REFERENCES office (id);\n	DROP TABLE users_permissions_offices;\nDROP SEQUENCE seq_users_permissions_offices;\n	applied	
15	3ad1fbd845de417d4c954e61a34308cf7359a80c	2014-07-28 00:00:00	ALTER TABLE contracts\n  ADD COLUMN source_date date,\n  ADD COLUMN source_permission_used integer,\n  ADD COLUMN source_recovery_day_used integer,\n  ADD COLUMN source_remaining_minutes_current_year integer,\n  ADD COLUMN source_remaining_minutes_last_year integer,\n  ADD COLUMN source_vacation_current_year_used integer,\n  ADD COLUMN source_vacation_last_year_used integer;\nALTER TABLE initialization_times\n  ADD COLUMN permissionused integer,\n  ADD COLUMN recoverydayused integer,\n  ADD COLUMN vacationcurrentyearused integer,\n  ADD COLUMN vacationlastyearused integer;\nALTER TABLE initialization_times_history\n  ADD COLUMN permissionused integer,\n  ADD COLUMN recoverydayused integer,\n  ADD COLUMN vacationcurrentyearused integer,\n  ADD COLUMN vacationlastyearused integer;\nCREATE SEQUENCE seq_contract_year_recap\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE contract_year_recap\n(\n  id bigint NOT NULL DEFAULT nextval('seq_contract_year_recap'::regclass),\n  has_source boolean,\n  permission_used integer,\n  recovery_day_used integer,\n  remaining_minutes_current_year integer,\n  remaining_minutes_last_year integer,\n  vacation_current_year_used integer,\n  vacation_last_year_used integer,\n  year integer,\n  contract_id bigint NOT NULL,\n  CONSTRAINT contract_year_recap_pkey PRIMARY KEY (id),\n  CONSTRAINT fkbf232f8afb1f039e FOREIGN KEY (contract_id)\n      REFERENCES contracts (id) \n);\n	DROP TABLE contract_year_recap;\nDROP SEQUENCE seq_contract_year_recap;\nALTER TABLE contracts\n  DROP COLUMN source_date,\n  DROP COLUMN source_permission_used,\n  DROP COLUMN source_recovery_day_used,\n  DROP COLUMN source_remaining_minutes_current_year,\n  DROP COLUMN source_remaining_minutes_last_year,\n  DROP COLUMN source_vacation_current_year_used,\n  DROP COLUMN source_vacation_last_year_used;\nALTER TABLE initialization_times\n  DROP COLUMN permissionused,\n  DROP COLUMN recoverydayused,\n  DROP COLUMN vacationcurrentyearused,\n  DROP COLUMN vacationlastyearused;\nALTER TABLE initialization_times_history\n  DROP COLUMN permissionused,\n  DROP COLUMN recoverydayused,\n  DROP COLUMN vacationcurrentyearused,\n  DROP COLUMN vacationlastyearused;\n	applied	
32	877fb2a8a4996e58305838a311cdb133903153e8	2014-07-28 00:00:00	CREATE TABLE meal_ticket\n(\n  id BIGSERIAL PRIMARY KEY,\n  code text,\n  block integer,\n  number integer,\n  year int,\n  quarter int,\n  date date,\n  person_id bigint NOT NULL REFERENCES persons (id),\n  admin_id bigint NOT NULL REFERENCES persons (id)\n);\nCREATE TABLE meal_ticket_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL REFERENCES revinfo (rev), \n  _revision_type smallint,\n  code text,\n  block integer,\n  number integer,\n  year int,\n  quarter int,\n  date date,\n  person_id bigint,\n  admin_id bigint,\n  CONSTRAINT meal_ticket_history_pkey PRIMARY KEY (id , _revision )\n);\n	DROP TABLE meal_ticket_history;\nDROP TABLE meal_ticket;\n	applied	
33	c6ad3d03507db130042948a62f963fba03c5007e	2014-07-28 00:00:00	ALTER TABLE total_overtime \n  ADD COLUMN office_id bigint,\n  ADD CONSTRAINT totalovertime_office_key FOREIGN KEY (office_id) REFERENCES office (id);\nUPDATE total_overtime SET office_id = null;\n	ALTER TABLE total_overtime\n  DROP CONSTRAINT totalovertime_office_key,\n  DROP COLUMN office_id;\n	applied	
34	ccb080d2b3a27cae2b7d9277049a9c7f3dd08566	2014-07-28 00:00:00	CREATE TABLE contract_stamp_profiles(\nid BIGSERIAL PRIMARY KEY,\nfixed_working_time boolean,\nstart_from date,\nend_to date,\ncontract_id bigint NOT NULL REFERENCES contracts (id)\n);\n	DROP TABLE contract_stamp_profiles;\n	applied	
22	11fc3ca893d52b9f3c60701fc0bf5778e6f22e58	2014-07-28 00:00:00	CREATE SEQUENCE seq_users\n  START WITH 10000\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE users\n(\n  id bigint NOT NULL DEFAULT nextval('seq_users'::regclass),\n  expire_recovery_token date,\n  password character varying(255),\n  recovery_token character varying(255),\n  username character varying(255),\n  CONSTRAINT users_pkey PRIMARY KEY (id)\n);\nCREATE TABLE users_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL,\n  _revision_type smallint,\n  expire_recovery_token date,\n  password character varying(255),\n  recovery_token character varying(255),\n  username character varying(255),\n  CONSTRAINT users_history_pkey PRIMARY KEY (id, _revision),\n  CONSTRAINT fk5c2b915dd54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nCREATE TABLE users_permissions\n(\n  users_id bigint NOT NULL,\n  permissions_id bigint NOT NULL,\n  CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id)\n      REFERENCES permissions (id),\n  CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id)\n      REFERENCES users (id)\n);\nCREATE TABLE users_permissions_history\n(\n  _revision integer NOT NULL,\n  users_id bigint NOT NULL,\n  permissions_id bigint NOT NULL,\n  _revision_type smallint,\n  CONSTRAINT users_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id),\n  CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nALTER TABLE persons\n  ADD COLUMN user_id bigint,\n  ADD CONSTRAINT fkd78fcfbe47140efe FOREIGN KEY (user_id) REFERENCES users (id);\nALTER TABLE persons_history\n  ADD COLUMN user_id bigint;\nINSERT INTO users(id, username, password) SELECT  id, username, password FROM persons;\nALTER TABLE persons\n  DROP COLUMN username,\n  DROP COLUMN password;\nALTER TABLE persons_history\n  DROP COLUMN username,\n  DROP COLUMN password;\nINSERT INTO users_permissions(users_id, permissions_id) SELECT users_id, permissions_id FROM persons_permissions;\nDROP TABLE persons_permissions;\nDROP TABLE persons_permissions_history;\nUPDATE persons set user_id = id;\nDELETE FROM persons_working_time_types where person_id = 1;\nDELETE FROM persons where id = 1;\n	CREATE TABLE persons_permissions\n(\n  users_id bigint NOT NULL,\n  permissions_id bigint NOT NULL,\n  CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id)\n      REFERENCES permissions (id),\n  CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id)\n      REFERENCES persons (id)\n);\nCREATE TABLE persons_permissions_history\n(\n  _revision integer NOT NULL,\n  users_id bigint NOT NULL,\n  permissions_id bigint NOT NULL,\n  _revision_type smallint,\n  CONSTRAINT persons_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id),\n  CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\nALTER TABLE persons \n  ADD COLUMN username character varying(255),\n  ADD COLUMN password character varying(255),\n  DROP CONSTRAINT fkd78fcfbe47140efe,\n  DROP COLUMN user_id;\nALTER TABLE persons_history \n  ADD COLUMN username character varying(255),\n  ADD COLUMN password character varying(255),\n  DROP COLUMN user_id;\nDROP TABLE users_permissions_history;\nDROP TABLE users_permissions;\nDROP TABLE users_history;\nDROP TABLE users;\nDROP SEQUENCE seq_users;\n	applied	
23	f168f97f2ce56a4007e10610a104093bcfdc750f	2014-07-28 00:00:00	ALTER TABLE working_time_types ADD COLUMN meal_ticket_enabled boolean;\nALTER TABLE\tworking_time_types_history ADD COLUMN meal_ticket_enabled boolean;\nUPDATE working_time_types SET meal_ticket_enabled = true;\nUPDATE working_time_types_history SET meal_ticket_enabled = true;\n	ALTER TABLE working_time_types_history DROP COLUMN meal_ticket_enabled;\nALTER TABLE working_time_types DROP COLUMN meal_ticket_enabled;\n	applied	
24	6d565d0893ede6954213cd2df28c0bcbc3a37c38	2014-07-28 00:00:00	CREATE SEQUENCE seq_conf_general_tmp\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE conf_general_tmp(\nid bigint not null DEFAULT nextval('seq_conf_general_tmp'::regclass),\nfield text,\nfield_value text,\noffice_id bigint not null,\nCONSTRAINT conf_general_tmp_pkey PRIMARY KEY (id),\nCONSTRAINT conf_general_tmp_fkey FOREIGN KEY (office_id)\n      REFERENCES office (id)\n);\nCREATE TABLE conf_general_tmp_history(\nid bigint not null,\n_revision integer NOT NULL,\n_revision_type smallint,\nfield text,\nfield_value text,\noffice_id bigint not null,\nCONSTRAINT conf_general_tmp_history_pkey PRIMARY KEY (id, _revision),\nCONSTRAINT revinfo_conf_general_tmp_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\ninsert into conf_general_tmp (field, office_id) values('init_use_program', 1);\ninsert into conf_general_tmp (field, office_id) values('institute_name', 1);\ninsert into conf_general_tmp (field, office_id) values('email_to_contact', 1);\ninsert into conf_general_tmp (field, office_id) values('seat_code', 1);\ninsert into conf_general_tmp (field, office_id) values('url_to_presence', 1);\ninsert into conf_general_tmp (field, office_id) values('user_to_presence', 1);\ninsert into conf_general_tmp (field, office_id) values('password_to_presence', 1);\ninsert into conf_general_tmp (field, office_id) values('number_of_viewing_couple', 1);\ninsert into conf_general_tmp (field, office_id) values('month_of_patron', 1);\ninsert into conf_general_tmp (field, office_id) values('day_of_patron', 1);\ninsert into conf_general_tmp (field, office_id) values('web_stamping_allowed', 1);\ninsert into conf_general_tmp (field, office_id) values('meal_time_start_hour', 1);\ninsert into conf_general_tmp (field, office_id) values('meal_time_start_minute', 1);\ninsert into conf_general_tmp (field, office_id) values('meal_time_end_hour', 1);\ninsert into conf_general_tmp (field, office_id) values('meal_time_end_minute', 1);\nUPDATE conf_general_tmp SET field_value = conf_general.init_use_program from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'init_use_program';\nUPDATE conf_general_tmp SET field_value = conf_general.institute_name from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'institute_name';\nUPDATE conf_general_tmp SET field_value = conf_general.email_to_contact from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'email_to_contact';\nUPDATE conf_general_tmp SET field_value = conf_general.seat_code from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'seat_code';\nUPDATE conf_general_tmp SET field_value = conf_general.url_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'url_to_presence';\nUPDATE conf_general_tmp SET field_value = conf_general.user_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'user_to_presence';\nUPDATE conf_general_tmp SET field_value = conf_general.password_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'password_to_presence';\nUPDATE conf_general_tmp SET field_value = conf_general.number_of_viewing_couple from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'number_of_viewing_couple';\nUPDATE conf_general_tmp SET field_value = conf_general.month_of_patron from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'month_of_patron';\nUPDATE conf_general_tmp SET field_value = conf_general.day_of_patron from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'day_of_patron';\nUPDATE conf_general_tmp SET field_value = conf_general.web_stamping_allowed from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'web_stamping_allowed';\nUPDATE conf_general_tmp SET field_value = conf_general.meal_time_start_hour from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_start_hour';\nUPDATE conf_general_tmp SET field_value = conf_general.meal_time_start_minute from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_start_minute';\nUPDATE conf_general_tmp SET field_value = conf_general.meal_time_end_hour from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_end_hour';\nUPDATE conf_general_tmp SET field_value = conf_general.meal_time_end_minute from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_end_minute';\nDROP TABLE conf_general_history;\nDROP TABLE conf_general;\nDROP SEQUENCE seq_conf_general;\nALTER TABLE conf_general_tmp RENAME TO conf_general;\nALTER TABLE conf_general_tmp_history RENAME TO conf_general_history;\nALTER SEQUENCE seq_conf_general_tmp RENAME TO seq_conf_general;\nCREATE SEQUENCE seq_conf_year_tmp\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE conf_year_tmp(\nid bigint not null DEFAULT nextval('seq_conf_year_tmp'::regclass),\nfield text,\nfield_value text,\nyear integer,\noffice_id bigint not null,\nCONSTRAINT conf_year_tmp_pkey PRIMARY KEY (id),\nCONSTRAINT conf_year_tmp_fkey FOREIGN KEY (office_id)\n      REFERENCES office (id)\n);\nCREATE TABLE conf_year_tmp_history(\nid bigint not null,\n_revision integer NOT NULL,\n_revision_type smallint,\nfield text,\nfield_value text,\nyear integer,\noffice_id bigint not null,\nCONSTRAINT conf_year_tmp_history_pkey PRIMARY KEY (id, _revision),\nCONSTRAINT revinfo_conf_year_tmp_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\ninsert into conf_year_tmp (field, year, office_id) values('month_expiry_vacation_past_year', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('day_expiry_vacation_past_year', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_13', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_49', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('max_recovery_days_13', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('max_recovery_days_49', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('hour_max_to_calculate_worktime', 2014, 1);\ninsert into conf_year_tmp (field, year, office_id) values('month_expiry_vacation_past_year', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('day_expiry_vacation_past_year', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_13', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_49', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('max_recovery_days_13', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('max_recovery_days_49', 2013, 1);\ninsert into conf_year_tmp (field, year, office_id) values('hour_max_to_calculate_worktime', 2013, 1);\nUPDATE conf_year_tmp SET field_value = conf_year.month_expiry_vacation_past_year from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expiry_vacation_past_year';\nUPDATE conf_year_tmp SET field_value = conf_year.day_expiry_vacation_past_year from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'day_expiry_vacation_past_year';\nUPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_13 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expire_recovery_days_13';\nUPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_49 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expire_recovery_days_49';\nUPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_13 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'max_recovery_days_13';\nUPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_49 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'max_recovery_days_49';\nUPDATE conf_year_tmp SET field_value = conf_year.hour_max_to_calculate_worktime from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'hour_max_to_calculate_worktime';\nUPDATE conf_year_tmp SET field_value = conf_year.month_expiry_vacation_past_year from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expiry_vacation_past_year';\nUPDATE conf_year_tmp SET field_value = conf_year.day_expiry_vacation_past_year from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'day_expiry_vacation_past_year';\nUPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_13 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expire_recovery_days_13';\nUPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_49 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expire_recovery_days_49';\nUPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_13 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'max_recovery_days_13';\nUPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_49 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'max_recovery_days_49';\nUPDATE conf_year_tmp SET field_value = conf_year.hour_max_to_calculate_worktime from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'hour_max_to_calculate_worktime';\nDROP TABLE conf_year_history;\nDROP TABLE conf_year;\nDROP SEQUENCE seq_conf_year;\nALTER TABLE conf_year_tmp RENAME TO conf_year;\nALTER TABLE conf_year_tmp_history RENAME TO conf_year_history;\nALTER SEQUENCE seq_conf_year_tmp RENAME TO seq_conf_year;\nALTER TABLE conf_general ADD CONSTRAINT unique_conf_general_integrity_key UNIQUE (field, office_id);\nALTER TABLE conf_year ADD CONSTRAINT unique_conf_year_integrity_key UNIQUE (field, year, office_id);\n	CREATE SEQUENCE seq_conf_general_tmp\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE conf_general_tmp(\nid bigint not null DEFAULT nextval('seq_conf_general_tmp'::regclass),\ninit_use_program date,\t\ninstitute_name text,\nemail_to_contact text,\nseat_code integer,\nurl_to_presence text,\nuser_to_presence text,\npassword_to_presence text,\nnumber_of_viewing_couple integer,\nmonth_of_patron integer,\nday_of_patron integer,\nweb_stamping_allowed boolean,\nmeal_time_start_hour integer, \nmeal_time_start_minute integer,\nmeal_time_end_hour integer,\nmeal_time_end_minute integer,\nCONSTRAINT conf_general_tmp_pkey PRIMARY KEY (id)\n);\nCREATE TABLE conf_general_tmp_history(\nid bigint not null,\n_revision integer NOT NULL,\n_revision_type smallint,\ninit_use_program date,\t\ninstitute_name text,\nemail_to_contact text,\nseat_code integer,\nurl_to_presence text,\nuser_to_presence text,\npassword_to_presence text,\nnumber_of_viewing_couple integer,\nmonth_of_patron integer,\nday_of_patron integer,\nweb_stamping_allowed boolean,\nmeal_time_start_hour integer, \nmeal_time_start_minute integer,\nmeal_time_end_hour integer,\nmeal_time_end_minute integer,\nCONSTRAINT revinfo_conf_general_tmp_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\ninsert into conf_general_tmp (id) values (1);\nUPDATE conf_general_tmp SET init_use_program = conf_general.field_value from conf_general where conf_general.field = 'init_use_program' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET institute_name = conf_general.field_value from conf_general where conf_general.field = 'institute_name' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET email_to_contact = conf_general.field_value from conf_general where conf_general.field = 'email_to_contact' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET seat_code = conf_general.field_value from conf_general where conf_general.field = 'seat_code' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET url_to_presence = conf_general.field_value from conf_general where conf_general.field = 'url_to_presence' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET user_to_presence = conf_general.field_value from conf_general where conf_general.field = 'user_to_presence' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET password_to_presence = conf_general.field_value from conf_general where conf_general.field = 'password_to_presence' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET number_of_viewing_couple = conf_general.field_value from conf_general where conf_general.field = 'number_of_viewing_couple' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET month_of_patron = conf_general.field_value from conf_general where conf_general.field = 'month_of_patron' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET day_of_patron = conf_general.field_value from conf_general where conf_general.field = 'day_of_patron' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET web_stamping_allowed = conf_general.field_value from conf_general where conf_general.field = 'web_stamping_allowed' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET meal_time_start_hour = conf_general.field_value from conf_general where conf_general.field = 'meal_time_start_hour' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET meal_time_start_minute = conf_general.field_value from conf_general where conf_general.field = 'meal_time_start_minute' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET meal_time_end_hour = conf_general.field_value from conf_general where conf_general.field = 'meal_time_end_hour' and conf_general_tmp.id = 1;\nUPDATE conf_general_tmp SET meal_time_end_minute = conf_general.field_value from conf_general where conf_general.field = 'meal_time_end_minute' and conf_general_tmp.id = 1;\nDROP TABLE conf_general_history;\nDROP TABLE conf_general;\nDROP SEQUENCE seq_conf_general;\nALTER TABLE conf_general_tmp RENAME TO conf_general;\nALTER TABLE conf_general_tmp_history RENAME TO conf_general_history;\nALTER SEQUENCE seq_conf_general_tmp RENAME TO seq_conf_general;\nCREATE SEQUENCE seq_conf_year_tmp\n  START WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE conf_year_tmp(\nid bigint not null DEFAULT nextval('seq_conf_year_tmp'::regclass),\nmonth_expiry_vacation_past_year integer,\nday_expiry_vacation_past_year integer, \nmonth_expire_recovery_days_13 integer, \nmonth_expire_recovery_days_49 integer,\nmax_recovery_days_13 integer,\nmax_recovery_days_49 integer,\nhour_max_to_calculate_worktime integer,\nyear integer,\nCONSTRAINT conf_year_tmp_pkey PRIMARY KEY (id)\n);\nCREATE TABLE conf_year_tmp_history(\nid bigint not null,\n_revision integer NOT NULL,\n_revision_type smallint,\nmonth_expiry_vacation_past_year integer,\nday_expiry_vacation_past_year integer, \nmonth_expire_recovery_days_13 integer, \nmonth_expire_recovery_days_49 integer,\nmax_recovery_days_13 integer,\nmax_recovery_days_49 integer,\nhour_max_to_calculate_worktime integer,\nyear integer,\nCONSTRAINT conf_year_tmp_history_pkey PRIMARY KEY (id, _revision),\nCONSTRAINT revinfo_conf_year_tmp_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev)\n);\ninsert into conf_year_tmp (id, year) values (1, 2014);\ninsert into conf_year_tmp (id, year) values (2, 2013);\nUPDATE conf_year_tmp SET month_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'month_expiry_vacation_past_year' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET day_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'day_expiry_vacation_past_year' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET month_expire_recovery_days_13 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_13' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET month_expire_recovery_days_49 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_49' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET max_recovery_days_13 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_13' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET max_recovery_days_49 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_49' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET hour_max_to_calculate_worktime = conf_year.field_value from conf_year where conf_year.field = 'hour_max_to_calculate_worktime' and conf_year.year = 2014 and conf_year_tmp.year = 2014;\nUPDATE conf_year_tmp SET month_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'month_expiry_vacation_past_year' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET day_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'day_expiry_vacation_past_year' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET month_expire_recovery_days_13 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_13' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET month_expire_recovery_days_49 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_49' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET max_recovery_days_13 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_13' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET max_recovery_days_49 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_49' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nUPDATE conf_year_tmp SET hour_max_to_calculate_worktime = conf_year.field_value from conf_year where conf_year.field = 'hour_max_to_calculate_worktime' and conf_year.year = 2013 and conf_year_tmp.year = 2013;\nDROP TABLE conf_year_history;\nDROP TABLE conf_year;\nDROP SEQUENCE seq_conf_year;\nALTER TABLE conf_year_tmp RENAME TO conf_year;\nALTER TABLE conf_year_tmp_history RENAME TO conf_year_history;\nALTER SEQUENCE seq_conf_year_tmp RENAME TO seq_conf_year;\n	applied	
26	fc0ff14469708e1b51f0d1b0aada67ac0a9b584e	2014-07-28 00:00:00	ALTER TABLE working_time_types \n  ADD COLUMN office_id bigint,\n  ADD CONSTRAINT wtt_office_key FOREIGN KEY (office_id) REFERENCES office (id);\nUPDATE working_time_types SET office_id = null;\nALTER TABLE working_time_types_history \n  ADD COLUMN office_id bigint;\n	ALTER TABLE working_time_types\n  DROP CONSTRAINT wtt_office_key,\n  DROP COLUMN office_id;\nALTER TABLE working_time_types_history \n  DROP COLUMN office_id;  \n	applied	
27	23cbaadff0d90d48f8dc7e3f56846a54de654032	2014-07-28 00:00:00	ALTER TABLE working_time_types \n  ADD COLUMN disabled boolean;\nUPDATE working_time_types SET disabled = false;\nALTER TABLE working_time_types_history \n  ADD COLUMN disabled boolean;\n	ALTER TABLE working_time_types\n  DROP COLUMN disabled;\nALTER TABLE working_time_types_history \n  DROP COLUMN disabled;  \n	applied	
28	ab4b9d094ea8137d4d1d5e02f5715b578dc3f6d0	2014-07-28 00:00:00	ALTER TABLE persons ADD COLUMN cnr_email varchar(255);\nALTER TABLE persons ADD COLUMN fax varchar (255);\nALTER TABLE persons ADD COLUMN mobile varchar (255);\nALTER TABLE persons ADD COLUMN telephone varchar (255);\nALTER TABLE persons ADD COLUMN department varchar (255);\nALTER TABLE persons ADD COLUMN head_office varchar (255);\nALTER TABLE persons ADD COLUMN room varchar (255);\nALTER TABLE persons ADD COLUMN want_email boolean;\nALTER TABLE persons_history ADD COLUMN cnr_email varchar(255);\nALTER TABLE persons_history ADD COLUMN fax varchar (255);\nALTER TABLE persons_history ADD COLUMN mobile varchar (255);\nALTER TABLE persons_history ADD COLUMN telephone varchar (255);\nALTER TABLE persons_history ADD COLUMN department varchar (255);\nALTER TABLE persons_history ADD COLUMN head_office varchar (255);\nALTER TABLE persons_history ADD COLUMN room varchar (255);\nALTER TABLE persons_history ADD COLUMN want_email boolean;\nUPDATE persons set email = contact_data.email from contact_data where persons.id = contact_data.person_id;\nUPDATE persons set fax = contact_data.fax from contact_data where persons.id = contact_data.person_id;\nUPDATE persons set mobile = contact_data.mobile from contact_data where persons.id = contact_data.person_id;\nUPDATE persons set telephone = contact_data.telephone from contact_data where persons.id = contact_data.person_id;\nUPDATE persons set department = locations.department from locations where persons.id = locations.person_id;\nUPDATE persons set head_office = locations.headoffice from locations where persons.id = locations.person_id;\nUPDATE persons set room = locations.room from locations where persons.id = locations.person_id;\nDROP TABLE contact_data_history;\nDROP TABLE contact_data;\nDROP TABLE locations;\nDROP sequence seq_contact_data;\nDROP sequence seq_locations;\n	CREATE SEQUENCE seq_contact_data\nSTART WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE SEQUENCE seq_locations\nSTART WITH 1\n  INCREMENT BY 1\n  NO MINVALUE\n  NO MAXVALUE;\nCREATE TABLE contact_data(\nid bigint not null DEFAULT nextval('seq_contact_data'::regclass),\nemail varchar(255),\nfax varchar(255),\nmobile varchar(255),\ntelephone varchar(255),\nperson_id bigint,\nCONSTRAINT contact_data_pkey PRIMARY KEY (id),\nCONSTRAINT contact_data_fkey FOREIGN KEY (person_id)\n      REFERENCES persons (id));\nCREATE TABLE locations(\nid bigint not null DEFAULT nextval('seq_locations'::regclass),\ndepartment varchar(255),\nhead_office varchar(255),\nroom varchar(255),\nperson_id bigint,\nCONSTRAINT locations_pkey PRIMARY KEY (id),\nCONSTRAINT locations_fkey FOREIGN KEY (person_id)\n      REFERENCES persons (id));\nCREATE TABLE contact_data_history(\nid bigint not null,\n_revision integer NOT NULL,\n_revision_type smallint,\nemail varchar(255),\nfax varchar(255),\nmobile varchar(255),\ntelephone varchar(255),\nperson_id bigint,\nCONSTRAINT contact_data_history_pkey PRIMARY KEY (id, _revision),\nCONSTRAINT revinfo_contact_data_history_fkey FOREIGN KEY (_revision)\n      REFERENCES revinfo (rev));\ninsert into contact_data(person_id) select id from persons;\nupdate contact_data set email = persons.email, fax = persons.fax, mobile = persons.mobile, telephone = persons.telephone \nfrom persons where persons.id = contact_data.person_id;\ninsert into locations(person_id) select id from persons;\nupdate locations set department = persons.department, head_office = persons.head_office, room = persons.room \nfrom persons where persons.id = locations.person_id;\n	applied	
29	9a987e24327390b391872d286fa08a750d8804a8	2014-07-28 00:00:00	ALTER TABLE office\n  ADD COLUMN contraction TEXT;\nALTER TABLE office_history\n  ADD COLUMN contraction TEXT;\nCREATE TABLE roles\n(\n  id BIGSERIAL PRIMARY KEY,\n  name TEXT\n);\nCREATE TABLE roles_history\n(\n  id bigint NOT NULL,\n  _revision integer NOT NULL REFERENCES revinfo (rev), \n  _revision_type smallint,\n  name TEXT,\n  CONSTRAINT roles_history_pkey PRIMARY KEY (id , _revision )\n);\nCREATE TABLE roles_permissions\n(\n  roles_id bigint NOT NULL REFERENCES roles (id),\n  permissions_id bigint NOT NULL REFERENCES permissions (id)\n);\nCREATE TABLE roles_permissions_history\n(\n  roles_id bigint,\n  permissions_id bigint,\n  _revision integer NOT NULL REFERENCES revinfo (rev), \n  _revision_type smallint\n);\nCREATE TABLE users_roles_offices\n(\n  id BIGSERIAL PRIMARY KEY,\n  office_id BIGINT REFERENCES office (id),\n  role_id BIGINT REFERENCES roles (id),\n  user_id BIGINT REFERENCES users (id),\n  CONSTRAINT uro_unique_index UNIQUE (office_id, role_id, user_id)\n);\nDROP TABLE users_permissions_history;\nDROP TABLE users_permissions;\nDROP TABLE users_permissions_offices;\nALTER TABLE shift_time_table ALTER COLUMN id set DEFAULT nextval('seq_shift_time_table');\n	ALTER TABLE office\n  DROP COLUMN contraction;\nALTER TABLE office_history\n  DROP COLUMN contraction;\nDROP TABLE roles_permissions_history;  \nDROP TABLE roles_permissions;\nDROP TABLE users_roles_offices;\nDROP TABLE roles_history;\nDROP TABLE roles;\nCREATE TABLE users_permissions\n(\n  users_id bigint NOT NULL REFERENCES users (id),\n  permissions_id bigint NOT NULL REFERENCES permissions (id)\n);\nCREATE TABLE users_permissions_history\n(\n  users_id bigint,\n  permissions_id bigint,\n  _revision integer NOT NULL REFERENCES revinfo (rev), \n  _revision_type smallint\n);\nCREATE TABLE users_permissions_offices\n(\n  id BIGSERIAL PRIMARY KEY,\n  user_id bigint NOT NULL REFERENCES users (id),\n  permission_id bigint NOT NULL REFERENCES permissions (id),\n  office_id bigint NOT NULL REFERENCES office (id)\n);\n	applied	
30	697f35209c61b27338d7bf8dc9b758c1b54a0bc4	2014-07-28 00:00:00	ALTER TABLE persons \n  ADD COLUMN birthday date;\nALTER TABLE persons_history \n  ADD COLUMN birthday date;\n	ALTER TABLE persons \n  DROP COLUMN birthday;\nALTER TABLE persons_history \n  DROP COLUMN birthday;\n	applied	
31	c29c4d6daa28e36e2da82401e114e0fcb6e0c683	2014-07-28 00:00:00	ALTER TABLE office \n  DROP COLUMN discriminator;\nALTER TABLE office_history\n  DROP COLUMN discriminator; \n	ALTER TABLE office \n  ADD COLUMN discriminator character varying(31);\nALTER TABLE office_history \n  ADD COLUMN discriminator character varying(31);\n	applied	
35	b94f56f189d7f187400b95299d1c72a176f9021b	2014-07-28 00:00:00	ALTER TABLE shift_time_table DROP COLUMN startshift;\nALTER TABLE shift_time_table DROP COLUMN endshift;\nALTER TABLE shift_time_table DROP COLUMN description;\nALTER TABLE shift_time_table ADD COLUMN start_morning VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN end_morning VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN start_afternoon VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN end_afternoon VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN start_morning_lunch_time VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN end_morning_lunch_time VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN start_afternoon_lunch_time VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN end_afternoon_lunch_time VARCHAR(64);\nALTER TABLE shift_time_table ADD COLUMN total_working_minutes INT;\nALTER TABLE shift_time_table ADD COLUMN paid_minutes INT;\nALTER TABLE person_shift_days ADD COLUMN shift_slot VARCHAR(64);\nALTER TABLE shift_type ADD COLUMN shift_time_table_id bigint REFERENCES shift_time_table(id);\nALTER TABLE shift_type ADD COLUMN supervisor bigint REFERENCES persons(id);\nUPDATE person_shift_days SET shift_slot = 'MORNING' WHERE shift_time_table_id = 82;\nUPDATE person_shift_days SET shift_slot = 'AFTERNOON' WHERE shift_time_table_id = 83;\nALTER TABLE person_shift_days DROP COLUMN shift_time_table_id;\nDELETE FROM shift_time_table;\nALTER TABLE shift_time_table ALTER COLUMN id SET DEFAULT nextval('seq_shift_time_table');\nSELECT setval('seq_shift_time_table', 1);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('07:00:00', '14:00:00', '12:30:00', '19:00:00', \n\t\t\t'12:30:00','13:00:00', '13:30:00', '14:00:00', 720, 390);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('06:00:00', '13:00:00', '11:30:00', '18:00:00', \n\t\t\t'12:00:00','12:30:00', '12:30:00', '13:00:00', 720, 390);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('06:30:00', '13:30:00', '12:00:00', '18:30:00', \n\t\t\t'12:30:00','13:00:00', '13:00:00', '13:30:00', 720, 390);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('07:50:00', '14:50:00', '13:20:00', '19:50:00', \n\t\t\t'13:50:00','14:20:00', '14:20:00', '14:50:00', 720, 390);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('08:20:00', '14:20:00', '13:50:00', '20:20:00', \n\t\t\t'14:20:00','', '', '13:50:00', 720, 390);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('07:00:00', '13:35:00', '13:05:00', '18:00:00', \n\t\t\t'13:35:00','', '', '13:05:00', 660, 345); \nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('07:50:00', '13:35:00', '13:05:00', '18:50:00', \n\t\t\t'13:35:00','', '', '13:05:00', 660, 345);\nINSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,\n\tstart_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,\n\ttotal_working_minutes, paid_minutes)\n\tVALUES ('08:20:00', '14:05:00', '13:35:00', '19:20:00', \n\t\t\t'14:05:00','', '', '13:35:00', 660, 345);\nCREATE TABLE shift_type_history (\n    id bigint NOT NULL,\n    _revision integer NOT NULL,\n  \t_revision_type smallint,\n    description character varying(255),\n    type character varying(255),\n    supervisor bigint,\n    CONSTRAINT revinfo_shift_type_history_fkey FOREIGN KEY (_revision)\n    REFERENCES revinfo (rev)\n);\nALTER TABLE competences RENAME COLUMN valuerequest TO valuerequested;\nALTER TABLE competences ALTER COLUMN valuerequested type numeric(4,1);\n	ALTER TABLE shift_time_table ADD COLUMN startshift TIMESTAMP without time zone;\nALTER TABLE shift_time_table ADD COLUMN endshift TIMESTAMP without time zone;\nALTER TABLE shift_time_table ADD COLUMN description VARCHAR(64);\nALTER TABLE shift_type DROP COLUMN shift_time_table_id;\nALTER TABLE shift_type DROP COLUMN supervisor;\ndrop table if exists shift_type_history; \nALTER TABLE shift_time_table DROP COLUMN start_morning;\nALTER TABLE shift_time_table DROP COLUMN end_morning;\nALTER TABLE shift_time_table DROP COLUMN start_afternoon;\nALTER TABLE shift_time_table DROP COLUMN end_afternoon;\nALTER TABLE shift_time_table DROP COLUMN start_morning_lunch_time;\nALTER TABLE shift_time_table DROP COLUMN end_morning_lunch_time;\nALTER TABLE shift_time_table DROP COLUMN start_afternoon_lunch_time;\nALTER TABLE shift_time_table DROP COLUMN end_afternoon_lunch_time;\nALTER TABLE shift_time_table DROP COLUMN total_working_minutes;\nALTER TABLE shift_time_table DROP COLUMN paid_minutes;\nALTER TABLE person_shift_days ADD COLUMN shift_time_table_id bigint REFERENCES shift_time_table(id);\nINSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (82, '01-01-12 07:00:00 AM', '01-01-12 01:30:00 PM', 'turno mattina');\nINSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (83, '01-01-12 01:30:00 PM', '01-01-12 07:00:00 PM', 'turno pomeriggio');\nUPDATE person_shift_days SET shift_time_table_id = 82 WHERE shift_slot = 'MORNING' ;\nUPDATE person_shift_days SET shift_time_table_id = 83 WHERE shift_slot = 'AFTERNOON';\nALTER TABLE person_shift_days DROP COLUMN shift_slot;\nALTER TABLE competences RENAME COLUMN valuerequested TO valuerequest;\nALTER TABLE competences ALTER COLUMN valuerequest type integer;\n	applied	
\.


--
-- Data for Name: qualifications; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY qualifications (id, description, qualification) FROM stdin;
\.


--
-- Data for Name: qualifications_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY qualifications_history (id, _revision, _revision_type, description, qualification) FROM stdin;
\.


--
-- Data for Name: revinfo; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY revinfo (rev, revtstmp) FROM stdin;
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY roles (id, name) FROM stdin;
\.


--
-- Data for Name: roles_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY roles_history (id, _revision, _revision_type, name) FROM stdin;
\.


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('roles_id_seq', 1, false);


--
-- Data for Name: roles_permissions; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY roles_permissions (roles_id, permissions_id) FROM stdin;
\.


--
-- Data for Name: roles_permissions_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY roles_permissions_history (roles_id, permissions_id, _revision, _revision_type) FROM stdin;
\.


--
-- Name: seq_absence_type_groups; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_absence_type_groups', 1, false);


--
-- Name: seq_absence_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_absence_types', 1, false);


--
-- Name: seq_absences; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_absences', 1, false);


--
-- Name: seq_auth_users; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_auth_users', 1, false);


--
-- Name: seq_badge_readers; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_badge_readers', 1, false);


--
-- Name: seq_certificated_data; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_certificated_data', 1, false);


--
-- Name: seq_competence_codes; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_competence_codes', 1, false);


--
-- Name: seq_competences; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_competences', 1, false);


--
-- Name: seq_conf_general; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_conf_general', 15, true);


--
-- Name: seq_conf_year; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_conf_year', 14, true);


--
-- Name: seq_configurations; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_configurations', 1, false);


--
-- Name: seq_contract_year_recap; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_contract_year_recap', 1, false);


--
-- Name: seq_contracts; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_contracts', 1, false);


--
-- Name: seq_contracts_working_time_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_contracts_working_time_types', 1, false);


--
-- Name: seq_groups; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_groups', 1, false);


--
-- Name: seq_initialization_absences; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_initialization_absences', 1, false);


--
-- Name: seq_initialization_times; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_initialization_times', 1, false);


--
-- Name: seq_office; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_office', 1, true);


--
-- Name: seq_options; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_options', 1, false);


--
-- Name: seq_permissions; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_permissions', 1, true);


--
-- Name: seq_person_children; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_children', 1, false);


--
-- Name: seq_person_days; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_days', 1, false);


--
-- Name: seq_person_days_in_trouble; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_days_in_trouble', 1, false);


--
-- Name: seq_person_hour_for_overtime; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_hour_for_overtime', 1, false);


--
-- Name: seq_person_months_recap; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_months_recap', 1, false);


--
-- Name: seq_person_reperibility; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_reperibility', 1, false);


--
-- Name: seq_person_reperibility_days; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_reperibility_days', 1, false);


--
-- Name: seq_person_reperibility_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_reperibility_types', 1, false);


--
-- Name: seq_person_shift; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_shift', 1, false);


--
-- Name: seq_person_shift_days; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_shift_days', 1, false);


--
-- Name: seq_person_shift_shift_type; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_shift_shift_type', 1, false);


--
-- Name: seq_person_years; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_person_years', 1, false);


--
-- Name: seq_persons; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_persons', 1, false);


--
-- Name: seq_persons_working_time_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_persons_working_time_types', 1, false);


--
-- Name: seq_qualifications; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_qualifications', 1, false);


--
-- Name: seq_revinfo; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_revinfo', 1, false);


--
-- Name: seq_shift_cancelled; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_shift_cancelled', 1, false);


--
-- Name: seq_shift_time_table; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_shift_time_table', 9, true);


--
-- Name: seq_shift_type; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_shift_type', 1, false);


--
-- Name: seq_stamp_modification_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_stamp_modification_types', 1, false);


--
-- Name: seq_stamp_profiles; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_stamp_profiles', 1, false);


--
-- Name: seq_stamp_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_stamp_types', 1, false);


--
-- Name: seq_stampings; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_stampings', 1, false);


--
-- Name: seq_total_overtime; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_total_overtime', 1, false);


--
-- Name: seq_users; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_users', 10000, false);


--
-- Name: seq_users_permissions_offices; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_users_permissions_offices', 1, false);


--
-- Name: seq_vacation_codes; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_vacation_codes', 1, false);


--
-- Name: seq_vacation_periods; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_vacation_periods', 1, false);


--
-- Name: seq_valuable_competences; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_valuable_competences', 1, false);


--
-- Name: seq_web_stamping_address; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_web_stamping_address', 1, false);


--
-- Name: seq_working_time_type_days; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_working_time_type_days', 1, false);


--
-- Name: seq_working_time_types; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_working_time_types', 1, false);


--
-- Name: seq_year_recaps; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('seq_year_recaps', 1, false);


--
-- Data for Name: shift_cancelled; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY shift_cancelled (id, date, shift_type_id) FROM stdin;
\.


--
-- Data for Name: shift_time_table; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY shift_time_table (id, start_morning, end_morning, start_afternoon, end_afternoon, start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time, total_working_minutes, paid_minutes) FROM stdin;
2	07:00:00	14:00:00	12:30:00	19:00:00	12:30:00	13:00:00	13:30:00	14:00:00	720	390
3	06:00:00	13:00:00	11:30:00	18:00:00	12:00:00	12:30:00	12:30:00	13:00:00	720	390
4	06:30:00	13:30:00	12:00:00	18:30:00	12:30:00	13:00:00	13:00:00	13:30:00	720	390
5	07:50:00	14:50:00	13:20:00	19:50:00	13:50:00	14:20:00	14:20:00	14:50:00	720	390
6	08:20:00	14:20:00	13:50:00	20:20:00	14:20:00			13:50:00	720	390
7	07:00:00	13:35:00	13:05:00	18:00:00	13:35:00			13:05:00	660	345
8	07:50:00	13:35:00	13:05:00	18:50:00	13:35:00			13:05:00	660	345
9	08:20:00	14:05:00	13:35:00	19:20:00	14:05:00			13:35:00	660	345
\.


--
-- Data for Name: shift_type; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY shift_type (id, description, type, shift_time_table_id, supervisor) FROM stdin;
\.


--
-- Data for Name: shift_type_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY shift_type_history (id, _revision, _revision_type, description, type, supervisor) FROM stdin;
\.


--
-- Data for Name: stamp_modification_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stamp_modification_types (id, code, description) FROM stdin;
\.


--
-- Data for Name: stamp_modification_types_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stamp_modification_types_history (id, _revision, _revision_type, code, description) FROM stdin;
\.


--
-- Data for Name: stamp_profiles; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stamp_profiles (id, end_to, fixedworkingtime, start_from, person_id) FROM stdin;
\.


--
-- Data for Name: stamp_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stamp_types (id, code, description, identifier) FROM stdin;
\.


--
-- Data for Name: stamp_types_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stamp_types_history (id, _revision, _revision_type, code, description, identifier) FROM stdin;
\.


--
-- Data for Name: stampings; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stampings (id, date, marked_by_admin, note, way, badge_reader_id, personday_id, stamp_modification_type_id, stamp_type_id) FROM stdin;
\.


--
-- Data for Name: stampings_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY stampings_history (id, _revision, _revision_type, date, marked_by_admin, note, way, badge_reader_id, personday_id, stamp_modification_type_id, stamp_type_id) FROM stdin;
\.


--
-- Data for Name: total_overtime; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY total_overtime (id, date, numberofhours, year, office_id) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY users (id, expire_recovery_token, password, recovery_token, username) FROM stdin;
\.


--
-- Data for Name: users_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY users_history (id, _revision, _revision_type, expire_recovery_token, password, recovery_token, username) FROM stdin;
\.


--
-- Data for Name: users_roles_offices; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY users_roles_offices (id, office_id, role_id, user_id) FROM stdin;
\.


--
-- Name: users_roles_offices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: epas
--

SELECT pg_catalog.setval('users_roles_offices_id_seq', 1, false);


--
-- Data for Name: vacation_codes; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY vacation_codes (id, description, permission_days, vacation_days) FROM stdin;
\.


--
-- Data for Name: vacation_codes_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY vacation_codes_history (id, _revision, _revision_type, description, permission_days, vacation_days) FROM stdin;
\.


--
-- Data for Name: vacation_periods; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY vacation_periods (id, begin_from, end_to, contract_id, vacation_codes_id) FROM stdin;
\.


--
-- Data for Name: vacation_periods_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY vacation_periods_history (id, _revision, _revision_type, begin_from, end_to, contract_id, vacation_codes_id) FROM stdin;
\.


--
-- Data for Name: valuable_competences; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY valuable_competences (id, codicecomp, descrizione, person_id) FROM stdin;
\.


--
-- Data for Name: web_stamping_address; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY web_stamping_address (id, webaddresstype, confparameters_id) FROM stdin;
\.


--
-- Data for Name: web_stamping_address_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY web_stamping_address_history (id, _revision, _revision_type, webaddresstype, confparameters_id) FROM stdin;
\.


--
-- Data for Name: working_time_type_days; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY working_time_type_days (id, breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id) FROM stdin;
\.


--
-- Data for Name: working_time_type_days_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY working_time_type_days_history (id, _revision, _revision_type, breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id) FROM stdin;
\.


--
-- Data for Name: working_time_types; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) FROM stdin;
\.


--
-- Data for Name: working_time_types_history; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY working_time_types_history (id, _revision, _revision_type, description, shift, meal_ticket_enabled, office_id, disabled) FROM stdin;
\.


--
-- Data for Name: year_recaps; Type: TABLE DATA; Schema: public; Owner: epas
--

COPY year_recaps (id, lastmodified, overtime, overtimeap, recg, recgap, recguap, recm, remaining, remainingap, year, person_id) FROM stdin;
\.


--
-- Name: absence_type_groups_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absence_type_groups_history
    ADD CONSTRAINT absence_type_groups_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: absence_type_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absence_type_groups
    ADD CONSTRAINT absence_type_groups_pkey PRIMARY KEY (id);


--
-- Name: absence_types_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absence_types_history
    ADD CONSTRAINT absence_types_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: absence_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absence_types
    ADD CONSTRAINT absence_types_pkey PRIMARY KEY (id);


--
-- Name: absence_types_qualifications_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absence_types_qualifications_history
    ADD CONSTRAINT absence_types_qualifications_history_pkey PRIMARY KEY (_revision, absencetypes_id, qualifications_id);


--
-- Name: absences_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absences_history
    ADD CONSTRAINT absences_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: absences_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY absences
    ADD CONSTRAINT absences_pkey PRIMARY KEY (id);


--
-- Name: auth_users_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY auth_users
    ADD CONSTRAINT auth_users_pkey PRIMARY KEY (id);


--
-- Name: badge_readers_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY badge_readers_history
    ADD CONSTRAINT badge_readers_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: badge_readers_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY badge_readers
    ADD CONSTRAINT badge_readers_pkey PRIMARY KEY (id);


--
-- Name: certificated_data_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY certificated_data_history
    ADD CONSTRAINT certificated_data_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: certificated_data_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY certificated_data
    ADD CONSTRAINT certificated_data_pkey PRIMARY KEY (id);


--
-- Name: competence_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY competence_codes
    ADD CONSTRAINT competence_codes_pkey PRIMARY KEY (id);


--
-- Name: competences_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY competences
    ADD CONSTRAINT competences_pkey PRIMARY KEY (id);


--
-- Name: conf_general_tmp_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_general_history
    ADD CONSTRAINT conf_general_tmp_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: conf_general_tmp_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_general
    ADD CONSTRAINT conf_general_tmp_pkey PRIMARY KEY (id);


--
-- Name: conf_year_tmp_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_year_history
    ADD CONSTRAINT conf_year_tmp_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: conf_year_tmp_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_year
    ADD CONSTRAINT conf_year_tmp_pkey PRIMARY KEY (id);


--
-- Name: configurations_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY configurations_history
    ADD CONSTRAINT configurations_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY configurations
    ADD CONSTRAINT configurations_pkey PRIMARY KEY (id);


--
-- Name: contract_stamp_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY contract_stamp_profiles
    ADD CONSTRAINT contract_stamp_profiles_pkey PRIMARY KEY (id);


--
-- Name: contract_year_recap_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY contract_year_recap
    ADD CONSTRAINT contract_year_recap_pkey PRIMARY KEY (id);


--
-- Name: contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY contracts
    ADD CONSTRAINT contracts_pkey PRIMARY KEY (id);


--
-- Name: contracts_working_time_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY contracts_working_time_types
    ADD CONSTRAINT contracts_working_time_types_pkey PRIMARY KEY (id);


--
-- Name: groups_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY groups_history
    ADD CONSTRAINT groups_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: groups_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: initialization_absences_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY initialization_absences_history
    ADD CONSTRAINT initialization_absences_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: initialization_absences_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT initialization_absences_pkey PRIMARY KEY (id);


--
-- Name: initialization_times_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY initialization_times_history
    ADD CONSTRAINT initialization_times_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: initialization_times_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY initialization_times
    ADD CONSTRAINT initialization_times_pkey PRIMARY KEY (id);


--
-- Name: meal_ticket_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY meal_ticket_history
    ADD CONSTRAINT meal_ticket_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: meal_ticket_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY meal_ticket
    ADD CONSTRAINT meal_ticket_pkey PRIMARY KEY (id);


--
-- Name: office_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY office_history
    ADD CONSTRAINT office_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: office_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY office
    ADD CONSTRAINT office_pkey PRIMARY KEY (id);


--
-- Name: options_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);


--
-- Name: permissions_groups_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY permissions_groups_history
    ADD CONSTRAINT permissions_groups_history_pkey PRIMARY KEY (_revision, permissions_id, groups_id);


--
-- Name: permissions_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY permissions_history
    ADD CONSTRAINT permissions_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: person_children_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_children_history
    ADD CONSTRAINT person_children_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_children_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_children
    ADD CONSTRAINT person_children_pkey PRIMARY KEY (id);


--
-- Name: person_days_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_days_history
    ADD CONSTRAINT person_days_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_days_in_trouble_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_days_in_trouble
    ADD CONSTRAINT person_days_in_trouble_pkey PRIMARY KEY (id);


--
-- Name: person_days_person_id_date_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_days
    ADD CONSTRAINT person_days_person_id_date_key UNIQUE (person_id, date);


--
-- Name: person_days_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_days
    ADD CONSTRAINT person_days_pkey PRIMARY KEY (id);


--
-- Name: person_hour_for_overtime_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_hour_for_overtime
    ADD CONSTRAINT person_hour_for_overtime_pkey PRIMARY KEY (id);


--
-- Name: person_months_recap_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_months_recap_history
    ADD CONSTRAINT person_months_recap_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_months_recap_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_months_recap
    ADD CONSTRAINT person_months_recap_pkey PRIMARY KEY (id);


--
-- Name: person_reperibility_days_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_days_history
    ADD CONSTRAINT person_reperibility_days_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_reperibility_days_person_reperibility_id_date_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT person_reperibility_days_person_reperibility_id_date_key UNIQUE (person_reperibility_id, date);


--
-- Name: person_reperibility_days_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT person_reperibility_days_pkey PRIMARY KEY (id);


--
-- Name: person_reperibility_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_history
    ADD CONSTRAINT person_reperibility_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_reperibility_person_id_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT person_reperibility_person_id_key UNIQUE (person_id);


--
-- Name: person_reperibility_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT person_reperibility_pkey PRIMARY KEY (id);


--
-- Name: person_reperibility_types_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_types_history
    ADD CONSTRAINT person_reperibility_types_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_reperibility_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_reperibility_types
    ADD CONSTRAINT person_reperibility_types_pkey PRIMARY KEY (id);


--
-- Name: person_shift_days_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT person_shift_days_pkey PRIMARY KEY (id);


--
-- Name: person_shift_person_id_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_shift
    ADD CONSTRAINT person_shift_person_id_key UNIQUE (person_id);


--
-- Name: person_shift_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_shift
    ADD CONSTRAINT person_shift_pkey PRIMARY KEY (id);


--
-- Name: person_shift_shift_type_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT person_shift_shift_type_pkey PRIMARY KEY (id);


--
-- Name: person_years_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_years_history
    ADD CONSTRAINT person_years_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: person_years_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY person_years
    ADD CONSTRAINT person_years_pkey PRIMARY KEY (id);


--
-- Name: persons_groups_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY persons_groups_history
    ADD CONSTRAINT persons_groups_history_pkey PRIMARY KEY (_revision, persons_id, groups_id);


--
-- Name: persons_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY persons_history
    ADD CONSTRAINT persons_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: persons_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY persons
    ADD CONSTRAINT persons_pkey PRIMARY KEY (id);


--
-- Name: persons_working_time_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT persons_working_time_types_pkey PRIMARY KEY (id);


--
-- Name: play_evolutions_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY play_evolutions
    ADD CONSTRAINT play_evolutions_pkey PRIMARY KEY (id);


--
-- Name: qualifications_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY qualifications_history
    ADD CONSTRAINT qualifications_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: qualifications_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY qualifications
    ADD CONSTRAINT qualifications_pkey PRIMARY KEY (id);


--
-- Name: revinfo_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY revinfo
    ADD CONSTRAINT revinfo_pkey PRIMARY KEY (rev);


--
-- Name: roles_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY roles_history
    ADD CONSTRAINT roles_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: roles_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: shift_cancelled_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY shift_cancelled
    ADD CONSTRAINT shift_cancelled_pkey PRIMARY KEY (id);


--
-- Name: shift_time_table_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY shift_time_table
    ADD CONSTRAINT shift_time_table_pkey PRIMARY KEY (id);


--
-- Name: shift_type_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY shift_type
    ADD CONSTRAINT shift_type_pkey PRIMARY KEY (id);


--
-- Name: stamp_modification_types_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stamp_modification_types_history
    ADD CONSTRAINT stamp_modification_types_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: stamp_modification_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stamp_modification_types
    ADD CONSTRAINT stamp_modification_types_pkey PRIMARY KEY (id);


--
-- Name: stamp_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stamp_profiles
    ADD CONSTRAINT stamp_profiles_pkey PRIMARY KEY (id);


--
-- Name: stamp_types_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stamp_types_history
    ADD CONSTRAINT stamp_types_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: stamp_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stamp_types
    ADD CONSTRAINT stamp_types_pkey PRIMARY KEY (id);


--
-- Name: stampings_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stampings_history
    ADD CONSTRAINT stampings_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: stampings_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY stampings
    ADD CONSTRAINT stampings_pkey PRIMARY KEY (id);


--
-- Name: total_overtime_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY total_overtime
    ADD CONSTRAINT total_overtime_pkey PRIMARY KEY (id);


--
-- Name: unique_conf_general_integrity_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_general
    ADD CONSTRAINT unique_conf_general_integrity_key UNIQUE (field, office_id);


--
-- Name: unique_conf_year_integrity_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY conf_year
    ADD CONSTRAINT unique_conf_year_integrity_key UNIQUE (field, year, office_id);


--
-- Name: unique_integrity_key; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY competences
    ADD CONSTRAINT unique_integrity_key UNIQUE (person_id, competence_code_id, year, month);


--
-- Name: uro_unique_index; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY users_roles_offices
    ADD CONSTRAINT uro_unique_index UNIQUE (office_id, role_id, user_id);


--
-- Name: users_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY users_history
    ADD CONSTRAINT users_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users_roles_offices_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY users_roles_offices
    ADD CONSTRAINT users_roles_offices_pkey PRIMARY KEY (id);


--
-- Name: vacation_codes_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY vacation_codes_history
    ADD CONSTRAINT vacation_codes_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: vacation_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY vacation_codes
    ADD CONSTRAINT vacation_codes_pkey PRIMARY KEY (id);


--
-- Name: vacation_periods_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY vacation_periods_history
    ADD CONSTRAINT vacation_periods_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: vacation_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT vacation_periods_pkey PRIMARY KEY (id);


--
-- Name: valuable_competences_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY valuable_competences
    ADD CONSTRAINT valuable_competences_pkey PRIMARY KEY (id);


--
-- Name: web_stamping_address_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY web_stamping_address_history
    ADD CONSTRAINT web_stamping_address_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: web_stamping_address_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY web_stamping_address
    ADD CONSTRAINT web_stamping_address_pkey PRIMARY KEY (id);


--
-- Name: working_time_type_days_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY working_time_type_days_history
    ADD CONSTRAINT working_time_type_days_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: working_time_type_days_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY working_time_type_days
    ADD CONSTRAINT working_time_type_days_pkey PRIMARY KEY (id);


--
-- Name: working_time_types_history_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY working_time_types_history
    ADD CONSTRAINT working_time_types_history_pkey PRIMARY KEY (id, _revision);


--
-- Name: working_time_types_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY working_time_types
    ADD CONSTRAINT working_time_types_pkey PRIMARY KEY (id);


--
-- Name: year_recaps_pkey; Type: CONSTRAINT; Schema: public; Owner: epas; Tablespace: 
--

ALTER TABLE ONLY year_recaps
    ADD CONSTRAINT year_recaps_pkey PRIMARY KEY (id);


--
-- Name: conf_general_tmp_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY conf_general
    ADD CONSTRAINT conf_general_tmp_fkey FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: conf_year_tmp_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY conf_year
    ADD CONSTRAINT conf_year_tmp_fkey FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: contract_stamp_profiles_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contract_stamp_profiles
    ADD CONSTRAINT contract_stamp_profiles_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES contracts(id);


--
-- Name: fk10e41b2bd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absences_history
    ADD CONSTRAINT fk10e41b2bd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk160d77e7d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility_history
    ADD CONSTRAINT fk160d77e7d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk16765acbce1b821; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_types_qualifications
    ADD CONSTRAINT fk16765acbce1b821 FOREIGN KEY (qualifications_id) REFERENCES qualifications(id);


--
-- Name: fk16765acd966a951; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_types_qualifications
    ADD CONSTRAINT fk16765acd966a951 FOREIGN KEY (absencetypes_id) REFERENCES absence_types(id);


--
-- Name: fk18bdcd6dd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY working_time_type_days_history
    ADD CONSTRAINT fk18bdcd6dd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk1fe1fac5d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY vacation_periods_history
    ADD CONSTRAINT fk1fe1fac5d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk20975c68e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_hour_for_overtime
    ADD CONSTRAINT fk20975c68e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk218a96913c1ea3e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_competence_codes
    ADD CONSTRAINT fk218a96913c1ea3e FOREIGN KEY (competencecode_id) REFERENCES competence_codes(id);


--
-- Name: fk218a9691dd4eb8b5; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_competence_codes
    ADD CONSTRAINT fk218a9691dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);


--
-- Name: fk2631804cd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_types_history
    ADD CONSTRAINT fk2631804cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk277bbd9d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY permissions_history
    ADD CONSTRAINT fk277bbd9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk27e84399d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY vacation_codes_history
    ADD CONSTRAINT fk27e84399d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk3075811e3df511bb; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT fk3075811e3df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);


--
-- Name: fk3075811eda784c2b; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_shift_days
    ADD CONSTRAINT fk3075811eda784c2b FOREIGN KEY (person_shift_id) REFERENCES person_shift(id);


--
-- Name: fk353559b335555570; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contracts_working_time_types
    ADD CONSTRAINT fk353559b335555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);


--
-- Name: fk353559b3fb1f039e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contracts_working_time_types
    ADD CONSTRAINT fk353559b3fb1f039e FOREIGN KEY (contract_id) REFERENCES contracts(id);


--
-- Name: fk3f7a55d43df511bb; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY shift_cancelled
    ADD CONSTRAINT fk3f7a55d43df511bb FOREIGN KEY (shift_type_id) REFERENCES shift_type(id);


--
-- Name: fk4094eae7e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY initialization_times
    ADD CONSTRAINT fk4094eae7e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk4b732cdbfbe89596; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_days_in_trouble
    ADD CONSTRAINT fk4b732cdbfbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);


--
-- Name: fk4d19b244d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY permissions_groups_history
    ADD CONSTRAINT fk4d19b244d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk5170df704649fe84; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT fk5170df704649fe84 FOREIGN KEY (vacation_codes_id) REFERENCES vacation_codes(id);


--
-- Name: fk5170df70e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY vacation_periods
    ADD CONSTRAINT fk5170df70e7a7b1be FOREIGN KEY (contract_id) REFERENCES contracts(id);


--
-- Name: fk53ee32958c3d68d6; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT fk53ee32958c3d68d6 FOREIGN KEY (absencetype_id) REFERENCES absence_types(id);


--
-- Name: fk53ee3295e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY initialization_absences
    ADD CONSTRAINT fk53ee3295e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk5842d3d9e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY valuable_competences
    ADD CONSTRAINT fk5842d3d9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk5c2b915dd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY users_history
    ADD CONSTRAINT fk5c2b915dd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk5c3f2f67d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stamp_modification_types_history
    ADD CONSTRAINT fk5c3f2f67d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk64d88a51d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY certificated_data_history
    ADD CONSTRAINT fk64d88a51d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk6674c5d65b5f15b1; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absences
    ADD CONSTRAINT fk6674c5d65b5f15b1 FOREIGN KEY (absence_type_id) REFERENCES absence_types(id);


--
-- Name: fk6674c5d6fbe89596; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absences
    ADD CONSTRAINT fk6674c5d6fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);


--
-- Name: fk68e007b9d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY qualifications_history
    ADD CONSTRAINT fk68e007b9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk6ace3a9e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_children
    ADD CONSTRAINT fk6ace3a9e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk71b9ff7730325be3; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_type_groups
    ADD CONSTRAINT fk71b9ff7730325be3 FOREIGN KEY (replacing_absence_type_id) REFERENCES absence_types(id);


--
-- Name: fk74fdda1835555570; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY working_time_type_days
    ADD CONSTRAINT fk74fdda1835555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);


--
-- Name: fk7757fc5e29b090bd; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT fk7757fc5e29b090bd FOREIGN KEY (personshifts_id) REFERENCES person_shift(id);


--
-- Name: fk7757fc5ebbfca55b; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_shift_shift_type
    ADD CONSTRAINT fk7757fc5ebbfca55b FOREIGN KEY (shifttypes_id) REFERENCES shift_type(id);


--
-- Name: fk785e8f1435175bce; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f1435175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types(id);


--
-- Name: fk785e8f148868391d; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f148868391d FOREIGN KEY (badge_reader_id) REFERENCES badge_readers(id);


--
-- Name: fk785e8f14932966bd; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f14932966bd FOREIGN KEY (stamp_type_id) REFERENCES stamp_types(id);


--
-- Name: fk785e8f14fbe89596; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stampings
    ADD CONSTRAINT fk785e8f14fbe89596 FOREIGN KEY (personday_id) REFERENCES person_days(id);


--
-- Name: fk7ab49e924e498a6e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT fk7ab49e924e498a6e FOREIGN KEY (person_reperibility_type_id) REFERENCES person_reperibility_types(id);


--
-- Name: fk7ab49e92e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility
    ADD CONSTRAINT fk7ab49e92e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk7abb85ef34428ea9; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY permissions_groups
    ADD CONSTRAINT fk7abb85ef34428ea9 FOREIGN KEY (permissions_id) REFERENCES permissions(id);


--
-- Name: fk7abb85ef522ebd41; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY permissions_groups
    ADD CONSTRAINT fk7abb85ef522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);


--
-- Name: fk82ea23ccd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_type_groups_history
    ADD CONSTRAINT fk82ea23ccd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk8379a4e6e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stamp_profiles
    ADD CONSTRAINT fk8379a4e6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fk84df567fac97433e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY web_stamping_address
    ADD CONSTRAINT fk84df567fac97433e FOREIGN KEY (confparameters_id) REFERENCES configurations(id);


--
-- Name: fk84ebf2d4d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY web_stamping_address_history
    ADD CONSTRAINT fk84ebf2d4d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk879a9f3cd54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY initialization_times_history
    ADD CONSTRAINT fk879a9f3cd54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk8fa3d556d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_days_history
    ADD CONSTRAINT fk8fa3d556d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk94fa58aad54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY working_time_types_history
    ADD CONSTRAINT fk94fa58aad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fk9afc5dd6e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY year_recaps
    ADD CONSTRAINT fk9afc5dd6e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fka577bbcad54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_groups_history
    ADD CONSTRAINT fka577bbcad54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fka8bd39d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility_days_history
    ADD CONSTRAINT fka8bd39d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkb20b55e41df6de9; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT fkb20b55e41df6de9 FOREIGN KEY (person_reperibility_id) REFERENCES person_reperibility(id);


--
-- Name: fkb20b55e47d5fd20c; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility_days
    ADD CONSTRAINT fkb20b55e47d5fd20c FOREIGN KEY (reperibility_type) REFERENCES person_reperibility_types(id);


--
-- Name: fkb7d05129d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY groups_history
    ADD CONSTRAINT fkb7d05129d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkb943247635555570; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT fkb943247635555570 FOREIGN KEY (working_time_type_id) REFERENCES working_time_types(id);


--
-- Name: fkb9432476e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_working_time_types
    ADD CONSTRAINT fkb9432476e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fkbe7a61ca62728bb1; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY competences
    ADD CONSTRAINT fkbe7a61ca62728bb1 FOREIGN KEY (competence_code_id) REFERENCES competence_codes(id);


--
-- Name: fkbe7a61cae7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY competences
    ADD CONSTRAINT fkbe7a61cae7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fkbf232f8afb1f039e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contract_year_recap
    ADD CONSTRAINT fkbf232f8afb1f039e FOREIGN KEY (contract_id) REFERENCES contracts(id);


--
-- Name: fkbfc8501d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_types_qualifications_history
    ADD CONSTRAINT fkbfc8501d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkc3373ebc2d0fa45e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY office
    ADD CONSTRAINT fkc3373ebc2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: fkc5a3e3e1d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_reperibility_types_history
    ADD CONSTRAINT fkc5a3e3e1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkca05bafce7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY certificated_data
    ADD CONSTRAINT fkca05bafce7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fkcc549f52d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stamp_types_history
    ADD CONSTRAINT fkcc549f52d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkcd87c669d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY stampings_history
    ADD CONSTRAINT fkcd87c669d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkd30e49c1d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_years_history
    ADD CONSTRAINT fkd30e49c1d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkd52a4e11d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY office_history
    ADD CONSTRAINT fkd52a4e11d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkd78fcfbe2d0fa45e; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe2d0fa45e FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: fkd78fcfbe47140efe; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe47140efe FOREIGN KEY (user_id) REFERENCES users(id);


--
-- Name: fkd78fcfbe786a4ab6; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons
    ADD CONSTRAINT fkd78fcfbe786a4ab6 FOREIGN KEY (qualification_id) REFERENCES qualifications(id);


--
-- Name: fkda8f2892d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY configurations_history
    ADD CONSTRAINT fkda8f2892d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkdda67575522ebd41; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_groups
    ADD CONSTRAINT fkdda67575522ebd41 FOREIGN KEY (groups_id) REFERENCES groups(id);


--
-- Name: fkdda67575dd4eb8b5; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_groups
    ADD CONSTRAINT fkdda67575dd4eb8b5 FOREIGN KEY (persons_id) REFERENCES persons(id);


--
-- Name: fke69adb0135175bce; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_days
    ADD CONSTRAINT fke69adb0135175bce FOREIGN KEY (stamp_modification_type_id) REFERENCES stamp_modification_types(id);


--
-- Name: fke69adb01e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_days
    ADD CONSTRAINT fke69adb01e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fke86d11a1e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY contracts
    ADD CONSTRAINT fke86d11a1e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fked96d718e7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_shift
    ADD CONSTRAINT fked96d718e7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fkede9ea6ce7a7b1be; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_years
    ADD CONSTRAINT fkede9ea6ce7a7b1be FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: fkee16b5fed54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_children_history
    ADD CONSTRAINT fkee16b5fed54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkf30286c9d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY badge_readers_history
    ADD CONSTRAINT fkf30286c9d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkf34058ead54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY initialization_absences_history
    ADD CONSTRAINT fkf34058ead54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkfceabd13d54d10ea; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY persons_history
    ADD CONSTRAINT fkfceabd13d54d10ea FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: fkfe65dbf7ca0a1c8a; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY absence_types
    ADD CONSTRAINT fkfe65dbf7ca0a1c8a FOREIGN KEY (absence_type_group_id) REFERENCES absence_type_groups(id);


--
-- Name: meal_ticket_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY meal_ticket
    ADD CONSTRAINT meal_ticket_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES persons(id);


--
-- Name: meal_ticket_history__revision_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY meal_ticket_history
    ADD CONSTRAINT meal_ticket_history__revision_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: meal_ticket_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY meal_ticket
    ADD CONSTRAINT meal_ticket_person_id_fkey FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: person_person_months_recap_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_months_recap
    ADD CONSTRAINT person_person_months_recap_fkey FOREIGN KEY (person_id) REFERENCES persons(id);


--
-- Name: revinfo_conf_general_tmp_history_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY conf_general_history
    ADD CONSTRAINT revinfo_conf_general_tmp_history_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: revinfo_conf_year_tmp_history_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY conf_year_history
    ADD CONSTRAINT revinfo_conf_year_tmp_history_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: revinfo_person_months_recap_history_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY person_months_recap_history
    ADD CONSTRAINT revinfo_person_months_recap_history_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: revinfo_shift_type_history_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY shift_type_history
    ADD CONSTRAINT revinfo_shift_type_history_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: roles_history__revision_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY roles_history
    ADD CONSTRAINT roles_history__revision_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: roles_permissions_history__revision_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY roles_permissions_history
    ADD CONSTRAINT roles_permissions_history__revision_fkey FOREIGN KEY (_revision) REFERENCES revinfo(rev);


--
-- Name: roles_permissions_permissions_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY roles_permissions
    ADD CONSTRAINT roles_permissions_permissions_id_fkey FOREIGN KEY (permissions_id) REFERENCES permissions(id);


--
-- Name: roles_permissions_roles_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY roles_permissions
    ADD CONSTRAINT roles_permissions_roles_id_fkey FOREIGN KEY (roles_id) REFERENCES roles(id);


--
-- Name: shift_type_shift_time_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY shift_type
    ADD CONSTRAINT shift_type_shift_time_table_id_fkey FOREIGN KEY (shift_time_table_id) REFERENCES shift_time_table(id);


--
-- Name: shift_type_supervisor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY shift_type
    ADD CONSTRAINT shift_type_supervisor_fkey FOREIGN KEY (supervisor) REFERENCES persons(id);


--
-- Name: totalovertime_office_key; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY total_overtime
    ADD CONSTRAINT totalovertime_office_key FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: users_roles_offices_office_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY users_roles_offices
    ADD CONSTRAINT users_roles_offices_office_id_fkey FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: users_roles_offices_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY users_roles_offices
    ADD CONSTRAINT users_roles_offices_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id);


--
-- Name: users_roles_offices_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY users_roles_offices
    ADD CONSTRAINT users_roles_offices_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);


--
-- Name: wtt_office_key; Type: FK CONSTRAINT; Schema: public; Owner: epas
--

ALTER TABLE ONLY working_time_types
    ADD CONSTRAINT wtt_office_key FOREIGN KEY (office_id) REFERENCES office(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

