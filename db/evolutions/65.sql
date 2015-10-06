# ---!Ups

ALTER TABLE person_reperibility_types ADD COLUMN supervisor bigint REFERENCES persons(id);
ALTER TABLE person_reperibility_types_history ADD COLUMN supervisor bigint;

-------------------------------------------------------------------------------------------------------------
-- DA QUI IN POI SI FA L'INSERIMENTO DEI RECORD CHE PRIMA VENIVANO INSERITI TRAMITE YML AL BOOTSTRAP --
-------------------------------------------------------------------------------------------------------------

--
-- Data for Name: competence_codes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO competence_codes (code, codetopresence, description) VALUES ('050', '050', 'Ind.tà Meccanografica');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('207', '207', 'Ind.ta'' Reper.ta'' Feriale');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('208', '208', 'Ind.ta'' Reper.ta'' Festiva');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('203', '203', 'Ind.ta'' Maneggio Valori');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('T1', 'T1', 'Turno ordinario');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('S2', 'S2', 'Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('S3', 'S3', 'Straordinario notturno nei giorni festivi');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('S1', 'S1', 'Straordinario diurno nei giorni lavorativi');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('T2', 'T2', 'Turno notturno');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('T3', 'T3', 'Turno festivo');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('042', '042', 'Ind.tà Sede Disagiata');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('205', '205', 'Ind.ta'' Risc.Rad.Ion. Com.1');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('206', '206', 'Ind.ta'' Risc.Rad.Ion. Com.3');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('106', '106', 'Ind.ta'' mansione L.397/71');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('107', '107', 'Ind.ta'' mansione L.397/71 Magg.');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('303', '303', 'Ind.ta'' Risc. Rad. Ion. Com.1');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('304', '304', 'Ind.ta'' Risc. Rad. Ion. Com.3');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('351', '351', 'Ind.ta'' Rischio GR.1 DPR.146');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('352', '352', 'Ind.ta'' Rischio GR.2 DPR.146');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('353', '353', 'Ind.ta'' Rischio GR.3 DPR.146');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('354', '354', 'Ind.ta'' Rischio GR.4 DPR.146');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('355', '355', 'Ind.ta'' Rischio GR.5 DPR.146');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('356', '356', 'Ind.ta'' Rischio Subacquei');
INSERT INTO competence_codes (code, codetopresence, description) VALUES ('367', '367', 'Ind.ta'' Natanti');

--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO roles (name) VALUES ('developer');
INSERT INTO roles (name) VALUES ('admin');
INSERT INTO roles (name) VALUES ('personnelAdmin');
INSERT INTO roles (name) VALUES ('personnelAdminMini');
INSERT INTO roles (name) VALUES ('employee');
INSERT INTO roles (name) VALUES ('badgeReader');
INSERT INTO roles (name) VALUES ('restClient');
INSERT INTO roles (name) VALUES ('shiftManager');
INSERT INTO roles (name) VALUES ('reperibilityManager');


--
-- Data for Name: stamp_modification_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO stamp_modification_types (code, description) VALUES ('p', 'Tempo calcolato togliendo dal tempo di lavoro la durata dell''intervallo pranzo');
INSERT INTO stamp_modification_types (code, description) VALUES ('e', 'Ora di entrata calcolata perché la durata dell''intervallo pranzo è minore del minimo');
INSERT INTO stamp_modification_types (code, description) VALUES ('m', 'Timbratura modificata dall''amministratore');
INSERT INTO stamp_modification_types (code, description) VALUES ('x', 'Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte');
INSERT INTO stamp_modification_types (code, description) VALUES ('f', 'Tempo di lavoro che si avrebbe uscendo adesso');
INSERT INTO stamp_modification_types (code, description) VALUES ('d', 'Considerato presente se non ci sono codici di assenza (orario di lavoro autodichiarato)');


--
-- Data for Name: stamp_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO stamp_types (code, description, identifier) VALUES ('motiviDiServizio', 'Motivi di servizio', 's');
INSERT INTO stamp_types (code, description, identifier) VALUES ('visitaMedica', 'Visita Medica', 'vm');
INSERT INTO stamp_types (code, description, identifier) VALUES ('permessoSindacale', 'Permesso sindacale', 'ps');
INSERT INTO stamp_types (code, description, identifier) VALUES ('incaricoDiInsegnamento', 'Incarico di insegnamento', 'is');
INSERT INTO stamp_types (code, description, identifier) VALUES ('dirittoAlloStudio', 'Diritto allo studio', 'das');
INSERT INTO stamp_types (code, description, identifier) VALUES ('motiviPersonali', 'Motivi personali', 'mp');
INSERT INTO stamp_types (code, description, identifier) VALUES ('reperibilita', 'Reperibilità', 'r');
INSERT INTO stamp_types (code, description, identifier) VALUES ('intramoenia', 'Intramoenia', 'i');
INSERT INTO stamp_types (code, description, identifier) VALUES ('guardiaMedica', 'Guardia Medica', 'gm');


--
-- Data for Name: vacation_codes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO vacation_codes (description, permission_days, vacation_days) VALUES ('28+4', 4, 28);
INSERT INTO vacation_codes (description, permission_days, vacation_days) VALUES ('26+4', 4, 26);
INSERT INTO vacation_codes (description, permission_days, vacation_days) VALUES ('25+4', 4, 25);
INSERT INTO vacation_codes (description, permission_days, vacation_days) VALUES ('21+4', 4, 21);

--
-- Data for Name: working_time_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (1, 'Normale', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (2, 'Maternità', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (4, '50%', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (5, '60%', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (6, 'Maternità Gemellare', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (8, '80%', false, true, NULL, false);
INSERT INTO working_time_types (id, description, shift, meal_ticket_enabled, office_id, disabled) VALUES (9, '85%', false, true, NULL, false);

--
-- Data for Name: working_time_type_days; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 1, false, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 2, false, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 3, false, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 4, false, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 5, false, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 6, true, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 7, true, 360, 0, 0, 0, 0, 0, 0, 432, 1, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 1, false, 312, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 2, false, 312, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 3, false, 312, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 4, false, 312, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 5, false, 312, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 312, 2, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 1, false, 360, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 2, false, 360, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 3, false, 360, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 4, false, 360, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 5, false, 360, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 216, 4, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 1, false, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 2, false, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 3, false, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 4, false, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 5, false, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 260, 5, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 1, false, 192, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 2, false, 192, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 3, false, 192, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 4, false, 192, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 5, false, 192, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 192, 6, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 1, false, 600, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 2, false, 600, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 3, false, 600, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 4, false, 600, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 5, false, 600, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 345, 8, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 1, false, 360, 0, 0, 0, 0, 0, 0, 367, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 2, false, 360, 0, 0, 0, 0, 0, 0, 367, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (3, 3, false, 360, 0, 0, 0, 0, 0, 0, 367, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 4, false, 360, 0, 0, 0, 0, 0, 0, 367, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (30, 5, false, 360, 0, 0, 0, 0, 0, 0, 367, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 6, true, 0, 0, 0, 0, 0, 0, 0, 0, 9, NULL, NULL);
INSERT INTO working_time_type_days (breaktickettime, dayofweek, holiday, mealtickettime, timemealfrom, timemealto, timeslotentrancefrom, timeslotentranceto, timeslotexitfrom, timeslotexitto, workingtime, working_time_type_id, ticket_afternoon_threshold, ticket_afternoon_working_time) VALUES (0, 7, true, 0, 0, 0, 0, 0, 0, 0, 0, 9, NULL, NULL);

# ---!Downs

ALTER TABLE person_reperibility_types DROP COLUMN supervisor;
ALTER TABLE person_reperibility_types_history DROP COLUMN supervisor;

DELETE FROM working_time_type_days;
DELETE FROM working_time_types;
DELETE FROM vacation_codes;
DELETE FROM competence_codes;
DELETE FROM stamp_modification_types;
DELETE FROM stamp_types;
DELETE FROM roles;