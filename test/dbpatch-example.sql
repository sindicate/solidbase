
--* // Copyright 2006 René M. de Bloois

--* // Licensed under the Apache License, Version 2.0 (the "License");
--* // you may not use this file except in compliance with the License.
--* // You may obtain a copy of the License at

--* //     http://www.apache.org/licenses/LICENSE-2.0

--* // Unless required by applicable law or agreed to in writing, software
--* // distributed under the License is distributed on an "AS IS" BASIS,
--* // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--* // See the License for the specific language governing permissions and
--* // limitations under the License.

--*	PATCHES

--*		// De init patch creeert tabellen voor de patchtool zelf
--*		INIT "" --> "INIT"

--*		// hier beginnen de echte patches
--*		PATCH "INIT" --> "3.0.1"
--*		PATCH "3.0.1" --> "3.0.2"
--*		PATCH "3.0.2" --> "3.0.3"
--*		PATCH "3.0.3" --> "3.0.4"
--*		PATCH OPEN "3.0.4" --> "3.1.0"
--*		PATCH OPEN "3.0.4" --> "3.0.5"

--*	/PATCHES







--* INIT "" --> "INIT"

--* // Check old style version
ASSERT EXISTS MESSAGE 'Expecting old style version 2.0.17'
SELECT *
FROM TTS.VERSION_CONTROL
WHERE VERS_CONT_ID = 'VERSION'
AND VERS_REG_NUM = 'DHL TTS 2.0.17'
GO

--* // DROP TRIGGER TTS.DBVERSIONLOG_AUTONUMBER;
--* // DROP TABLE TTS.DBVERSION;
--* // DROP TABLE TTS.DBVERSIONLOG;
--* // DROP SEQUENCE TTS.DBVERSIONLOG_SEQUENCE;

--* // Create version control table
CREATE TABLE TTS.DBVERSION
( 
	VERSION VARCHAR2(20) NOT NULL, 
	TARGET VARCHAR2(20), 
	STATEMENTS INTEGER NOT NULL 
)
GO

--* // Create version control log table sequence generator
CREATE SEQUENCE TTS.DBVERSIONLOG_SEQUENCE
GO

--* // Create version control log table
CREATE TABLE TTS.DBVERSIONLOG
(
	ID INTEGER NOT NULL, -- An index might be needed here when you use an autoincrement identity column
	SOURCE VARCHAR2(20),
	TARGET VARCHAR2(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP DATE NOT NULL,
	COMMAND VARCHAR2(4000),
	RESULT VARCHAR2(4000)
)
GO

--* // Create version control log table autonumber trigger
CREATE OR REPLACE TRIGGER TTS.DBVERSIONLOG_AUTONUMBER BEFORE INSERT ON TTS.DBVERSIONLOG
REFERENCING NEW AS NEWROW
FOR EACH ROW
BEGIN
	IF :NEWROW.ID IS NULL THEN
		SELECT TTS.DBVERSIONLOG_SEQUENCE.NEXTVAL INTO :NEWROW.ID FROM DUAL;
	END IF;
END;
GO

--* // Insert the first version
INSERT INTO TTS.DBVERSION ( VERSION, TARGET, STATEMENTS )
VALUES ( 'INIT', NULL, 6  )
GO

--* /INIT







--* PATCH "INIT" --> "3.0.1"

--* SET USER SYSTEM

--* MESSAGE START 'Dropping user TTS_DATAMART'
DROP USER TTS_DATAMART CASCADE
GO

--* SET USER tts

--* MESSAGE START 'Dropping table TTS.REJECTED_SHIPMENTS'
DROP TABLE TTS.REJECTED_SHIPMENTS PURGE
GO

--* MESSAGE START 'Dropping table TTS.BASE_TABLE'
DROP TABLE TTS.BASE_TABLE PURGE
GO

--* MESSAGE START 'Dropping table TTS.DHL_BATCH'
DROP TABLE TTS.DHL_BATCH PURGE
GO

--* MESSAGE START 'Dropping table TTS.BATCH_STATUS'
DROP TABLE TTS.BATCH_STATUS PURGE
GO

--* MESSAGE START 'Dropping table TTS.BATCH_PROCESSES'
DROP TABLE TTS.BATCH_PROCESSES PURGE
GO

--* MESSAGE START 'Dropping sequence TTS.SEQ_DHLB'
DROP SEQUENCE TTS.SEQ_DHLB
GO

--* MESSAGE START 'Dropping columns APPROVAL_PROCESS.DEST_TYPE and APPROVAL_PROCESS.ORIGIN_TYPE'
ALTER TABLE TTS.APPROVAL_PROCESS DROP CONSTRAINT APPP_DEST_CK
GO
ALTER TABLE TTS.APPROVAL_PROCESS DROP CONSTRAINT APPP_ORIGIN_CK
GO
ALTER TABLE TTS.APPROVAL_PROCESS DROP ( DEST_TYPE, ORIGIN_TYPE )
GO
ALTER TABLE TTS.APPROVAL_PROCESS ADD
(
	CONSTRAINT APPP_CHECK_DEST CHECK ( REGI_ID_DEST IS NOT NULL AND COUN_ID_DEST IS NULL OR REGI_ID_DEST IS NULL AND COUN_ID_DEST IS NOT NULL ),
	CONSTRAINT APPP_CHECK_ORIG CHECK ( REGI_ID_ORIGIN IS NOT NULL AND COUN_ID_ORIGIN IS NULL OR REGI_ID_ORIGIN IS NULL AND COUN_ID_ORIGIN IS NOT NULL )
)
GO

--* MESSAGE START 'Altering some id datatypes to INTEGER'
ALTER TABLE TTS.APPROVAL_PROCESS MODIFY ( APPP_ID INTEGER, USER_ID INTEGER, REGI_ID_DEST INTEGER, COUN_ID_DEST INTEGER, REGI_ID_ORIGIN INTEGER, COUN_ID_ORIGIN INTEGER )
GO
ALTER TABLE TTS.COUNTRIES MODIFY ( REGI_ID INTEGER )
GO
ALTER TABLE TTS.LANE_PRODUCT_GROUPS MODIFY ( LAPG_ID INTEGER, STAT_ID_DEST INTEGER, STAT_ID_ORIGIN INTEGER )
GO
ALTER TABLE TTS.LOG_DHL MODIFY ( LOGD_ID INTEGER )
GO
ALTER TABLE TTS.PROPOSALS MODIFY ( PROP_ID INTEGER, LAPG_ID INTEGER, USER_ID INTEGER )
GO
ALTER TABLE TTS.PROPOSAL_APPROVALS MODIFY ( PROA_ID INTEGER, PROP_ID INTEGER, APPP_ID INTEGER )
GO
ALTER TABLE TTS.PROPOSAL_APPROVAL_EMAILS MODIFY ( PROA_ID INTEGER )
GO
ALTER TABLE TTS.REGIONS MODIFY ( REGI_ID INTEGER )
GO
ALTER TABLE TTS.STATIONS MODIFY ( STAT_ID INTEGER, COUN_ID INTEGER )
GO
ALTER TABLE TTS.USERS MODIFY ( USER_ID INTEGER, COUN_ID INTEGER )
GO
ALTER TABLE TTS.USER_AUTHORITIES MODIFY ( USER_ID INTEGER )
GO
ALTER TABLE TTS.USER_COUNTRIES MODIFY ( USER_ID INTEGER, COUN_ID INTEGER )
GO
ALTER TABLE TTS.USER_REGIONS MODIFY ( USER_ID INTEGER, REGI_ID INTEGER )
GO

--* MESSAGE START 'Fixing datatype of COUNTRIES.COUN_ID'
ALTER TABLE TTS.COUNTRIES RENAME COLUMN COUN_ID TO COUN_ID_OLD
GO
ALTER TABLE TTS.COUNTRIES ADD ( COUN_ID INTEGER )
GO
UPDATE TTS.COUNTRIES SET COUN_ID = COUN_ID_OLD
GO
ALTER TABLE TTS.COUNTRIES MODIFY ( COUN_ID NOT NULL )
GO
ALTER TABLE TTS.COUNTRIES DROP ( COUN_ID_OLD ) CASCADE CONSTRAINTS
GO
ALTER TABLE TTS.COUNTRIES ADD ( CONSTRAINT COUN_PK PRIMARY KEY ( COUN_ID ) USING INDEX TABLESPACE TTSIND )
GO
ALTER TABLE TTS.APPROVAL_PROCESS ADD ( CONSTRAINT APPP_FK_COUN_ORIG FOREIGN KEY ( COUN_ID_ORIGIN ) REFERENCES TTS.COUNTRIES ( COUN_ID ) )
GO
ALTER TABLE TTS.APPROVAL_PROCESS ADD ( CONSTRAINT APPP_FK_COUN_DEST FOREIGN KEY ( COUN_ID_DEST ) REFERENCES TTS.COUNTRIES ( COUN_ID ) )
GO
ALTER TABLE TTS.STATIONS ADD ( CONSTRAINT STAT_FK_COUN FOREIGN KEY ( COUN_ID ) REFERENCES TTS.COUNTRIES ( COUN_ID ) )
GO
ALTER TABLE TTS.USERS ADD ( CONSTRAINT USER_FK_COUN FOREIGN KEY ( COUN_ID ) REFERENCES TTS.COUNTRIES ( COUN_ID ) )
GO
ALTER TABLE TTS.USER_COUNTRIES ADD ( CONSTRAINT USEC_FK_COUN FOREIGN KEY ( COUN_ID ) REFERENCES TTS.COUNTRIES ( COUN_ID ) )
GO

--* MESSAGE START 'Changing datatype of TARGET_EXPORT.PICKUP_CUTOFF to NUMBER(4)'
ALTER TABLE TTS.TARGET_EXPORT RENAME COLUMN PICKUP_CUTOFF TO PICKUP_CUTOFF_OLD
GO
ALTER TABLE TTS.TARGET_EXPORT ADD ( PICKUP_CUTOFF NUMBER(4) )
GO
UPDATE TTS.TARGET_EXPORT SET PICKUP_CUTOFF = TO_NUMBER( TO_CHAR( PICKUP_CUTOFF_OLD, 'HH24MI' ) )
GO
ALTER TABLE TTS.TARGET_EXPORT MODIFY ( PICKUP_CUTOFF NOT NULL )
GO
ALTER TABLE TTS.TARGET_EXPORT DROP ( PICKUP_CUTOFF_OLD )
GO

--* MESSAGE START 'Changing type of TIMEBANDS.LOWERBOUND and TIMEBANDS.UPPERBOUND to NUMBER(4)'
ALTER TABLE TTS.TIMEBANDS ADD ( LOWERBOUND2 NUMBER(4), UPPERBOUND2 NUMBER(4) )
GO
UPDATE TTS.TIMEBANDS SET LOWERBOUND2 = TO_NUMBER( TO_CHAR( LOWERBOUND, 'HH24MI' ) ), UPPERBOUND2 = TO_NUMBER( TO_CHAR( UPPERBOUND, 'HH24MI' ) )
GO
ALTER TABLE TTS.TIMEBANDS MODIFY ( LOWERBOUND2 NOT NULL, UPPERBOUND2 NOT NULL )
GO
UPDATE TTS.TIMEBANDS SET UPPERBOUND2 = 2400 WHERE TIME_ID = 96
GO
CREATE UNIQUE INDEX TIME_LOWERBOUND ON TTS.TIMEBANDS ( LOWERBOUND2 ) TABLESPACE TTSIND
GO
ALTER TABLE TTS.TIMEBANDS DROP ( LOWERBOUND, UPPERBOUND )
GO
ALTER TABLE TTS.TIMEBANDS RENAME COLUMN LOWERBOUND2 TO LOWERBOUND
GO
ALTER TABLE TTS.TIMEBANDS RENAME COLUMN UPPERBOUND2 TO UPPERBOUND
GO

--* MESSAGE START 'Updating TIMEBANDS.DISPLAY_LABEL'
UPDATE TIMEBANDS
SET DISPLAY_LABEL = SUBSTR( '0' || TRUNC( UPPERBOUND / 100 ), -2 ) || ':' || SUBSTR( '0' || MOD( UPPERBOUND, 100 ), -2 ) 
GO

--* MESSAGE START 'Creating unique index on TIMEBANDS.UPPERBOUND'
CREATE UNIQUE INDEX TIME_UPPERBOUND ON TTS.TIMEBANDS ( UPPERBOUND ) TABLESPACE TTSIND
GO

--* MESSAGE START 'Creating table TTS.LPG_TARGETS'
CREATE TABLE TTS.LPG_TARGETS
(
	LAPG_ID INTEGER NOT NULL,
	LPGT_DOW NUMBER(1) NOT NULL,
	LPGT_TARGET_DAYS NUMBER(2),
	LPGT_AM_PM_DELIVERY NUMBER(2),
	LPGT_PICKUP_CUTOFF NUMBER(4),
	LPGT_SYSPROP_TARGET_DAYS NUMBER(2),
	LPGT_SYSPROP_AM_PM_DELIVERY NUMBER(2),
	LPGT_SYSPROP_PICKUP_CUTOFF NUMBER(4),
	LPGT_PERFORMANCE NUMBER(3),
	CONSTRAINT LPGT_PK PRIMARY KEY ( LAPG_ID, LPGT_DOW ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT LPGT_FK_LAPG FOREIGN KEY ( LAPG_ID ) REFERENCES TTS.LANE_PRODUCT_GROUPS ( LAPG_ID ),
	CONSTRAINT LPGT_CHECK_DOW CHECK ( LPGT_DOW IN ( 1, 2, 3, 4, 5, 6, 7 ) ),
	CONSTRAINT LPGT_CHECK_AM_PM CHECK ( LPGT_AM_PM_DELIVERY IN ( 12, 24 ) AND LPGT_SYSPROP_AM_PM_DELIVERY IN ( 12, 24 ) ),
	CONSTRAINT LPGT_CHECK_TARGET CHECK
	(
		LPGT_TARGET_DAYS IS NULL AND LPGT_AM_PM_DELIVERY IS NULL AND LPGT_PICKUP_CUTOFF IS NULL OR
		LPGT_TARGET_DAYS IS NOT NULL AND LPGT_AM_PM_DELIVERY IS NOT NULL AND LPGT_PICKUP_CUTOFF IS NOT NULL
	),
	CONSTRAINT LPGT_CHECK_SYSPROP_TARGET CHECK
	(
		LPGT_SYSPROP_TARGET_DAYS IS NULL AND LPGT_SYSPROP_AM_PM_DELIVERY IS NULL AND LPGT_SYSPROP_PICKUP_CUTOFF IS NULL OR
		LPGT_SYSPROP_TARGET_DAYS IS NOT NULL AND LPGT_SYSPROP_AM_PM_DELIVERY IS NOT NULL AND LPGT_SYSPROP_PICKUP_CUTOFF IS NOT NULL
	)
)
GO

--* MESSAGE START 'Dropping unused columns from table TTS.STATIONS'
ALTER TABLE TTS.STATIONS
DROP ( MUST_WIN, WE_INBOUND, WE_OUTBOUND, WE_ECX )
GO

--* MESSAGE START 'Creating table TTS.PROPOSAL_TARGETS'
CREATE TABLE TTS.PROPOSAL_TARGETS
(
	PROP_ID INTEGER NOT NULL,
	PROT_DOW NUMBER(1) NOT NULL,
	PROT_TARGET_DAYS NUMBER(2) NOT NULL,
	PROT_AM_PM_DELIVERY NUMBER(2) NOT NULL,
	PROT_PICKUP_CUTOFF NUMBER(4) NOT NULL,
	CONSTRAINT PROT_PK PRIMARY KEY ( PROP_ID, PROT_DOW ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT PROT_FK_PROP FOREIGN KEY ( PROP_ID ) REFERENCES TTS.PROPOSALS ( PROP_ID ),
	CONSTRAINT PROT_CHECK_DOW CHECK ( PROT_DOW IN ( 1, 2, 3, 4, 5, 6, 7 ) ),
	CONSTRAINT PROT_CHECK_AM_PM CHECK ( PROT_AM_PM_DELIVERY IN ( 12, 24 ) )
)
GO

--* MESSAGE START 'Creating table TTS.SHIPMENT_COUNTS'
CREATE TABLE TTS.SHIPMENT_COUNTS
(
	LAPG_ID INTEGER NOT NULL,
	PU_DAY_OF_WEEK NUMBER(1) NOT NULL,
	TIME_ID NUMBER(4) NOT NULL,
	ADJUSTED_TRANSIT_TIME NUMBER(2) NOT NULL,
	AM_PM_DELIVERY NUMBER(2) NOT NULL,
	SHIPMENT_COUNT INTEGER NOT NULL,
	CONSTRAINT SHIC_FK_LAPG FOREIGN KEY ( LAPG_ID ) REFERENCES TTS.LANE_PRODUCT_GROUPS ( LAPG_ID ),
	CONSTRAINT SHIC_CHECK_DOW CHECK ( PU_DAY_OF_WEEK IN ( 1, 2, 3, 4, 5, 6, 7 ) ),
	CONSTRAINT SHIC_CHECK_TRANSIT_TIME CHECK ( ADJUSTED_TRANSIT_TIME > 0 ),
	CONSTRAINT SHIC_CHECK_AM_PM CHECK ( AM_PM_DELIVERY IN ( 12, 24 ) ),
	CONSTRAINT SHIC_CHECK_COUNT CHECK ( SHIPMENT_COUNT > 0 ),
	CONSTRAINT SHIC_CHECK_TIME_ID CHECK ( TIME_ID BETWEEN 1 AND 96 )
)
GO
CREATE INDEX TTS.SHIC_FK_LAPG ON TTS.SHIPMENT_COUNTS ( LAPG_ID ) TABLESPACE TTSIND
GO

--* MESSAGE START 'Creating table TTS.USER_SETTINGS'
CREATE TABLE TTS.USER_SETTINGS
(
	USES_ID INTEGER NOT NULL,
	USER_ID INTEGER NOT NULL,
	USES_KEY VARCHAR2(40) NOT NULL,
	USES_VALUE VARCHAR2(100),
	USES_SEQUENCE NUMBER(4),
	USES_BLOB BLOB,
	CONSTRAINT USES_PK PRIMARY KEY ( USES_ID ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT USES_FK_USER FOREIGN KEY ( USER_ID ) REFERENCES TTS.USERS ( USER_ID ),
	CONSTRAINT USES_CHECK_SEQUENCE CHECK ( USES_SEQUENCE > 0 )
)
GO
CREATE INDEX TTS.USES_FK_USER ON TTS.USER_SETTINGS ( USER_ID ) TABLESPACE TTSIND
GO
CREATE SEQUENCE TTS.SEQ_USES CACHE 10
GO

--* MESSAGE START 'Adding columns AM_PM_DELIVERY and PU_DAY_OF_WEEK to TTS.TARGET_EXPORT'
ALTER TABLE TTS.TARGET_EXPORT ADD
(
	AM_PM_DELIVERY NUMBER(2) NOT NULL,
	PU_DAY_OF_WEEK NUMBER(1) NOT NULL
)
GO

--* MESSAGE START 'Creating table TTS.COMMODITIES'
CREATE TABLE TTS.COMMODITIES
(
	COMM_CODE NUMBER(3) NOT NULL,
	COMM_DESCRIPTION VARCHAR2(45) NOT NULL,
	CONSTRAINT COMM_PK PRIMARY KEY ( COMM_CODE ) USING INDEX TABLESPACE TTSIND
)
GO
CREATE UNIQUE INDEX TTS.COMM_DESCRIPTION ON TTS.COMMODITIES ( COMM_DESCRIPTION ) TABLESPACE TTSIND
GO

--* MESSAGE START 'Creating table TTS.DELAYS_COMMODITY_DEST'
CREATE TABLE TTS.DELAYS_COMMODITY_DEST
(
	COUN_ID INTEGER NOT NULL,
	COMM_CODE NUMBER(3) NOT NULL,
	DECD_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DELC_PK PRIMARY KEY ( COUN_ID, COMM_CODE ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DELC_FK_COUN FOREIGN KEY ( COUN_ID ) REFERENCES TTS.COUNTRIES ( COUN_ID ),
	CONSTRAINT DELC_FK_COMM FOREIGN KEY ( COMM_CODE ) REFERENCES TTS.COMMODITIES ( COMM_CODE ),
	CONSTRAINT DELC_CHECK_DELAY CHECK ( DECD_DELAY > 0 )
)
GO
CREATE INDEX TTS.DELC_FK_COMM ON TTS.DELAYS_COMMODITY_DEST ( COMM_CODE ) TABLESPACE TTSIND
GO

--* MESSAGE START 'Creating table TTS.DELAYS_WEIGHT_ORIG'
CREATE TABLE TTS.DELAYS_WEIGHT_ORIG
(
	STAT_ID INTEGER NOT NULL,
	DEWO_START NUMBER(10,4) NOT NULL,
	DEWO_END NUMBER(10,4),
	DEWO_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DEWO_PK PRIMARY KEY ( STAT_ID, DEWO_START ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DEWO_FK_STAT FOREIGN KEY ( STAT_ID ) REFERENCES TTS.STATIONS ( STAT_ID ),
	CONSTRAINT DEWO_CHECK_DELAY CHECK ( DEWO_DELAY > 0 )
)
GO

--* MESSAGE START 'Creating table TTS.DELAYS_WEIGHT_DEST'
CREATE TABLE TTS.DELAYS_WEIGHT_DEST
(
	STAT_ID INTEGER NOT NULL,
	DEWD_START NUMBER(10,4) NOT NULL,
	DEWD_END NUMBER(10,4),
	DEWD_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DEWD_PK PRIMARY KEY ( STAT_ID, DEWD_START ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DEWD_FK_STAT FOREIGN KEY ( STAT_ID ) REFERENCES TTS.STATIONS ( STAT_ID ),
	CONSTRAINT DEWD_CHECK_DELAY CHECK ( DEWD_DELAY > 0 )
)
GO

--* MESSAGE START 'Creating table TTS.DELAYS_VALUE_DEST'
CREATE TABLE TTS.DELAYS_VALUE_DEST
(
	COUN_ID INTEGER NOT NULL,
	DEVD_START NUMBER(11,2) NOT NULL,
	DEVD_END NUMBER(11,2),
	DEVD_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DEVD_PK PRIMARY KEY ( COUN_ID, DEVD_START ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DEVD_FK_COUN FOREIGN KEY ( COUN_ID ) REFERENCES TTS.COUNTRIES ( COUN_ID ),
	CONSTRAINT DEVD_CHECK_DELAY CHECK ( DEVD_DELAY > 0 )
)
GO

--* MESSAGE START 'Creating table TTS.DELAYS_POSTCODE_ORIG'
CREATE TABLE TTS.DELAYS_POSTCODE_ORIG
(
	STAT_ID INTEGER NOT NULL,
	DEPO_POSTCODE_START VARCHAR2(12) NOT NULL,
	DEPO_POSTCODE_END VARCHAR2(12) NOT NULL,
	DEPO_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DEPO_PK PRIMARY KEY ( STAT_ID, DEPO_POSTCODE_START ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DEPO_FK_STAT FOREIGN KEY ( STAT_ID ) REFERENCES TTS.STATIONS ( STAT_ID ),
	CONSTRAINT DEPO_CHECK_DELAY CHECK ( DEPO_DELAY > 0 )
)
GO

--* MESSAGE START 'Creating table TTS.DELAYS_POSTCODE_DEST'
CREATE TABLE TTS.DELAYS_POSTCODE_DEST
(
	STAT_ID INTEGER NOT NULL,
	DEPD_POSTCODE_START VARCHAR2(12) NOT NULL,
	DEPD_POSTCODE_END VARCHAR2(12) NOT NULL,
	DEPD_DELAY NUMBER(2) NOT NULL,
	CONSTRAINT DEPD_PK PRIMARY KEY ( STAT_ID, DEPD_POSTCODE_START ) USING INDEX TABLESPACE TTSIND,
	CONSTRAINT DEPD_FK_STAT FOREIGN KEY ( STAT_ID ) REFERENCES TTS.STATIONS ( STAT_ID ),
	CONSTRAINT DEPD_CHECK_DELAY CHECK ( DEPD_DELAY > 0 )
)
GO

--* MESSAGE START 'Increasing the sequence cachesizes'
ALTER SEQUENCE TTS.SEQ_APPP CACHE 100 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_COUN CACHE 10 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_LAPG CACHE 1000 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_LOGD CACHE 10 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_PROA CACHE 1000 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_PROP CACHE 1000 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_REGI NOCACHE NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_STAT CACHE 10 NOMAXVALUE
GO
ALTER SEQUENCE TTS.SEQ_USER NOCACHE NOMAXVALUE
GO

--* MESSAGE START 'Fixing the start value of the SEQ_APPP sequence'
DECLARE
	LL_MAX INTEGER;
	LL_CURR INTEGER;
BEGIN
	SELECT MAX( APPP_ID ) INTO LL_MAX FROM TTS.APPROVAL_PROCESS;
	SELECT SEQ_APPP.NEXTVAL INTO LL_CURR FROM DUAL;
	IF LL_MAX > LL_CURR THEN
		EXECUTE IMMEDIATE 'ALTER SEQUENCE TTS.SEQ_APPP INCREMENT BY ' || ( LL_MAX - LL_CURR );
		SELECT TTS.SEQ_APPP.NEXTVAL INTO LL_CURR FROM DUAL;
		EXECUTE IMMEDIATE 'ALTER SEQUENCE TTS.SEQ_APPP INCREMENT BY 1';
	END IF;
END;
GO

--* MESSAGE START 'Adding columns LAPG_MONTH_IMPORTED and LAPG_PERFORMANCE to TTS.LANE_PRODUCT_GROUPS'
ALTER TABLE TTS.LANE_PRODUCT_GROUPS
ADD ( LAPG_MONTH_IMPORTED NUMBER(6), LAPG_PERFORMANCE NUMBER(3) )
GO

--* MESSAGE START 'Creating global temporary table TTS.TEMP_IDS1'
CREATE GLOBAL TEMPORARY TABLE TTS.TEMP_IDS1 ( TEMI_ID INTEGER NOT NULL )
GO
CREATE INDEX TTS.TEMI_PK ON TTS.TEMP_IDS1 ( TEMI_ID )
GO

--* MESSAGE START 'Adding new system settings to TTS.SYSTEM_SETTINGS'
INSERT INTO SYSTEM_SETTINGS ( SYSS_ID, NAME, VALUE, DESCRIPTION,DATA_TYPE ) VALUES ( 31, 'calculationRuleMethod', 'max', 'Calculation Rule Method', 'C' )
GO
INSERT INTO SYSTEM_SETTINGS ( SYSS_ID, NAME, VALUE, DESCRIPTION,DATA_TYPE ) VALUES ( 32, 'calculationRuleHighValue', '0', 'Calculation Rule High Value percentage', 'I' )
GO
INSERT INTO SYSTEM_SETTINGS ( SYSS_ID, NAME, VALUE, DESCRIPTION,DATA_TYPE ) VALUES ( 33, 'calculationRuleCommodity', '0', 'Calculation Rule Commodity percentage', 'I' )
GO

--* /PATCH







--* PATCH "3.0.1" --> "3.0.2"

--* MESSAGE START 'Dropping package TTS.PCK_DHL_BATCH'
DROP PACKAGE TTS.PCK_DHL_BATCH
GO

--* MESSAGE START 'Dropping procedure TTS.PRC_UPDATE_VERSION'
DROP PROCEDURE TTS.PRC_UPDATE_VERSION
GO

--* MESSAGE START 'Dropping package TTS.PCK_TTS_MLTT'
DROP PACKAGE TTS.PCK_TTS_MLTT
GO

--* MESSAGE START 'Dropping package TTS.PCK_TTS_GEOGROUP'
DROP PACKAGE TTS.PCK_TTS_GEOGROUP
GO

--* MESSAGE START 'Upgrading procedure TTS.PRC_REMOVE_IATA_NO_COMMIT'
CREATE OR REPLACE PROCEDURE "TTS"."PRC_REMOVE_IATA_NO_COMMIT" ( p_iata_code IN VARCHAR2 ) IS

	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_remove_iata_no_commit';	

	l_records_processed log_dhl.records_processed%TYPE := 0;	
	l_records_lanes PLS_INTEGER := 0;
	l_records_ctt PLS_INTEGER := 0;
	l_records_prae PLS_INTEGER := 0;
	l_records_proa PLS_INTEGER := 0;
	l_records_prop PLS_INTEGER := 0;
	l_records_lpg PLS_INTEGER := 0;
	l_records_dpo PLS_INTEGER := 0;
	l_records_dpd PLS_INTEGER := 0;
	l_records_dwo PLS_INTEGER := 0;
	l_records_dwd PLS_INTEGER := 0;
	l_records_sta PLS_INTEGER := 0;
	l_temp_proc PLS_INTEGER := 0;
	
BEGIN

	FOR r_lanes IN
	(
		SELECT DISTINCT LAPG.LAPG_ID
		FROM TTS.STATIONS STAT
		JOIN TTS.LANE_PRODUCT_GROUPS LAPG
			ON LAPG.STAT_ID_DEST = STAT.STAT_ID OR LAPG.STAT_ID_ORIGIN = STAT.STAT_ID
		WHERE STAT.IATA_CODE = p_iata_code
	)
	LOOP

		DELETE FROM TTS.SHIPMENT_COUNTS
		WHERE LAPG_ID = r_lanes.lapg_id;
		
		l_temp_proc := SQL%ROWCOUNT;
		l_records_ctt := l_records_ctt + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

		DELETE FROM 
		(
			SELECT PRAE.ROWID
			FROM TTS.PROPOSAL_APPROVAL_EMAILS PRAE, TTS.PROPOSAL_APPROVALS PROA, TTS.PROPOSALS PROP
			WHERE PROA.PROA_ID = PRAE.PROA_ID
			AND PROP.PROP_ID = PROA.PROP_ID
			AND PROP.LAPG_ID = r_lanes.lapg_id
		);

		l_temp_proc := SQL%ROWCOUNT;
		l_records_prae := l_records_prae + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

		DELETE FROM 
		(
			SELECT PROA.ROWID
			FROM TTS.PROPOSAL_APPROVALS PROA, TTS.PROPOSALS PROP
			WHERE PROP.PROP_ID = PROA.PROP_ID
			AND PROP.LAPG_ID = r_lanes.lapg_id
		);

		l_temp_proc := SQL%ROWCOUNT;
		l_records_proa := l_records_proa + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

		DELETE FROM 
		(
			SELECT PROT.ROWID
			FROM TTS.PROPOSAL_TARGETS PROT, TTS.PROPOSALS PROP
			WHERE PROP.PROP_ID = PROT.PROP_ID
			AND PROP.LAPG_ID = r_lanes.lapg_id
		);

		DELETE FROM TTS.PROPOSALS PROP
		WHERE PROP.LAPG_ID = r_lanes.lapg_id;

		l_temp_proc := SQL%ROWCOUNT;
		l_records_prop := l_records_prop + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

		DELETE FROM TTS.LPG_TARGETS
		WHERE LAPG_ID = r_lanes.lapg_id;

		DELETE FROM TTS.LANE_PRODUCT_GROUPS
		WHERE LAPG_ID = r_lanes.lapg_id;

		l_temp_proc := SQL%ROWCOUNT;
		l_records_lpg := l_records_lpg + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

	END LOOP;

	DELETE FROM TTS.DELAYS_POSTCODE_DEST
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );

	l_temp_proc := SQL%ROWCOUNT;
	l_records_dpd := l_records_dpd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;	
	
	DELETE FROM TTS.DELAYS_POSTCODE_ORIG
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dpo := l_records_dpo + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	DELETE FROM TTS.DELAYS_WEIGHT_DEST
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dwd := l_records_dwd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	DELETE FROM TTS.DELAYS_WEIGHT_ORIG
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dwo := l_records_dwo + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM TTS.STATIONS
	WHERE IATA_CODE = p_iata_code;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_sta := l_records_sta + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	INSERT INTO TTS.log_dhl ( logd_id, process_name, batch, procedure_name, logging_timestamp, log_level, records_processed, parameter_text )
	VALUES ( seq_logd.NEXTVAL, C_PROCESS_NAME, 0, C_PROCEDURE_NAME, SYSDATE, 0, l_records_processed, 'Removed iata ' || p_iata_code );

	dbms_output.put_line( 'Removing for iata [' || p_iata_code  || '] a total of ' || l_records_processed || ' records:' );
	dbms_output.put_line( ' from CUM_TRANSIT_TIMES: ' || l_records_ctt );
	dbms_output.put_line( ' from PROPOSAL_APPROVAL_EMAILS: ' || l_records_prae );
	dbms_output.put_line( ' from PROPOSAL_APPROVALS: ' || l_records_proa );
	dbms_output.put_line( ' from PROPOSALS: ' || l_records_prop );
	dbms_output.put_line( ' from LANE_PRODUCT_GROUPS: ' || l_records_lpg );
	dbms_output.put_line( ' from DELAYS_POSTCODE_DEST: ' || l_records_dpd );
	dbms_output.put_line( ' from DELAYS_POSTCODE_ORIG: ' || l_records_dpo );
	dbms_output.put_line( ' from DELAYS_WEIGHT_DEST: ' || l_records_dwd );
	dbms_output.put_line( ' from DELAYS_WEIGHT_ORIG: ' || l_records_dwo );
	dbms_output.put_line( ' from STATIONS: ' || l_records_sta );
	dbms_output.put_line( 'No commit done yet.  Please commit or rollback.' );
	
END;
GO

--* MESSAGE START 'Upgrading package body TTS.PCK_TTS_TARGET'
CREATE OR REPLACE PACKAGE BODY pck_tts_target IS

	s_process_name log_dhl.process_name%TYPE;
	
	C_STATUS_APPROVED CONSTANT status_definitions.code%TYPE := 'APPROVED';
	C_STATUS_EXPORTED CONSTANT status_definitions.code%TYPE := 'EXPORTED';
	C_STATUS_PROPOSED CONSTANT status_definitions.code%TYPE := 'PROPOSED';
	C_STATUS_REMOVED CONSTANT status_definitions.code%TYPE := 'REMOVED';
	C_COMMIT_INTERVAL CONSTANT PLS_INTEGER := 1000; 

	----------------------------------------------------------------------------------------------------
	-- This procedure loops through the proposals which are eligible for purging. The status of these records is updated to REMOVED.
	----------------------------------------------------------------------------------------------------

	PROCEDURE p_purge_proposals IS
	
		CURSOR c_proposal IS
			SELECT prop.prop_id
			FROM proposals prop, status_definitions stad
			WHERE prop.stad_id = stad.stad_id
			AND stad.code = C_STATUS_PROPOSED
			AND prop.last_change_date < ADD_MONTHS( SYSDATE, -2 );
	
		r_proposal c_proposal%ROWTYPE;
		l_package_name CONSTANT log_dhl.package_name%TYPE := 'pck_tts_target';
		l_procedure_name CONSTANT log_dhl.procedure_name%TYPE := 'p_purge_proposals';
		l_phase_code log_dhl.phase_code%TYPE := 0;
		l_records_processed log_dhl.records_processed%TYPE := 0;
		
	BEGIN
	
		l_phase_code := 10;
		OPEN c_proposal;

		l_phase_code := 20;
		LOOP
			FETCH c_proposal INTO r_proposal;
			IF c_proposal%NOTFOUND THEN EXIT; END IF;
			
			UPDATE proposals prop
			SET ( prop.stad_id, prop.last_change_date, prop.syst_comment ) = 
			( 
				SELECT stad.stad_id, SYSDATE, 'Proposal is outdated and removed by the system.'
				FROM status_definitions stad
				WHERE stad.code = C_STATUS_REMOVED
			)
			WHERE prop.prop_id = r_proposal.prop_id;
			
			l_records_processed := l_records_processed + SQL%ROWCOUNT;
	
			DELETE FROM proposal_approval_emails
			WHERE ROWID IN
			(
				SELECT e.ROWID
				FROM proposal_approvals a, proposal_approval_emails e
				WHERE a.prop_id = r_proposal.prop_id
				AND e.proa_id = a.proa_id
			);
		
			DELETE FROM proposal_approvals
			WHERE prop_id = r_proposal.prop_id;
	
			IF MOD( l_records_processed, C_COMMIT_INTERVAL ) = 0 THEN
				COMMIT;
			END IF;
			
		END LOOP;

		COMMIT;

		pck_dhl_log.p_log ( p_process_name => s_process_name, p_package_name => l_package_name, p_procedure_name => l_procedure_name,
			p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );
		
	EXCEPTION 
		WHEN others THEN
			pck_dhl_log.p_log ( p_process_name => s_process_name, p_package_name => l_package_name, p_procedure_name => l_procedure_name,
				p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
		RAISE; 
		
	END p_purge_proposals;

	----------------------------------------------------------------------------------------------------
	-- This procedure loops through the proposals which are eligible for exporting. The records are copied to the TARGET_EXPORT table and their status is updated to EXPORTED.
	----------------------------------------------------------------------------------------------------

	PROCEDURE p_export_targets ( p_process_name IN log_dhl.process_name%TYPE ) IS
	
		l_package_name CONSTANT log_dhl.package_name%TYPE := 'pck_tts_target';
		l_procedure_name CONSTANT log_dhl.procedure_name%TYPE := 'p_export_targets';
		l_phase_code log_dhl.phase_code%TYPE := 0;
		l_records_processed log_dhl.records_processed%TYPE := 0;
		l_current_prop_id PLS_INTEGER;
		l_stad_id_exported proposals.stad_id%TYPE;
		
	BEGIN
		s_process_name := p_process_name;
		
		p_purge_proposals;
		
		BEGIN
		
			l_phase_code := 10;
			SELECT stad_id INTO l_stad_id_exported FROM status_definitions WHERE code = C_STATUS_EXPORTED;

			l_phase_code := 20;
			FOR r_target IN
			(
				SELECT prop.prop_id,
					 stat_orig.iata_code orig_stn,
					 stat_dest.iata_code dest_stn,
					 prog.code prodgroup,
					 prop.last_change_date last_change_date,
					 prot.prot_dow dow,
					 prot.prot_target_days dhl_target,
					 prot.prot_am_pm_delivery am_pm_delivery,
					 prot.prot_pickup_cutoff pickup_cutoff
				FROM proposals prop,
					 status_definitions stad,
					 proposal_targets prot,
					 lane_product_groups lapg,
					 stations stat_orig,
					 stations stat_dest,
					 product_groups prog
				WHERE prop.last_change_date < TO_DATE( '01-' || TO_CHAR( SYSDATE, 'MM-YYYY' ), 'DD-MM-YYYY' )
				AND stad.stad_id = prop.stad_id
				AND prot.prop_id = prop.prop_id
				AND lapg.lapg_id = prop.lapg_id 
				AND stat_orig.stat_id = lapg.stat_id_origin 
				AND stat_dest.stat_id = lapg.stat_id_dest 
				AND prog.prog_id = lapg.prog_id 
				AND stad.code = C_STATUS_APPROVED
				ORDER BY prop_id, dow
			)
			LOOP
			
				IF l_current_prop_id IS NOT NULL AND r_target.prop_id <> l_current_prop_id THEN
				
					UPDATE proposals prop
					SET prop.stad_id = l_stad_id_exported, 
						prop.last_change_date = SYSDATE
					WHERE prop.prop_id = l_current_prop_id;
					
					l_records_processed := l_records_processed + 1;
					
					IF MOD( l_records_processed, C_COMMIT_INTERVAL ) = 0 THEN
						COMMIT;
					END IF;
					
				END IF;

				l_current_prop_id := r_target.prop_id;
				
				INSERT INTO target_export ( orig_stn, dest_stn, prodgroup, pu_day_of_week, dhl_target, pickup_cutoff, am_pm_delivery, last_change_date )
				VALUES ( r_target.orig_stn, r_target.dest_stn, r_target.prodgroup, r_target.dow, r_target.dhl_target, r_target.pickup_cutoff, r_target.am_pm_delivery, r_target.last_change_date );
				
			END LOOP;
			
			IF l_current_prop_id IS NOT NULL THEN
			
				UPDATE proposals prop
				SET prop.stad_id = l_stad_id_exported, 
					prop.last_change_date = SYSDATE
				WHERE prop.prop_id = l_current_prop_id;
				
				l_records_processed := l_records_processed + 1;
				
			END IF;

			COMMIT;
			
			pck_dhl_log.p_log ( p_process_name => s_process_name, p_package_name => l_package_name, p_procedure_name => l_procedure_name,
				p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );
				
		EXCEPTION 
			WHEN others THEN
				pck_dhl_log.p_log ( p_process_name => p_process_name, p_package_name => l_package_name, p_procedure_name => l_procedure_name,
					p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
				RAISE; 
		END;
			
	END p_export_targets;

END pck_tts_target;
GO

--* MESSAGE START 'Creating procedure TTS.PRC_LOAD_SHIPMENT_COUNTS'
CREATE OR REPLACE PROCEDURE PRC_LOAD_SHIPMENT_COUNTS IS

	-- Constants.
	C_TIMELINE_SIZE PLS_INTEGER := 1248; -- 1248 = 96 * ( 6 * 2 + 1 )
	C_NUMBER_OF_TIMEBANDS PLS_INTEGER := 96;
	C_NUMBER_OF_TIMEBANDS2 PLS_INTEGER := 192;
	C_TIMELINE_LAST_DAY PLS_INTEGER := C_TIMELINE_SIZE - C_NUMBER_OF_TIMEBANDS; -- 1152 = 96 * 6 * 2
	C_NUMBER_OF_DAYS PLS_INTEGER := 7;
	C_NUMBER_OF_DAYPARTS PLS_INTEGER := 13;

	C_SYSPROP_SHIPMENTS_PERC VARCHAR2(40) := 'sysPropShipPerc';
	C_SYSPROP_PERFORMANCE VARCHAR2(40) := 'sysPropBaseEtp';
	C_NUMBER_OF_PERIODS VARCHAR2(40) := 'numberOfPeriodsTotShipmVol';
	C_MAJOR_LANE_THRESHOLD VARCHAR2(40) := 'sysPropTotShipmVol';

	C_LANE_TYPE_MINOR VARCHAR2(40) := '-';
	C_LANE_TYPE_MAJOR VARCHAR2(40) := '+';

	-- HashMap that maintains the counts for a complete laneproductgroup before inserting them.
	TYPE TYPE_SHIPMENT_COUNTS IS TABLE OF SHIPMENT_COUNTS%ROWTYPE INDEX BY PLS_INTEGER;
	L_SHIPMENT_COUNTS TYPE_SHIPMENT_COUNTS;

	-- A timeline maintains the deliveries over the course of time.
	TYPE TYPE_TIMELINE IS VARRAY(1248) OF PLS_INTEGER;
	L_TIMELINE_DAY TYPE_TIMELINE; -- Timeline for a day in the week.
	L_TIMELINE_WEEK TYPE_TIMELINE; -- Timeline for the whole week.

	-- Cummulative totals per timeband.
	TYPE TYPE_TIMELINE_TOTALS IS VARRAY(96) OF PLS_INTEGER;
	L_TIMELINE_TOTALS TYPE_TIMELINE_TOTALS;

	-- The period to use from the SHIPMENTs.
	L_FIRST_MONTH PLS_INTEGER;
	L_LAST_MONTH PLS_INTEGER;

	L_CURRENT_LAPG_ID PLS_INTEGER; -- Current LAPG_ID. The cursor is sorted, so we can sense breaks from one laneproductgroup to the next.
	L_CURRENT_DOW PLS_INTEGER; -- Current day-of-week. The cursor is sorted, so we can sense breaks from one day to the next.
	L_CURRENT_TARGET_DAYS PLS_INTEGER; -- Target days of the current day-of-week.
	L_CURRENT_AM_PM_DELIVERY PLS_INTEGER; -- AM/PM delivery of the current day-of-week.
	L_CURRENT_PICKUP_CUTOFF PLS_INTEGER; -- Pickup cutoff of the current day-of-week.
	L_CURRENT_PRODGROUP TTS.PRODUCT_GROUPS.CODE%TYPE; -- Product group of the current laneproductgroup.

	L_WEEK_TARGET BOOLEAN; -- TRUE if all days of the week have the same target, am/pm and pickup cutoff.

	-- System proposal system settings
	L_SYSPROP_SHIPMENTS_PERC PLS_INTEGER;
	L_SYSPROP_PERFORMANCE PLS_INTEGER;
	L_NUMBER_OF_PERIODS PLS_INTEGER;
	L_MAJOR_LANE_THRESHOLD PLS_INTEGER;

	L_TRANSIT_DAYS PLS_INTEGER; -- Needed for calculating the transit days to store in SHIPMENT_COUNTS.
	L_DELIVERY_TIME PLS_INTEGER; -- Needed for calculating the delivery time to store in SHIPMENT_COUNTS.

	L_I PLS_INTEGER; -- A loop index.
	L_KEY PLS_INTEGER; -- Key into the HashMap.

	-- Counters
	L_COMMIT_COUNTER PLS_INTEGER := 0;

	-- Logging
	C_PROCEDURE_NAME CONSTANT LOG_DHL.PROCEDURE_NAME%TYPE := 'prc_load_shipment_counts';
	C_PROCESS_NAME CONSTANT LOG_DHL.PROCESS_NAME%TYPE := 'TTS';
	L_PHASE_CODE LOG_DHL.PHASE_CODE%TYPE := 0;
	L_PARAMETER_TEXT LOG_DHL.MESSAGE_TEXT%TYPE;
	L_PROCESSED_COUNT LOG_DHL.RECORDS_PROCESSED%TYPE;
	L_REJECTED_COUNT LOG_DHL.RECORDS_REJECTED%TYPE;



	-- Do commit.
	PROCEDURE DOCOMMIT IS
	BEGIN
		COMMIT;
		L_COMMIT_COUNTER := 0;
	END;



	-- Do commit in intervals of 100000.
	PROCEDURE DOCOMMIT100000 IS
	BEGIN
		L_COMMIT_COUNTER := L_COMMIT_COUNTER + 1;
		IF L_COMMIT_COUNTER >= 100000 THEN
			COMMIT;
			L_COMMIT_COUNTER := 0;
		END IF;
	END;



	-- Accumulate the timeline.
	PROCEDURE ACCUMULATE( P_TIMELINE IN OUT NOCOPY TYPE_TIMELINE ) IS
		L_I PLS_INTEGER;
	BEGIN
		FOR L_I IN 2..C_TIMELINE_SIZE LOOP
			P_TIMELINE( L_I ) := P_TIMELINE( L_I ) + P_TIMELINE( L_I - 1 );
		END LOOP;
	END;



	-- Accumulate the timeline totals.
	PROCEDURE ACCUMULATE_TOTALS( P_TIMELINE IN OUT NOCOPY TYPE_TIMELINE, P_TIMELINE_TOTALS IN OUT NOCOPY TYPE_TIMELINE_TOTALS ) IS
		L_I PLS_INTEGER;
		L_J PLS_INTEGER;
		L_K PLS_INTEGER;
		L_TOTAL PLS_INTEGER;
	BEGIN
		P_TIMELINE_TOTALS := TYPE_TIMELINE_TOTALS( 0 ); P_TIMELINE_TOTALS.EXTEND( C_NUMBER_OF_TIMEBANDS - 1, 1 );
		L_K := C_NUMBER_OF_DAYPARTS - 1;

		L_TOTAL := 0;
		FOR L_I IN 1..C_NUMBER_OF_TIMEBANDS LOOP
			FOR L_J IN 0..L_K LOOP
				L_TOTAL := L_TOTAL + P_TIMELINE( L_I + L_J * C_NUMBER_OF_TIMEBANDS );
			END LOOP;
			P_TIMELINE_TOTALS( L_I ) := L_TOTAL;
		END LOOP;
	END;



	-- Store the collected counts into SHIPMENT_COUNTS.
	PROCEDURE STORE_COUNTS IS
	BEGIN
		DELETE FROM TTS.SHIPMENT_COUNTS WHERE LAPG_ID = L_CURRENT_LAPG_ID;

		FORALL L_KEY IN INDICES OF L_SHIPMENT_COUNTS
		INSERT INTO TTS.SHIPMENT_COUNTS VALUES L_SHIPMENT_COUNTS( L_KEY );
	END;



	-- Stores the performance in the LANE_PRODUCT_GROUPS. Only if the target/pickup is the same for each day of the week.
	PROCEDURE STORE_WEEKLY_PERFORMANCE IS
		L_TRANSIT_DAYS PLS_INTEGER;
		L_TIME_ID PLS_INTEGER;
		L_I PLS_INTEGER;
		L_SHIPMENTS PLS_INTEGER;
		L_TOTAL PLS_INTEGER;
		L_PERFORMANCE LANE_PRODUCT_GROUPS.LAPG_PERFORMANCE%TYPE;
		L_LANE_TYPE TTS.LANE_PRODUCT_GROUPS.LAPG_TYPE%TYPE;
	BEGIN
		L_PERFORMANCE := NULL;
		L_LANE_TYPE := NULL;

		L_TOTAL := L_TIMELINE_WEEK( C_TIMELINE_SIZE );

		IF L_TOTAL > L_MAJOR_LANE_THRESHOLD THEN
			L_LANE_TYPE := C_LANE_TYPE_MAJOR;
		ELSE
			L_LANE_TYPE := C_LANE_TYPE_MINOR;
		END IF;

		IF L_WEEK_TARGET THEN

			L_TRANSIT_DAYS := L_CURRENT_TARGET_DAYS;
			IF L_TRANSIT_DAYS > C_NUMBER_OF_DAYS THEN L_TRANSIT_DAYS := C_NUMBER_OF_DAYS; END IF;
			-- Here we need to round up. For example, a pickup of 15:23 falls before the pickup cutoff of 15:30.
			L_TIME_ID := CEIL( MOD( L_CURRENT_PICKUP_CUTOFF, 100 ) / 15 ) + TRUNC( L_CURRENT_PICKUP_CUTOFF / 100 ) * 4;
			IF L_TIME_ID < 1 OR L_TIME_ID > 96 THEN L_TIME_ID := 96; END IF; -- = 24:00
			L_I := L_TIME_ID + ( L_TRANSIT_DAYS - 1 ) * C_NUMBER_OF_TIMEBANDS2;
			IF L_CURRENT_AM_PM_DELIVERY = 24 AND L_TRANSIT_DAYS < C_NUMBER_OF_DAYS THEN L_I := L_I + C_NUMBER_OF_TIMEBANDS; END IF;

			IF L_TOTAL > 0 THEN L_PERFORMANCE := 100.0 * L_TIMELINE_WEEK( L_I ) / L_TOTAL; END IF;

		END IF;

		UPDATE LANE_PRODUCT_GROUPS
		SET LAPG_PERFORMANCE = L_PERFORMANCE, -- Will be rounded
			LAPG_TYPE = L_LANE_TYPE,
			LAPG_MONTH_IMPORTED = L_LAST_MONTH
		WHERE LAPG_ID = L_CURRENT_LAPG_ID;

	END;



	-- Stores the performance in the LPG_TARGETS.
	PROCEDURE STORE_DAILY_PERFORMANCE IS
		L_TRANSIT_DAYS PLS_INTEGER;
		L_TIME_ID PLS_INTEGER;
		L_I PLS_INTEGER;
		L_SHIPMENTS PLS_INTEGER;
		L_TOTAL PLS_INTEGER;
		L_PERFORMANCE LANE_PRODUCT_GROUPS.LAPG_PERFORMANCE%TYPE;
	BEGIN
		L_PERFORMANCE := NULL;
		IF L_CURRENT_TARGET_DAYS IS NOT NULL THEN

			L_TRANSIT_DAYS := L_CURRENT_TARGET_DAYS;
			IF L_TRANSIT_DAYS > C_NUMBER_OF_DAYS THEN L_TRANSIT_DAYS := C_NUMBER_OF_DAYS; END IF;
			-- Here we need to round up. For example, a pickup of 15:23 falls before the pickup cutoff of 15:30.
			L_TIME_ID := CEIL( MOD( L_CURRENT_PICKUP_CUTOFF, 100 ) / 15 ) + TRUNC( L_CURRENT_PICKUP_CUTOFF / 100 ) * 4;
			IF L_TIME_ID < 1 OR L_TIME_ID > 96 THEN L_TIME_ID := 96; END IF; -- = 24:00
			L_I := L_TIME_ID + ( L_TRANSIT_DAYS - 1 ) * C_NUMBER_OF_TIMEBANDS2;
			IF L_CURRENT_AM_PM_DELIVERY = 24 AND L_TRANSIT_DAYS < C_NUMBER_OF_DAYS THEN L_I := L_I + C_NUMBER_OF_TIMEBANDS; END IF;

			L_TOTAL := L_TIMELINE_DAY( C_TIMELINE_SIZE );
			IF L_TOTAL > 0 THEN L_PERFORMANCE := 100.0 * L_TIMELINE_DAY( L_I ) / L_TOTAL; END IF;

		END IF;

		UPDATE LPG_TARGETS
		SET LPGT_PERFORMANCE = L_PERFORMANCE -- Will be rounded
		WHERE LAPG_ID = L_CURRENT_LAPG_ID
		AND LPGT_DOW = L_CURRENT_DOW;

	END;



	-- Calculate and store the system proposed values.
	PROCEDURE STORE_SYSTEMPROPOSED_VALUES IS
		L_I PLS_INTEGER;
		L_J PLS_INTEGER;
		L_TOTAL PLS_INTEGER;
		L_PICKUP_CUTOFF PLS_INTEGER;
		L_TARGET_DAYS PLS_INTEGER;
		L_AM_PM_DELIVERY PLS_INTEGER;
		L_K PLS_INTEGER;
	BEGIN
		L_K := C_NUMBER_OF_DAYPARTS - 1;
		L_PICKUP_CUTOFF := NULL;
		L_TARGET_DAYS := NULL;
		L_AM_PM_DELIVERY := NULL;

		L_TOTAL := L_TIMELINE_DAY( C_TIMELINE_SIZE );
		IF L_TOTAL > 0 THEN

			FOR L_I IN 1..C_NUMBER_OF_TIMEBANDS LOOP
				IF L_TIMELINE_TOTALS( L_I ) * 100 / L_TOTAL >= L_SYSPROP_SHIPMENTS_PERC THEN
					L_PICKUP_CUTOFF := L_I;
					EXIT;
				END IF;
			END LOOP;

			IF L_PICKUP_CUTOFF IS NOT NULL THEN

				FOR L_J IN 0..L_K LOOP
					IF L_TIMELINE_DAY( L_PICKUP_CUTOFF + L_J * C_NUMBER_OF_TIMEBANDS ) * 100 / L_TOTAL >= L_SYSPROP_PERFORMANCE THEN
						L_TARGET_DAYS := L_J + 1;
						EXIT;
					END IF;
				END LOOP;

				IF L_TARGET_DAYS IS NOT NULL THEN
					IF L_TARGET_DAYS < C_NUMBER_OF_DAYPARTS THEN
						L_AM_PM_DELIVERY := 12;
						IF MOD( L_TARGET_DAYS, 2 ) = 0 THEN
							L_AM_PM_DELIVERY := 24;
						END IF;
					ELSE
						L_AM_PM_DELIVERY := 24;
					END IF;
					L_TARGET_DAYS := L_TARGET_DAYS / 2; -- Does rounding so 1 becomes 1, 2 becomes 1, 3 becomes 2, ... 13 becomes 7
					L_PICKUP_CUTOFF := TRUNC( L_PICKUP_CUTOFF / 4 ) * 100 + MOD( L_PICKUP_CUTOFF, 4 ) * 15;
				ELSE
					L_PICKUP_CUTOFF := NULL; -- To be sure
				END IF;

			END IF;

		END IF;

		UPDATE LPG_TARGETS
		SET LPGT_SYSPROP_TARGET_DAYS = L_TARGET_DAYS
		,	LPGT_SYSPROP_AM_PM_DELIVERY = L_AM_PM_DELIVERY
		,	LPGT_SYSPROP_PICKUP_CUTOFF = L_PICKUP_CUTOFF
		WHERE LAPG_ID = L_CURRENT_LAPG_ID
		AND LPGT_DOW = L_CURRENT_DOW;

	END;



BEGIN
	L_PHASE_CODE := 10;

	-- Read the system settings.
	SELECT TO_NUMBER( VALUE ) INTO L_SYSPROP_SHIPMENTS_PERC FROM TTS.SYSTEM_SETTINGS WHERE NAME = C_SYSPROP_SHIPMENTS_PERC;
	SELECT TO_NUMBER( VALUE ) INTO L_SYSPROP_PERFORMANCE FROM TTS.SYSTEM_SETTINGS WHERE NAME = C_SYSPROP_PERFORMANCE;
	SELECT TO_NUMBER( VALUE ) INTO L_NUMBER_OF_PERIODS FROM TTS.SYSTEM_SETTINGS WHERE NAME = C_NUMBER_OF_PERIODS;
	SELECT TO_NUMBER( VALUE ) INTO L_MAJOR_LANE_THRESHOLD FROM TTS.SYSTEM_SETTINGS WHERE NAME = C_MAJOR_LANE_THRESHOLD;

	L_LAST_MONTH := TO_NUMBER( TO_CHAR( ADD_MONTHS( SYSDATE, -1 ), 'YYYYMM' ) );
	L_FIRST_MONTH := TO_NUMBER( TO_CHAR( ADD_MONTHS( SYSDATE, -L_NUMBER_OF_PERIODS ), 'YYYYMM' ) );

	L_PARAMETER_TEXT := 'SYSPROP_SHIPMENTS_PERC=' || L_SYSPROP_SHIPMENTS_PERC || ', ' || 'SYSPROP_PERFORMANCE=' || L_SYSPROP_PERFORMANCE || ', ' || 'NUMBER_OF_PERIODS=' || L_NUMBER_OF_PERIODS || ', ' || 'MAJOR_LANE_THRESHOLD=' || L_MAJOR_LANE_THRESHOLD || ', ' || 'FIRST_MONTH=' || L_FIRST_MONTH || ', ' || 'LAST_MONTH=' || L_LAST_MONTH;
	PCK_DHL_LOG.P_LOG( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.CONFIG_LEVEL_NORMAL, P_PARAMETER_TEXT => L_PARAMETER_TEXT );

	L_PROCESSED_COUNT := 0;
	L_REJECTED_COUNT := 0;

	---------- Select shipments joined to laneproductgroups and timebands
	L_PHASE_CODE := 20;

	-- TODO: If problems with "snapshot too old" persist, use the following (possible) solution:
	-- Use a temporary table.
	-- First fill this table with: LAPG_ID, LPGT_DOW, LPGT_TARGET_DAYS, LPGT_AM_PM_DELIVERY, LPGT_PICKUP_CUTOFF and last but not least: PU_MONTH/AWB (which has a combination of partitioning and index).
	-- use this temporary table to query SHIPMENT.
	-- This way tables being updated will never be part of the select.
	FOR R_SHIPMENTS IN
	(
		SELECT LT.LAPG_ID,
			LT.LPGT_DOW,
			LT.LPGT_TARGET_DAYS,
			LT.LPGT_AM_PM_DELIVERY,
			LT.LPGT_PICKUP_CUTOFF,
			T.TIME_ID,
			CASE WHEN VALID_DL_ZIP IN ( 'Y', 'C' ) AND VALID_DL_POSTCODE_RANGE = 1 THEN REG_TRANSIT_TIME_DAYS ELSE GLOB_TRANSIT_TIME_DAYS END TRANSIT_DAYS,
			CASE WHEN VALID_DL_ZIP IN ( 'Y', 'C' ) AND VALID_DL_POSTCODE_RANGE = 1 THEN SC_STAMP ELSE GLOBAL_SC_STAMP END SC_STAMP,
			S.WAIT_DAY,
			S.PU_CUTOFF_FLAG,
			S.TOTAL_DELAY,
			P.CODE PRODGROUP,
			S.PU_MONTH
		FROM TTS.SHIPMENT S
		JOIN STATIONS SO
			ON SO.IATA_CODE = S.ORG_STN
		JOIN STATIONS SD
			ON SD.IATA_CODE = S.DST_STN
		JOIN PRODUCT_GROUPS P
			ON P.CODE = S.PRODGROUP
		JOIN LANE_PRODUCT_GROUPS L
			ON L.STAT_ID_ORIGIN = SO.STAT_ID
			AND L.STAT_ID_DEST = SD.STAT_ID
			AND L.PROG_ID = P.PROG_ID
			AND ( L.LAPG_MONTH_IMPORTED < L_LAST_MONTH OR L.LAPG_MONTH_IMPORTED IS NULL )
		JOIN TTS.LPG_TARGETS LT
			ON LT.LAPG_ID = L.LAPG_ID
			AND TO_CHAR( S.CUST_STARTCLOCK, 'D' ) = LT.LPGT_DOW
		JOIN TTS.TIMEBANDS T
			ON ( S.HAS_PU IS NULL OR S.HAS_PU = 0 ) AND T.TIME_ID = 96
			OR S.HAS_PU <> 0 AND TO_CHAR( S.CUST_STARTCLOCK, 'HH24MI' ) BETWEEN T.LOWERBOUND AND ( T.UPPERBOUND - 1 )
		WHERE S.CUST_STARTCLOCK IS NOT NULL
		AND S.PU_MONTH BETWEEN L_FIRST_MONTH AND L_LAST_MONTH
		ORDER BY S.PRODGROUP /* HAS INDEX */, S.ORG_STN, S.DST_STN, TO_CHAR( S.CUST_STARTCLOCK, 'D' )
	)
	LOOP

		---------- Check for breaks in laneproductgroups or day-of-week

		IF L_CURRENT_LAPG_ID IS NULL OR R_SHIPMENTS.LAPG_ID <> L_CURRENT_LAPG_ID THEN

			-- Break in laneproductgroup

			-- Store everything.
			IF L_CURRENT_LAPG_ID IS NOT NULL THEN

				ACCUMULATE( L_TIMELINE_WEEK );
				ACCUMULATE_TOTALS( L_TIMELINE_DAY, L_TIMELINE_TOTALS );
				ACCUMULATE( L_TIMELINE_DAY );

				STORE_COUNTS;
				STORE_WEEKLY_PERFORMANCE;
				STORE_DAILY_PERFORMANCE;
				STORE_SYSTEMPROPOSED_VALUES;
				DOCOMMIT100000;

			END IF;

			-- Reset the counts, the weekly timeline and the daily timeline
			L_SHIPMENT_COUNTS.DELETE;
			L_TIMELINE_WEEK := TYPE_TIMELINE( 0 ); L_TIMELINE_WEEK.EXTEND( C_TIMELINE_SIZE - 1, 1 );
			L_TIMELINE_DAY := TYPE_TIMELINE( 0 ); L_TIMELINE_DAY.EXTEND( C_TIMELINE_SIZE - 1, 1 );

			-- Set the current break values.
			L_CURRENT_LAPG_ID := R_SHIPMENTS.LAPG_ID;
			L_CURRENT_DOW := R_SHIPMENTS.LPGT_DOW;
			L_CURRENT_TARGET_DAYS := R_SHIPMENTS.LPGT_TARGET_DAYS;
			L_CURRENT_AM_PM_DELIVERY := R_SHIPMENTS.LPGT_AM_PM_DELIVERY;
			L_CURRENT_PICKUP_CUTOFF := R_SHIPMENTS.LPGT_PICKUP_CUTOFF;
			L_CURRENT_PRODGROUP := R_SHIPMENTS.PRODGROUP;

			-- Start with all weekdays equal when target days has a value.
			L_WEEK_TARGET := L_CURRENT_TARGET_DAYS IS NOT NULL;

		END IF;

		IF R_SHIPMENTS.LPGT_DOW <> L_CURRENT_DOW THEN

			-- Break in day-of-week, there will be NO break when laneproductgroups breaks

			-- Store daily things.
			ACCUMULATE_TOTALS( L_TIMELINE_DAY, L_TIMELINE_TOTALS );
			ACCUMULATE( L_TIMELINE_DAY );

			STORE_DAILY_PERFORMANCE;
			STORE_SYSTEMPROPOSED_VALUES;

			-- Reset the daily timeline.
			L_TIMELINE_DAY := TYPE_TIMELINE( 0 ); L_TIMELINE_DAY.EXTEND( C_TIMELINE_SIZE - 1, 1 );

			-- Check if this new day has equal values.
			IF R_SHIPMENTS.LPGT_TARGET_DAYS IS NULL OR R_SHIPMENTS.LPGT_TARGET_DAYS <> L_CURRENT_TARGET_DAYS THEN L_WEEK_TARGET := FALSE; END IF;
			IF R_SHIPMENTS.LPGT_AM_PM_DELIVERY <> L_CURRENT_AM_PM_DELIVERY THEN L_WEEK_TARGET := FALSE; END IF;
			IF R_SHIPMENTS.LPGT_PICKUP_CUTOFF <> L_CURRENT_PICKUP_CUTOFF THEN L_WEEK_TARGET := FALSE; END IF;

			-- Set the current break values.
			L_CURRENT_DOW := R_SHIPMENTS.LPGT_DOW;
			L_CURRENT_TARGET_DAYS := R_SHIPMENTS.LPGT_TARGET_DAYS;
			L_CURRENT_AM_PM_DELIVERY := R_SHIPMENTS.LPGT_AM_PM_DELIVERY;
			L_CURRENT_PICKUP_CUTOFF := R_SHIPMENTS.LPGT_PICKUP_CUTOFF;

		END IF;

		---------- Take the shipment, calculate, and store in counts and timelines.

		L_TRANSIT_DAYS := R_SHIPMENTS.TRANSIT_DAYS;
		IF L_TRANSIT_DAYS IS NOT NULL AND L_TRANSIT_DAYS >= 0 AND L_TRANSIT_DAYS < 99 THEN -- 99 is special value in the RDW

			-- Calculate transit days
			IF R_SHIPMENTS.WAIT_DAY IS NOT NULL THEN L_TRANSIT_DAYS := L_TRANSIT_DAYS + R_SHIPMENTS.WAIT_DAY; END IF;
			IF R_SHIPMENTS.PU_CUTOFF_FLAG = 'N' THEN L_TRANSIT_DAYS := L_TRANSIT_DAYS - 1; END IF;
			IF R_SHIPMENTS.TOTAL_DELAY IS NOT NULL THEN L_TRANSIT_DAYS := L_TRANSIT_DAYS - R_SHIPMENTS.TOTAL_DELAY; END IF;
			IF L_TRANSIT_DAYS < 1 THEN L_TRANSIT_DAYS := 1; END IF;
			IF L_TRANSIT_DAYS > 99 THEN L_TRANSIT_DAYS := 99; END IF;

			L_DELIVERY_TIME := TO_CHAR( R_SHIPMENTS.SC_STAMP, 'HH24MI' );
			IF L_TRANSIT_DAYS < 100 AND L_DELIVERY_TIME IS NOT NULL THEN

				-- Calculate delivery time
				IF L_DELIVERY_TIME <= 1200 THEN L_DELIVERY_TIME := 12; ELSE L_DELIVERY_TIME := 24; END IF;

				-- Store in shipment counts
				L_KEY := L_TRANSIT_DAYS * 10000 + R_SHIPMENTS.LPGT_DOW * 1000 + R_SHIPMENTS.TIME_ID * 10 + L_DELIVERY_TIME / 12;
				IF NOT L_SHIPMENT_COUNTS.EXISTS( L_KEY ) THEN
					L_SHIPMENT_COUNTS( L_KEY ).LAPG_ID := R_SHIPMENTS.LAPG_ID;
					L_SHIPMENT_COUNTS( L_KEY ).PU_DAY_OF_WEEK := R_SHIPMENTS.LPGT_DOW;
					L_SHIPMENT_COUNTS( L_KEY ).TIME_ID := R_SHIPMENTS.TIME_ID;
					L_SHIPMENT_COUNTS( L_KEY ).ADJUSTED_TRANSIT_TIME := L_TRANSIT_DAYS;
					L_SHIPMENT_COUNTS( L_KEY ).AM_PM_DELIVERY := L_DELIVERY_TIME;
					L_SHIPMENT_COUNTS( L_KEY ).SHIPMENT_COUNT := 1;
				ELSE
					L_SHIPMENT_COUNTS( L_KEY ).SHIPMENT_COUNT := L_SHIPMENT_COUNTS( L_KEY ).SHIPMENT_COUNT + 1;
				END IF;

				-- Store in timelines
				IF L_TRANSIT_DAYS > C_NUMBER_OF_DAYS THEN L_TRANSIT_DAYS := C_NUMBER_OF_DAYS; END IF;
				L_I := R_SHIPMENTS.TIME_ID + ( L_TRANSIT_DAYS - 1 ) * C_NUMBER_OF_TIMEBANDS2;
				IF L_DELIVERY_TIME = 24 AND L_TRANSIT_DAYS < C_NUMBER_OF_DAYS THEN L_I := L_I + C_NUMBER_OF_TIMEBANDS; END IF;
				L_TIMELINE_DAY( L_I ) := L_TIMELINE_DAY( L_I ) + 1;
				L_TIMELINE_WEEK( L_I ) := L_TIMELINE_WEEK( L_I ) + 1;

			ELSE
				L_REJECTED_COUNT := L_REJECTED_COUNT + 1;
			END IF;

		ELSE
			L_REJECTED_COUNT := L_REJECTED_COUNT + 1;
		END IF;

		L_PROCESSED_COUNT := L_PROCESSED_COUNT + 1;

	END LOOP;

	-- Store everything.
	IF L_CURRENT_LAPG_ID IS NOT NULL THEN

		ACCUMULATE( L_TIMELINE_WEEK );
		ACCUMULATE_TOTALS( L_TIMELINE_DAY, L_TIMELINE_TOTALS );
		ACCUMULATE( L_TIMELINE_DAY );

		STORE_COUNTS;
		STORE_WEEKLY_PERFORMANCE;
		STORE_DAILY_PERFORMANCE;
		STORE_SYSTEMPROPOSED_VALUES;

	END IF;

	DOCOMMIT;

	PCK_DHL_LOG.P_LOG ( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.CONFIG_LEVEL_NORMAL, P_RECORDS_PROCESSED => L_PROCESSED_COUNT, P_RECORDS_REJECTED => L_REJECTED_COUNT );

	---------- Clear LPG_TARGETS that are left over.
	L_PHASE_CODE := 30;

	UPDATE
	(
		SELECT T.ROWID, T.LPGT_SYSPROP_TARGET_DAYS, T.LPGT_SYSPROP_AM_PM_DELIVERY, T.LPGT_SYSPROP_PICKUP_CUTOFF, T.LPGT_PERFORMANCE 
		FROM TTS.LANE_PRODUCT_GROUPS L, TTS.LPG_TARGETS T
		WHERE T.LAPG_ID = L.LAPG_ID
		AND ( L.LAPG_MONTH_IMPORTED < L_LAST_MONTH OR L.LAPG_MONTH_IMPORTED IS NULL )
		AND ( T.LPGT_SYSPROP_TARGET_DAYS IS NOT NULL OR T.LPGT_SYSPROP_AM_PM_DELIVERY IS NOT NULL OR T.LPGT_SYSPROP_PICKUP_CUTOFF IS NOT NULL OR T.LPGT_PERFORMANCE IS NOT NULL )
	)
	SET LPGT_SYSPROP_TARGET_DAYS = NULL, LPGT_SYSPROP_AM_PM_DELIVERY = NULL, LPGT_SYSPROP_PICKUP_CUTOFF = NULL, LPGT_PERFORMANCE = NULL;
	
	L_PROCESSED_COUNT := SQL%ROWCOUNT;

	DOCOMMIT;

	PCK_DHL_LOG.P_LOG ( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.CONFIG_LEVEL_NORMAL, P_RECORDS_PROCESSED => L_PROCESSED_COUNT );

	---------- Remove SHIPMENT_COUNTS that are left over.
	L_PHASE_CODE := 40;

	DELETE FROM
	(
		SELECT S.ROWID 
		FROM TTS.LANE_PRODUCT_GROUPS L, TTS.SHIPMENT_COUNTS S
		WHERE S.LAPG_ID = L.LAPG_ID
		AND ( L.LAPG_MONTH_IMPORTED < L_LAST_MONTH OR L.LAPG_MONTH_IMPORTED IS NULL )
	);

	L_PROCESSED_COUNT := SQL%ROWCOUNT;

	DOCOMMIT;

	PCK_DHL_LOG.P_LOG ( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.CONFIG_LEVEL_NORMAL, P_RECORDS_PROCESSED => L_PROCESSED_COUNT );

	---------- Clear LANE_PRODUCT_GROUPS that are left over.
	L_PHASE_CODE := 50;
	
	UPDATE TTS.LANE_PRODUCT_GROUPS L
	SET L.LAPG_TYPE = C_LANE_TYPE_MINOR, L.LAPG_MONTH_IMPORTED = L_LAST_MONTH, L.LAPG_PERFORMANCE = NULL
	WHERE ( L.LAPG_MONTH_IMPORTED < L_LAST_MONTH OR L.LAPG_MONTH_IMPORTED IS NULL );

	L_PROCESSED_COUNT := SQL%ROWCOUNT;

	DOCOMMIT;

	PCK_DHL_LOG.P_LOG ( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.CONFIG_LEVEL_NORMAL, P_RECORDS_PROCESSED => L_PROCESSED_COUNT );

EXCEPTION
	WHEN OTHERS THEN
		PCK_DHL_LOG.P_LOG ( P_PROCESS_NAME => C_PROCESS_NAME, P_PROCEDURE_NAME => C_PROCEDURE_NAME, P_PHASE_CODE => L_PHASE_CODE, P_LOG_LEVEL => PCK_DHL_LOG.LOG_LEVEL_ORACLE, P_MESSAGE_CODE => SQLCODE );
		RAISE;
END;
GO

--* MESSAGE START 'Creating procedure TTS.PRC_LOAD_TARGETS'
CREATE OR REPLACE PROCEDURE PRC_LOAD_TARGETS IS

	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_load_targets';
	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	
	l_records_processed log_dhl.records_processed%TYPE := 0;
	l_phase_code log_dhl.phase_code%TYPE := 0;

	TYPE TYPE_INTARRAY IS VARRAY(7) OF PLS_INTEGER;

	l_target PLS_INTEGER;
	l_am_pm PLS_INTEGER;
	l_pickup_cutoff PLS_INTEGER;
	
	l_targets TYPE_INTARRAY := TYPE_INTARRAY();
	l_am_pms TYPE_INTARRAY := TYPE_INTARRAY();
	l_pickup_cutoffs TYPE_INTARRAY := TYPE_INTARRAY();	
	
	l_current_lapg_id PLS_INTEGER := -1;
	l_i PLS_INTEGER;
	
	PROCEDURE DO_COMMIT	IS
	BEGIN
		IF MOD( l_records_processed, 1000 ) = 0 THEN 
			COMMIT;
		END IF;
	END;
	
	PROCEDURE WRITE_LPG_TARGETS IS
	BEGIN
		IF l_current_lapg_id >= 0 THEN
		
			FOR l_i IN 1..7 LOOP
				UPDATE tts.lpg_targets
				SET	lpgt_target_days = l_targets( l_i )
				,	lpgt_am_pm_delivery = l_am_pms( l_i )
				,	lpgt_pickup_cutoff = l_pickup_cutoffs( l_i )
				WHERE lapg_id = l_current_lapg_id
				AND lpgt_dow = l_i;
			END LOOP;
			
			l_records_processed := l_records_processed + 1;
			DO_COMMIT; -- Commit per 1000 lane/productgroups
			
		END IF;
	END;

BEGIN
	l_phase_code := 10;

	FOR r_target IN
	(
		SELECT lapg.lapg_id
		,	targ.dhl_target
		,	targ.pickup_cutoff
		,	targ.dhl_time
		,	targ.pu_day_of_week
		FROM tts.lane_product_groups lapg
		JOIN tts.stations stat_orig ON lapg.stat_id_origin = stat_orig.stat_id
		JOIN tts.stations stat_dest ON lapg.stat_id_dest = stat_dest.stat_id
		JOIN tts.product_groups prog ON lapg.prog_id = prog.prog_id
		LEFT OUTER JOIN tts.target targ
			ON prog.code = targ.prodgroup
			AND stat_orig.iata_code = targ.orig_stn
			AND stat_dest.iata_code = targ.dest_stn
		ORDER BY lapg.lapg_id, targ.prodgroup, targ.pu_day_of_week desc
	)
	LOOP
		
		IF l_current_lapg_id <> r_target.lapg_id THEN
		
			WRITE_LPG_TARGETS;
			
			-- reset values
			l_targets := new TYPE_INTARRAY( NULL, NULL, NULL, NULL, NULL, NULL, NULL );
			l_am_pms := new TYPE_INTARRAY( NULL, NULL, NULL, NULL, NULL, NULL, NULL );
			l_pickup_cutoffs := new TYPE_INTARRAY( NULL, NULL, NULL, NULL, NULL, NULL, NULL );
			
			l_current_lapg_id := r_target.lapg_id;
			
		END IF;
		
		-- read values
		l_target := r_target.dhl_target;
		l_am_pm := r_target.dhl_time;
		l_pickup_cutoff := r_target.pickup_cutoff;
		
		-- fix values before writing into TTS database
		IF l_target IS NOT NULL THEN
			IF l_pickup_cutoff IS NULL OR l_pickup_cutoff < 1 OR l_pickup_cutoff > 2400 THEN l_pickup_cutoff := 2400; END IF;
			IF l_am_pm IS NULL OR l_am_pm >= 1200 THEN -- Yes: >= 1200
				l_am_pm := 24;
			ELSE
				l_am_pm := 12;
			END IF;
			IF MOD( l_pickup_cutoff, 100 ) > 59 THEN
				l_pickup_cutoff := TRUNC( l_pickup_cutoff, -2 ) + 59;
			END IF;
		ELSE
			l_pickup_cutoff := NULL;
			l_am_pm := NULL;
		END IF;
		
		-- store values in varray
		IF r_target.pu_day_of_week IS NULL THEN
			FOR l_i in 1..7 LOOP
				l_targets( l_i ) := l_target;
				l_pickup_cutoffs( l_i ) := l_pickup_cutoff;
				l_am_pms( l_i ) := l_am_pm;
			END LOOP;
		ELSE
			l_targets( r_target.pu_day_of_week ) := r_target.dhl_target;
			l_pickup_cutoffs( r_target.pu_day_of_week ) := r_target.pickup_cutoff;
			l_am_pms( r_target.pu_day_of_week ) := l_am_pm;
		END IF;
		
	END LOOP;
	
	WRITE_LPG_TARGETS;
	
	l_phase_code := 20;
	COMMIT;
	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );

EXCEPTION 
	WHEN others THEN
		pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
	RAISE;
END;
GO

--* MESSAGE START 'Creating procedure TTS.PRC_LOAD_GEOGROUPS'
CREATE OR REPLACE PROCEDURE PRC_LOAD_GEOGROUPS IS

	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_load_geogroups';
	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';

	l_records_processed log_dhl.records_processed%TYPE := 0;
	l_phase_code log_dhl.phase_code%TYPE := 0;

BEGIN

	l_phase_code := 0;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_phase_code => l_phase_code );

	l_phase_code := 10;

	l_records_processed := 0;

	MERGE INTO regions
		USING ( SELECT DISTINCT TRIM( super_region_code ) super_region_code, super_region_name FROM geogroup ) source
		ON ( source.super_region_code = regions.region_code )
	WHEN MATCHED THEN UPDATE
		SET regions.region_name = source.super_region_name
	WHEN NOT MATCHED THEN
		INSERT ( regi_id, region_code, region_name )
		VALUES ( seq_regi.NEXTVAL, source.super_region_code, source.super_region_name );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	MERGE INTO countries
		USING
		(
			SELECT sub.ctry_code, sub.ctry_name, regions.regi_id
			FROM
			(
				SELECT ctry_code, ctry_name, TRIM( MIN( super_region_code ) ) region_code
				FROM geogroup
				GROUP BY ctry_code, ctry_name
			) sub
			JOIN regions ON regions.region_code = sub.region_code
		) source
		ON ( source.ctry_code = countries.ctry_code )
	WHEN MATCHED THEN UPDATE
		SET countries.ctry_name = source.ctry_name, countries.regi_id = source.regi_id
	WHEN NOT MATCHED THEN
		INSERT ( coun_id, ctry_code, ctry_name, regi_id )
		VALUES ( seq_coun.NEXTVAL, source.ctry_code, source.ctry_name, source.regi_id );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	MERGE INTO stations
		USING
		(
			SELECT iata_code, iata_desc, countries.coun_id
			FROM geogroup
			JOIN countries ON countries.ctry_code = geogroup.ctry_code
		) source
		ON ( source.iata_code = stations.iata_code )
	WHEN MATCHED THEN UPDATE
		SET stations.iata_desc = source.iata_desc,
			stations.coun_id = source.coun_id
	WHEN NOT MATCHED THEN
		INSERT ( stat_id, iata_code, iata_desc, coun_id )
		VALUES ( seq_stat.NEXTVAL, source.iata_code, source.iata_desc, source.coun_id );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	COMMIT; -- <<<<

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL,
		p_phase_code => l_phase_code, p_records_processed => l_records_processed );

	l_phase_code := 20;

	l_records_processed := 0;

	FOR r_stations IN ( SELECT stat_id FROM stations ) LOOP

		FOR r_combinations IN
		(
			SELECT stat_orig.stat_id stat_id_orig, stat_dest.stat_id stat_id_dest, prog.prog_id
			FROM stations stat_orig, stations stat_dest,
				countries coun_orig, countries coun_dest,
				regions regi_orig, product_groups prog
			WHERE stat_orig.stat_id = r_stations.stat_id
			AND coun_orig.coun_id = stat_orig.coun_id
			AND coun_dest.coun_id = stat_dest.coun_id
			AND regi_orig.regi_id = coun_orig.regi_id
			AND stat_dest.stat_id <> r_stations.stat_id
			AND
			(
				stat_orig.coun_id = stat_dest.coun_id AND prog.code = 'DOM'
				OR
				NOT( stat_orig.coun_id = stat_dest.coun_id ) AND
				(
					regi_orig.region_code = 'WE' AND coun_orig.regi_id = coun_dest.regi_id AND prog.code = 'ECX'
					OR
					NOT( regi_orig.region_code = 'WE' AND coun_orig.regi_id = coun_dest.regi_id ) AND prog.code IN ( 'DOX', 'WPX', 'DDF' )
				)
			)
			AND NOT EXISTS
			(
				SELECT lapg_id
				FROM lane_product_groups lapg
				WHERE stat_id_origin = r_stations.stat_id
				AND stat_id_dest = stat_dest.stat_id
				AND prog_id = prog.prog_id
			)
		)
		LOOP

			INSERT INTO lane_product_groups ( lapg_id, stat_id_origin, stat_id_dest, prog_id )
			VALUES ( seq_lapg.nextval, r_combinations.stat_id_orig, r_combinations.stat_id_dest, r_combinations.prog_id );

			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 1 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 2 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 3 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 4 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 5 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 6 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 7 );

			l_records_processed := l_records_processed + 1;

		END LOOP;

		COMMIT; -- <<<<

	END LOOP;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL,
		p_phase_code => l_phase_code, p_records_processed => l_records_processed );

	COMMIT;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );

EXCEPTION
	WHEN others THEN
		pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
	RAISE;
END;
GO

--* MESSAGE START 'Creating procedure TTS.PRC_LOAD_REF_DATA'
CREATE OR REPLACE PROCEDURE PRC_LOAD_REF_DATA IS

	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_load_ref_data';
	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	
	l_records_processed log_dhl.records_processed%TYPE := 0;
	l_phase_code log_dhl.phase_code%TYPE := 0;

	-- Replace the commodites
	PROCEDURE LOAD_COMMODITIES IS
	BEGIN
		DELETE FROM TTS.DELAYS_COMMODITY_DEST;
		DELETE FROM TTS.COMMODITIES;
	
		INSERT INTO TTS.COMMODITIES ( COMM_CODE, COMM_DESCRIPTION )
		SELECT COMM_CODE, COMM_DESCRIPTION FROM REF.COMMODITIES WHERE COMM_DESCRIPTION IS NOT NULL;
		
		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
	-- Load the delays for the commodities
	PROCEDURE LOAD_DELAYS_COMMODITY_DEST IS
	BEGIN
		INSERT INTO TTS.DELAYS_COMMODITY_DEST ( COUN_ID, COMM_CODE, DECD_DELAY )
		SELECT COUN.COUN_ID, R_DELC.COMM_CODE, MIN(R_DELC.DELAY)
		FROM REF.DELAYS_COMMODITY_DEST R_DELC
		JOIN TTS.COUNTRIES COUN ON R_DELC.COUNTRY = COUN.CTRY_CODE
		WHERE R_DELC.DELAY IS NOT NULL
		AND R_DELC.DELAY > 0
		GROUP BY COUN.COUN_ID, R_DELC.COMM_CODE;
		
		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
	-- Replace the delays for weight ranges per origin station
	PROCEDURE LOAD_DELAYS_WEIGHT_ORIG IS
	BEGIN
		DELETE FROM TTS.DELAYS_WEIGHT_ORIG;
	
		INSERT INTO TTS.DELAYS_WEIGHT_ORIG ( STAT_ID, DEWO_START, DEWO_END, DEWO_DELAY )
		SELECT STAT.STAT_ID, R_DEWO.LOW, MIN(R_DEWO.HIGH), MIN(R_DEWO.DELAY)
		FROM REF.DELAYS_WEIGHT_ORIG R_DEWO
		JOIN TTS.STATIONS STAT ON R_DEWO.STATION = STAT.IATA_CODE
		WHERE R_DEWO.DELAY IS NOT NULL
		AND R_DEWO.DELAY > 0
		GROUP BY STAT.STAT_ID, R_DEWO.LOW;
		
		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
	-- Replace the delays for weight ranges per destination station
	PROCEDURE LOAD_DELAYS_WEIGHT_DEST IS
	BEGIN
		DELETE FROM TTS.DELAYS_WEIGHT_DEST;
	
		INSERT INTO TTS.DELAYS_WEIGHT_DEST ( STAT_ID, DEWD_START, DEWD_END, DEWD_DELAY )
		SELECT STAT.STAT_ID, R_DEWD.LOW, MIN(R_DEWD.HIGH), MIN(R_DEWD.DELAY)
		FROM REF.DELAYS_WEIGHT_DEST R_DEWD
		JOIN TTS.STATIONS STAT ON R_DEWD.STATION = STAT.IATA_CODE
		WHERE R_DEWD.DELAY IS NOT NULL
		AND R_DEWD.DELAY > 0
		GROUP BY STAT.STAT_ID, R_DEWD.LOW;
		
		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
	-- Replace the delays for value ranges per destination country
	PROCEDURE LOAD_DELAYS_VALUE_DEST IS
	BEGIN
		DELETE FROM TTS.DELAYS_VALUE_DEST;
	
		INSERT INTO TTS.DELAYS_VALUE_DEST ( COUN_ID, DEVD_START, DEVD_END, DEVD_DELAY )
		SELECT COUN.COUN_ID, R_DEVD.LOW, MIN(R_DEVD.HIGH), MIN(R_DEVD.DELAY)
		FROM REF.DELAYS_VALUE_DEST R_DEVD
		JOIN TTS.COUNTRIES COUN ON R_DEVD.COUNTRY = COUN.CTRY_CODE
		WHERE R_DEVD.DELAY IS NOT NULL
		AND R_DEVD.DELAY > 0
		GROUP BY COUN.COUN_ID, R_DEVD.LOW;
		
		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
	-- Replace the delays for postcode ranges per origin station
	PROCEDURE LOAD_DELAYS_POSTCODE_ORIG IS
	BEGIN
		DELETE FROM TTS.DELAYS_POSTCODE_ORIG;
	
		INSERT INTO TTS.DELAYS_POSTCODE_ORIG ( STAT_ID, DEPO_POSTCODE_START, DEPO_POSTCODE_END, DEPO_DELAY )
		SELECT STAT.STAT_ID, R_DEPO.POSTCODE_START, MIN(R_DEPO.POSTCODE_END), MIN(R_DEPO.ADDITIONAL_DAYS)
		FROM REF.ONFORWARDING_PICKUP R_DEPO
		JOIN TTS.STATIONS STAT ON R_DEPO.STN = STAT.IATA_CODE
		WHERE R_DEPO.ADDITIONAL_DAYS IS NOT NULL
		AND R_DEPO.ADDITIONAL_DAYS > 0
		GROUP BY STAT.STAT_ID, R_DEPO.POSTCODE_START;

		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;	

	-- Replace the delays for postcode ranges per destination station
	PROCEDURE LOAD_DELAYS_POSTCODE_DEST IS
	BEGIN
		DELETE FROM TTS.DELAYS_POSTCODE_DEST;
	
		INSERT INTO TTS.DELAYS_POSTCODE_DEST ( STAT_ID, DEPD_POSTCODE_START, DEPD_POSTCODE_END, DEPD_DELAY )
		SELECT STAT.STAT_ID, R_DEPD.POSTCODE_START, MIN(R_DEPD.POSTCODE_END), MIN(R_DEPD.ADDITIONAL_DAYS)
		FROM REF.ONFORWARDING_DELIVERY R_DEPD
		JOIN TTS.STATIONS STAT ON R_DEPD.STN = STAT.IATA_CODE
		WHERE R_DEPD.ADDITIONAL_DAYS IS NOT NULL
		AND R_DEPD.ADDITIONAL_DAYS > 0
		GROUP BY STAT.STAT_ID, R_DEPD.POSTCODE_START;

		l_records_processed := l_records_processed + SQL%ROWCOUNT;
	END;
	
BEGIN

	l_phase_code := 10;
	LOAD_COMMODITIES;
	
	l_phase_code := 20;
	LOAD_DELAYS_COMMODITY_DEST;
	
	l_phase_code := 30;
	LOAD_DELAYS_WEIGHT_ORIG;
	
	l_phase_code := 40;
	LOAD_DELAYS_WEIGHT_DEST;
	
	l_phase_code := 50;
	LOAD_DELAYS_VALUE_DEST;
	
	l_phase_code := 60;
	LOAD_DELAYS_POSTCODE_ORIG;
	
	l_phase_code := 70;
	LOAD_DELAYS_POSTCODE_DEST;

	COMMIT;
	
	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );

EXCEPTION 
	WHEN others THEN
		pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
	RAISE;
END;
GO

--* /PATCH







--* PATCH "3.0.2" --> "3.0.3"

--* MESSAGE START 'Updating LANE_PRODUCT_GROUP.LAPG_TYPE'
ALTER TABLE lane_product_groups DROP CONSTRAINT lapg_type_ck
GO
UPDATE lane_product_groups SET lapg_type = DECODE( lapg_type, 'MAJOR', '+', 'MINOR', '-', null )
GO
ALTER TABLE lane_product_groups MODIFY lapg_type CHAR(1)
GO
ALTER TABLE lane_product_groups ADD CONSTRAINT lapg_type_ck CHECK ( lapg_type IN ( '+', '-' ) )
GO

--* MESSAGE START 'Transferring data from PROPOSALS to PROPOSAL_TARGETS'
DECLARE
	ID_MIN TTS.PROPOSALS.PROP_ID%TYPE;
	ID_MAX TTS.PROPOSALS.PROP_ID%TYPE;
BEGIN
	SELECT MIN( PROP_ID ), MAX( PROP_ID )
	INTO ID_MIN, ID_MAX
	FROM TTS.PROPOSALS;
	
	WHILE ID_MIN <= ID_MAX LOOP

		INSERT INTO TTS.PROPOSAL_TARGETS ( PROP_ID, PROT_DOW, PROT_TARGET_DAYS, PROT_AM_PM_DELIVERY, PROT_PICKUP_CUTOFF )
		SELECT PROP.PROP_ID, DOW.DOW, PROP.PROP_DHL_TARGET, 24, TO_NUMBER( TO_CHAR( PROP.PROP_PICKUP_CUTOFF, 'HH24MI' ) )
		FROM TTS.PROPOSALS PROP,
			 ( SELECT 1 DOW FROM DUAL UNION ALL SELECT 2 FROM DUAL UNION ALL SELECT 3 FROM DUAL UNION ALL SELECT 4 FROM DUAL UNION ALL SELECT 5 FROM DUAL UNION ALL SELECT 6 FROM DUAL UNION ALL SELECT 7 FROM DUAL ) DOW
		WHERE PROP_ID BETWEEN ID_MIN AND ID_MIN + 9999
		ORDER BY PROP_ID, DOW.DOW;

		COMMIT;

		ID_MIN := ID_MIN + 10000;

	END LOOP;

END;
GO

--* MESSAGE START 'Transferring data from LANE_PRODUCT_GROUPS to LPG_TARGETS'
DECLARE
	ID_MIN TTS.LANE_PRODUCT_GROUPS.LAPG_ID%TYPE;
	ID_MAX TTS.LANE_PRODUCT_GROUPS.LAPG_ID%TYPE;
BEGIN
	SELECT MIN( LAPG_ID ), MAX( LAPG_ID )
	INTO ID_MIN, ID_MAX
	FROM TTS.LANE_PRODUCT_GROUPS;
	
	WHILE ID_MIN <= ID_MAX LOOP

		INSERT INTO TTS.LPG_TARGETS ( LAPG_ID, LPGT_DOW, LPGT_TARGET_DAYS, LPGT_AM_PM_DELIVERY, LPGT_PICKUP_CUTOFF, LPGT_SYSPROP_TARGET_DAYS, LPGT_SYSPROP_AM_PM_DELIVERY, LPGT_SYSPROP_PICKUP_CUTOFF )
		SELECT LAPG.LAPG_ID, DOW.DOW, LAPG.DHL_TARGET, NVL2( LAPG.DHL_TARGET, 24, NULL ), TO_NUMBER( TO_CHAR( LAPG.PICKUP_CUTOFF, 'HH24MI' ) ), LAPG.SYST_PROPOSED_TARGET, NVL2( LAPG.SYST_PROPOSED_TARGET, 24, NULL ), TO_NUMBER( TO_CHAR( LAPG.SYST_PROPOSED_PICKUP_CUTOFF, 'HH24MI' ) )
		FROM TTS.LANE_PRODUCT_GROUPS LAPG,
			 ( SELECT 1 DOW FROM DUAL UNION ALL SELECT 2 FROM DUAL UNION ALL SELECT 3 FROM DUAL UNION ALL SELECT 4 FROM DUAL UNION ALL SELECT 5 FROM DUAL UNION ALL SELECT 6 FROM DUAL UNION ALL SELECT 7 FROM DUAL ) DOW
		WHERE LAPG_ID BETWEEN ID_MIN AND ID_MIN + 9999
		ORDER BY LAPG_ID, DOW.DOW;

		COMMIT;

		ID_MIN := ID_MIN + 10000;

	END LOOP;

END;
GO

--* MESSAGE START 'Dropping columns from LANE_PRODUCT_GROUPS'
ALTER TABLE TTS.LANE_PRODUCT_GROUPS
DROP ( DHL_TARGET, PICKUP_CUTOFF, SYST_PROPOSED_TARGET, SYST_PROPOSED_PICKUP_CUTOFF )
GO

--* MESSAGE START 'Dropping columns from PROPOSALS'
ALTER TABLE TTS.PROPOSALS
DROP ( PROP_DHL_TARGET, PROP_PICKUP_CUTOFF, OLD_DHL_TARGET, OLD_PICKUP_CUTOFF )
GO

--* MESSAGE START 'Cleaning up PROPOSAL_TARGETS.PROT_PICKUP_CUTOFF'
UPDATE PROPOSAL_TARGETS
SET PROT_PICKUP_CUTOFF = 2400
WHERE ( PROT_PICKUP_CUTOFF < 15 OR PROT_PICKUP_CUTOFF >= 2359 )
GO

UPDATE PROPOSAL_TARGETS
SET PROT_PICKUP_CUTOFF = TRUNC( PROT_PICKUP_CUTOFF, -2 ) + CASE WHEN MOD( PROT_PICKUP_CUTOFF, 100 ) > 45 THEN 45 ELSE MOD( PROT_PICKUP_CUTOFF, 100 ) - MOD( MOD( PROT_PICKUP_CUTOFF, 100 ), 15 ) END
WHERE MOD( PROT_PICKUP_CUTOFF, 100 ) NOT IN ( 0, 15, 30, 45 )
GO

--* MESSAGE START 'Adding constraint on PROPOSAL_TARGETS.PROT_PICKUP_CUTOFF'
ALTER TABLE TTS.PROPOSAL_TARGETS ADD
CONSTRAINT PROT_CHECK_PICKUP_CUTOFF CHECK ( ( PROT_PICKUP_CUTOFF BETWEEN 1 AND 2400 ) AND ( MOD( PROT_PICKUP_CUTOFF, 100 ) IN ( 0, 15, 30, 45 ) ) )
GO

--* MESSAGE START 'Cleaning up LPG_TARGETS.LPGT_PICKUP_CUTOFF and LPG_TARGETS.LPGT_SYSPROP_PICKUP_CUTOFF'
UPDATE LPG_TARGETS
SET LPGT_PICKUP_CUTOFF = 2400
WHERE ( LPGT_PICKUP_CUTOFF < 1 OR LPGT_PICKUP_CUTOFF >= 2359 )
GO

UPDATE LPG_TARGETS
SET LPGT_PICKUP_CUTOFF = TRUNC( LPGT_PICKUP_CUTOFF, -2 ) + 59
WHERE MOD( LPGT_PICKUP_CUTOFF, 100 ) > 59
GO

--* // Current targets (from the RDW) are not checked for quarters of the hour !!!!! And no constraint is added for this too !!!!!

UPDATE LPG_TARGETS
SET LPGT_SYSPROP_PICKUP_CUTOFF = 2400
WHERE ( LPGT_SYSPROP_PICKUP_CUTOFF < 15 OR LPGT_SYSPROP_PICKUP_CUTOFF >= 2359 )
GO

UPDATE LPG_TARGETS
SET LPGT_SYSPROP_PICKUP_CUTOFF = TRUNC( LPGT_SYSPROP_PICKUP_CUTOFF, -2 ) + CASE WHEN MOD( LPGT_SYSPROP_PICKUP_CUTOFF, 100 ) > 45 THEN 45 ELSE MOD( LPGT_SYSPROP_PICKUP_CUTOFF, 100 ) - MOD( MOD( LPGT_SYSPROP_PICKUP_CUTOFF, 100 ), 15 ) END
WHERE MOD( LPGT_SYSPROP_PICKUP_CUTOFF, 100 ) NOT IN ( 0, 15, 30, 45 )
GO

--* MESSAGE START 'Adding constraint on PROPOSAL_TARGETS.PROT_PICKUP_CUTOFF and PROPOSAL_TARGETS.PROT_SYSPROP_PICKUP_CUTOFF'
ALTER TABLE TTS.LPG_TARGETS ADD 
CONSTRAINT LPGT_CHECK_PICKUP_CUTOFF CHECK ( ( LPGT_PICKUP_CUTOFF BETWEEN 1 AND 2400 ) 
	AND ( MOD( LPGT_PICKUP_CUTOFF, 100 ) BETWEEN 0 AND 59 ) 
	AND ( LPGT_SYSPROP_PICKUP_CUTOFF BETWEEN 1 AND 2400 ) 
	AND ( MOD( LPGT_SYSPROP_PICKUP_CUTOFF, 100 ) IN ( 0, 15, 30, 45 ) ) )
GO

--* MESSAGE START 'Cleaning up approvals for purged proposals'
DELETE FROM PROPOSAL_APPROVAL_EMAILS
WHERE ROWID IN
(
	SELECT E.ROWID
	FROM PROPOSALS P, STATUS_DEFINITIONS S, PROPOSAL_APPROVALS A, PROPOSAL_APPROVAL_EMAILS E
	WHERE S.STAD_ID = P.STAD_ID
	AND S.CODE = 'REMOVED'
	AND A.PROP_ID = P.PROP_ID
	AND E.PROA_ID = A.PROA_ID
)
GO

DELETE FROM PROPOSAL_APPROVALS
WHERE ROWID IN
(
	SELECT A.ROWID
	FROM PROPOSALS P, STATUS_DEFINITIONS S, PROPOSAL_APPROVALS A
	WHERE S.STAD_ID = P.STAD_ID
	AND S.CODE = 'REMOVED'
	AND A.PROP_ID = P.PROP_ID
)
GO

--* /PATCH







--* PATCH "3.0.3" --> "3.0.4"

--* MESSAGE START 'Adding column STAT_ECX to TTS.STATIONS'
ALTER TABLE TTS.STATIONS ADD ( STAT_ECX CHAR(1) )
GO

UPDATE TTS.STATIONS SET STAT_ECX = 'N'
GO

ALTER TABLE TTS.STATIONS MODIFY ( STAT_ECX NOT NULL )
GO

ALTER TABLE TTS.STATIONS ADD ( CONSTRAINT STAT_CHECK_ECX CHECK ( STAT_ECX IN ( 'Y', 'N' ) ) )
GO

--* MESSAGE START 'Upgrading procedure TTS.PRC_LOAD_GEOGROUPS'
CREATE OR REPLACE PROCEDURE PRC_LOAD_GEOGROUPS IS

	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_load_geogroups';
	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';

	l_records_processed log_dhl.records_processed%TYPE := 0;
	l_phase_code log_dhl.phase_code%TYPE := 0;

BEGIN

	l_phase_code := 0;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_phase_code => l_phase_code );

	l_phase_code := 10;

	l_records_processed := 0;

	MERGE INTO regions
		USING ( SELECT DISTINCT TRIM( super_region_code ) super_region_code, super_region_name FROM geogroup ) source
		ON ( source.super_region_code = regions.region_code )
	WHEN MATCHED THEN UPDATE
		SET regions.region_name = source.super_region_name
	WHEN NOT MATCHED THEN
		INSERT ( regi_id, region_code, region_name )
		VALUES ( seq_regi.NEXTVAL, source.super_region_code, source.super_region_name );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	MERGE INTO countries
		USING
		(
			SELECT sub.ctry_code, sub.ctry_name, regions.regi_id
			FROM
			(
				SELECT ctry_code, ctry_name, TRIM( MIN( super_region_code ) ) region_code
				FROM geogroup
				GROUP BY ctry_code, ctry_name
			) sub
			JOIN regions ON regions.region_code = sub.region_code
		) source
		ON ( source.ctry_code = countries.ctry_code )
	WHEN MATCHED THEN UPDATE
		SET countries.ctry_name = source.ctry_name, countries.regi_id = source.regi_id
	WHEN NOT MATCHED THEN
		INSERT ( coun_id, ctry_code, ctry_name, regi_id )
		VALUES ( seq_coun.NEXTVAL, source.ctry_code, source.ctry_name, source.regi_id );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	MERGE INTO stations
		USING
		(
			SELECT iata_code, iata_desc, countries.coun_id, DECODE(we_ecx, 'Y', 'Y', 'N') ecx
			FROM geogroup
			JOIN countries ON countries.ctry_code = geogroup.ctry_code
		) source
		ON ( source.iata_code = stations.iata_code )
	WHEN MATCHED THEN UPDATE
		SET stations.iata_desc = source.iata_desc,
			stations.coun_id = source.coun_id,
			stations.stat_ecx = source.ecx
	WHEN NOT MATCHED THEN
		INSERT ( stat_id, iata_code, iata_desc, coun_id, stat_ecx )
		VALUES ( seq_stat.NEXTVAL, source.iata_code, source.iata_desc, source.coun_id, source.ecx );

	l_records_processed := l_records_processed + SQL%ROWCOUNT;

	COMMIT; -- <<<<

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL,
		p_phase_code => l_phase_code, p_records_processed => l_records_processed );

	l_phase_code := 20;

	l_records_processed := 0;

	FOR r_stations IN ( SELECT stat_id FROM stations ) LOOP

		FOR r_combinations IN
		(
			SELECT stat_orig.stat_id stat_id_orig, stat_dest.stat_id stat_id_dest, prog.prog_id
			FROM stations stat_orig, stations stat_dest,
				countries coun_orig, countries coun_dest,
				regions regi_orig, product_groups prog
			WHERE stat_orig.stat_id = r_stations.stat_id
			AND coun_orig.coun_id = stat_orig.coun_id
			AND coun_dest.coun_id = stat_dest.coun_id
			AND regi_orig.regi_id = coun_orig.regi_id
			AND stat_dest.stat_id <> r_stations.stat_id
			AND
			(
				stat_orig.coun_id = stat_dest.coun_id AND prog.code = 'DOM'
				OR
				NOT( stat_orig.coun_id = stat_dest.coun_id ) AND
				(
					stat_orig.stat_ecx = 'Y' AND stat_dest.stat_ecx = 'Y' AND prog.code = 'ECX'
					OR
					NOT( stat_orig.stat_ecx = 'Y' AND stat_dest.stat_ecx = 'Y' ) AND prog.code IN ( 'DOX', 'WPX', 'DDF' )
				)
			)
			AND NOT EXISTS
			(
				SELECT lapg_id
				FROM lane_product_groups lapg
				WHERE stat_id_origin = r_stations.stat_id
				AND stat_id_dest = stat_dest.stat_id
				AND prog_id = prog.prog_id
			)
		)
		LOOP

			INSERT INTO lane_product_groups ( lapg_id, stat_id_origin, stat_id_dest, prog_id )
			VALUES ( seq_lapg.nextval, r_combinations.stat_id_orig, r_combinations.stat_id_dest, r_combinations.prog_id );

			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 1 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 2 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 3 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 4 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 5 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 6 );
			INSERT INTO lpg_targets ( lapg_id, lpgt_dow ) VALUES ( seq_lapg.currval, 7 );

			l_records_processed := l_records_processed + 1;

		END LOOP;

		COMMIT; -- <<<<

	END LOOP;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL,
		p_phase_code => l_phase_code, p_records_processed => l_records_processed );

	COMMIT;

	pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_log_level => pck_dhl_log.CONFIG_LEVEL_NORMAL, p_records_processed => l_records_processed );

EXCEPTION
	WHEN others THEN
		pck_dhl_log.p_log ( p_process_name => C_PROCESS_NAME, p_procedure_name => C_PROCEDURE_NAME, p_phase_code => l_phase_code, p_log_level => pck_dhl_log.LOG_LEVEL_ORACLE, p_message_code => SQLCODE );
	RAISE;
END;
GO


--* /PATCH







--* PATCH "3.0.4" --> "3.0.5"

--* MESSAGE START 'Switching descriptions of the sysPropShipPerc and sysPropBaseEtp system setting'
UPDATE SYSTEM_SETTINGS
SET DESCRIPTION = 'System proposed ETP'
WHERE NAME = 'sysPropBaseEtp'
GO

UPDATE SYSTEM_SETTINGS
SET DESCRIPTION = 'Percentage of shipment captured'
WHERE NAME = 'sysPropShipPerc'
GO

--* MESSAGE START 'Setting the sysPropShipPerc system setting to 80'
UPDATE SYSTEM_SETTINGS
SET VALUE = 80
WHERE NAME = 'sysPropShipPerc'
GO

--* MESSAGE START 'Setting the sysPropBaseEtp system setting to 85'
UPDATE SYSTEM_SETTINGS
SET VALUE = 85
WHERE NAME = 'sysPropBaseEtp'
GO

--* /PATCH







--* PATCH "3.0.4" --> "3.1.0"

--* MESSAGE START 'Upgrading procedure TTS.PRC_REMOVE_LANE'
CREATE OR REPLACE PROCEDURE "TTS"."PRC_REMOVE_LANE" ( p_lane_id IN INTEGER ) IS

	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_remove_lane';	

	l_records_processed log_dhl.records_processed%TYPE := 0;	
	l_records_ctt PLS_INTEGER := 0;
	l_records_prae PLS_INTEGER := 0;
	l_records_proa PLS_INTEGER := 0;
	l_records_prop PLS_INTEGER := 0;
	l_records_lpg PLS_INTEGER := 0;
	l_temp_proc PLS_INTEGER := 0;
	
BEGIN

	DELETE FROM TTS.SHIPMENT_COUNTS
	WHERE LAPG_ID = p_lane_id;
		
	l_temp_proc := SQL%ROWCOUNT;
	l_records_ctt := l_records_ctt + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM 
	(
		SELECT PRAE.ROWID
		FROM TTS.PROPOSAL_APPROVAL_EMAILS PRAE, TTS.PROPOSAL_APPROVALS PROA, TTS.PROPOSALS PROP
		WHERE PROA.PROA_ID = PRAE.PROA_ID
		AND PROP.PROP_ID = PROA.PROP_ID
		AND PROP.LAPG_ID = p_lane_id
	);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_prae := l_records_prae + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM 
	(
		SELECT PROA.ROWID
		FROM TTS.PROPOSAL_APPROVALS PROA, TTS.PROPOSALS PROP
		WHERE PROP.PROP_ID = PROA.PROP_ID
		AND PROP.LAPG_ID = p_lane_id
	);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_proa := l_records_proa + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM 
	(
		SELECT PROT.ROWID
		FROM TTS.PROPOSAL_TARGETS PROT, TTS.PROPOSALS PROP
		WHERE PROP.PROP_ID = PROT.PROP_ID
		AND PROP.LAPG_ID = p_lane_id
	);

	DELETE FROM TTS.PROPOSALS PROP
	WHERE PROP.LAPG_ID = p_lane_id;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_prop := l_records_prop + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM TTS.LPG_TARGETS
	WHERE LAPG_ID = p_lane_id;

	DELETE FROM TTS.LANE_PRODUCT_GROUPS
	WHERE LAPG_ID = p_lane_id;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_lpg := l_records_lpg + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	INSERT INTO TTS.log_dhl ( logd_id, process_name, batch, procedure_name, logging_timestamp, log_level, records_processed, parameter_text )
	VALUES ( seq_logd.NEXTVAL, C_PROCESS_NAME, 0, C_PROCEDURE_NAME, SYSDATE, 0, l_records_processed, 'Removed lane ' || p_lane_id );

	COMMIT;
	
	dbms_output.put_line( 'Removing for lane [' || p_lane_id  || '] a total of ' || l_records_processed || ' records:' );
	dbms_output.put_line( ' from CUM_TRANSIT_TIMES: ' || l_records_ctt );
	dbms_output.put_line( ' from PROPOSAL_APPROVAL_EMAILS: ' || l_records_prae );
	dbms_output.put_line( ' from PROPOSAL_APPROVALS: ' || l_records_proa );
	dbms_output.put_line( ' from PROPOSALS: ' || l_records_prop );
	dbms_output.put_line( ' from LANE_PRODUCT_GROUPS: ' || l_records_lpg );
	
END;
GO

--* MESSAGE START 'Upgrading procedure TTS.PRC_REMOVE_STATION'
CREATE OR REPLACE PROCEDURE "TTS"."PRC_REMOVE_STATION" ( p_iata_code IN VARCHAR2 ) IS

	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_remove_station';	

	l_records_processed log_dhl.records_processed%TYPE := 0;	
	l_records_lanes PLS_INTEGER := 0;
	l_records_dpo PLS_INTEGER := 0;
	l_records_dpd PLS_INTEGER := 0;
	l_records_dwo PLS_INTEGER := 0;
	l_records_dwd PLS_INTEGER := 0;
	l_records_sta PLS_INTEGER := 0;
	l_temp_proc PLS_INTEGER := 0;
	
BEGIN

	FOR r_lanes IN
	(
		SELECT DISTINCT LAPG.LAPG_ID
		FROM TTS.STATIONS STAT
		JOIN TTS.LANE_PRODUCT_GROUPS LAPG
			ON LAPG.STAT_ID_DEST = STAT.STAT_ID OR LAPG.STAT_ID_ORIGIN = STAT.STAT_ID
		WHERE STAT.IATA_CODE = p_iata_code
	)
	LOOP

		prc_remove_lane( r_lanes.lapg_id );
		
		l_temp_proc := SQL%ROWCOUNT;
		l_records_lanes := l_records_lanes + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;	

	END LOOP;

	DELETE FROM TTS.DELAYS_POSTCODE_DEST
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );

	l_temp_proc := SQL%ROWCOUNT;
	l_records_dpd := l_records_dpd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;	
	
	DELETE FROM TTS.DELAYS_POSTCODE_ORIG
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dpo := l_records_dpo + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	DELETE FROM TTS.DELAYS_WEIGHT_DEST
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dwd := l_records_dwd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	DELETE FROM TTS.DELAYS_WEIGHT_ORIG
	WHERE STAT_ID = ( SELECT STAT_ID FROM STATIONS WHERE IATA_CODE = p_iata_code );
	
	l_temp_proc := SQL%ROWCOUNT;
	l_records_dwo := l_records_dwo + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM TTS.STATIONS
	WHERE IATA_CODE = p_iata_code;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_sta := l_records_sta + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	INSERT INTO TTS.log_dhl ( logd_id, process_name, batch, procedure_name, logging_timestamp, log_level, records_processed, parameter_text )
	VALUES ( seq_logd.NEXTVAL, C_PROCESS_NAME, 0, C_PROCEDURE_NAME, SYSDATE, 0, l_records_processed, 'Removed station ' || p_iata_code );

	COMMIT;
	
	dbms_output.put_line( 'Removing for station [' || p_iata_code  || '] a total of ' || l_records_processed || ' records:' );
	dbms_output.put_line( ' from DELAYS_POSTCODE_DEST: ' || l_records_dpd );
	dbms_output.put_line( ' from DELAYS_POSTCODE_ORIG: ' || l_records_dpo );
	dbms_output.put_line( ' from DELAYS_WEIGHT_DEST: ' || l_records_dwd );
	dbms_output.put_line( ' from DELAYS_WEIGHT_ORIG: ' || l_records_dwo );
	dbms_output.put_line( ' from STATIONS: ' || l_records_sta );
	
END;
GO

--* MESSAGE START 'Upgrading procedure TTS.PRC_REMOVE_COUNTRY'
CREATE OR REPLACE PROCEDURE "TTS"."PRC_REMOVE_COUNTRY" ( p_country_code IN VARCHAR2 ) IS

	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_remove_country';	

	l_records_processed log_dhl.records_processed%TYPE := 0;	
	l_records_dvd PLS_INTEGER := 0;
	l_records_dcd PLS_INTEGER := 0;
	l_records_uc PLS_INTEGER := 0;
	l_records_ap PLS_INTEGER := 0;
	l_records_iata PLS_INTEGER := 0;
	l_records_country PLS_INTEGER := 0;
	l_temp_proc PLS_INTEGER := 0;
	
BEGIN

	DELETE FROM TTS.DELAYS_VALUE_DEST
	WHERE COUN_ID = (	SELECT	COUN_ID
						FROM	TTS.COUNTRIES
						WHERE	CTRY_CODE = p_country_code
					);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_dvd := l_records_dvd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	DELETE FROM TTS.DELAYS_COMMODITY_DEST
	WHERE COUN_ID = (	SELECT	COUN_ID
						FROM	TTS.COUNTRIES
						WHERE	CTRY_CODE = p_country_code
					);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_dcd := l_records_dcd + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	DELETE FROM TTS.USER_COUNTRIES
	WHERE COUN_ID = (	SELECT	COUN_ID
						FROM	TTS.COUNTRIES
						WHERE	CTRY_CODE = p_country_code
					);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_uc := l_records_uc + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	FOR r_appp IN
	(
		SELECT AP.APPP_ID
		FROM TTS.APPROVAL_PROCESS AP 
				LEFT OUTER JOIN TTS.COUNTRIES CO ON AP.COUN_ID_ORIGIN = CO.COUN_ID
				LEFT OUTER JOIN TTS.COUNTRIES CD ON AP.COUN_ID_DEST = CD.COUN_ID
		WHERE CO.CTRY_CODE = p_country_code
		OR CD.CTRY_CODE = p_country_code
	)
	LOOP

		DELETE FROM TTS.PROPOSAL_APPROVALS
		WHERE APPP_ID = r_appp.appp_id;

		DELETE FROM TTS.APPROVAL_PROCESS AP
		WHERE AP.APPP_ID = r_appp.appp_id;
				
		l_temp_proc := SQL%ROWCOUNT;
		l_records_ap := l_records_ap + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;
	
	END LOOP;

	FOR r_iata IN
	(
		SELECT IATA_CODE
		FROM TTS.STATIONS JOIN TTS.COUNTRIES ON STATIONS.COUN_ID = COUNTRIES.COUN_ID
		WHERE CTRY_CODE = p_country_code
	)
	LOOP

		PRC_REMOVE_STATION( r_iata.iata_code );

		l_temp_proc := SQL%ROWCOUNT;
		l_records_iata := l_records_iata + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

	END LOOP;

	DELETE FROM TTS.COUNTRIES
	WHERE CTRY_CODE = p_country_code;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_country := l_records_country + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	INSERT INTO TTS.log_dhl ( logd_id, process_name, batch, procedure_name, logging_timestamp, log_level, records_processed, parameter_text )
	VALUES ( seq_logd.NEXTVAL, C_PROCESS_NAME, 0, C_PROCEDURE_NAME, SYSDATE, 0, l_records_processed, 'Removed country ' || p_country_code );

	COMMIT;
	
	dbms_output.put_line( 'Removing for country [' || p_country_code  || '] a total of ' || l_records_processed || ' records:' );
	dbms_output.put_line( ' from DELAYS_VALUE_DEST: ' || l_records_dvd );
	dbms_output.put_line( ' from DELAYS_COMMODITY_DEST: ' || l_records_dcd );
	dbms_output.put_line( ' from USER_COUNTRIES: ' || l_records_uc );
	dbms_output.put_line( ' from APPROVAL_PROCESS: ' || l_records_ap );
	dbms_output.put_line( ' from STATIONS: ' || l_records_iata );
	dbms_output.put_line( ' from COUNTRIES: ' || l_records_country );

END;
GO

--* MESSAGE START 'Upgrading procedure TTS.PRC_REMOVE_REGION'
CREATE OR REPLACE PROCEDURE "TTS"."PRC_REMOVE_REGION" ( p_region_code IN VARCHAR2 ) IS

	C_PROCESS_NAME CONSTANT log_dhl.process_name%TYPE := 'TTS';
	C_PROCEDURE_NAME CONSTANT log_dhl.procedure_name%TYPE := 'prc_remove_region';	

	l_records_processed log_dhl.records_processed%TYPE := 0;	
	l_records_ur PLS_INTEGER := 0;
	l_records_ap PLS_INTEGER := 0;
	l_records_country PLS_INTEGER := 0;
	l_records_region PLS_INTEGER := 0;
	l_temp_proc PLS_INTEGER := 0;
	
BEGIN

	DELETE FROM TTS.USER_REGIONS
	WHERE REGI_ID = (	SELECT	REGI_ID
						FROM	TTS.REGIONS
						WHERE	REGION_CODE = p_region_code
					);

	l_temp_proc := SQL%ROWCOUNT;
	l_records_ur := l_records_ur + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;
	
	FOR r_appp IN
	(
		SELECT AP.APPP_ID
		FROM TTS.APPROVAL_PROCESS AP 
				LEFT OUTER JOIN TTS.REGIONS RO ON AP.REGI_ID_ORIGIN = RO.REGI_ID
				LEFT OUTER JOIN TTS.REGIONS RD ON AP.REGI_ID_DEST = RD.REGI_ID
		WHERE RO.REGION_CODE = p_region_code
		OR RD.REGION_CODE = p_region_code
	)
	LOOP

		DELETE FROM TTS.PROPOSAL_APPROVALS
		WHERE APPP_ID = r_appp.appp_id;

		DELETE FROM TTS.APPROVAL_PROCESS AP
		WHERE AP.APPP_ID = r_appp.appp_id;
				
		l_temp_proc := SQL%ROWCOUNT;
		l_records_ap := l_records_ap + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;
	
	END LOOP;

	FOR r_country IN
	(
		SELECT CTRY_CODE
		FROM TTS.COUNTRIES JOIN TTS.REGIONS ON COUNTRIES.REGI_ID = REGIONS.REGI_ID
		WHERE REGION_CODE = p_region_code
	)
	LOOP

		PRC_REMOVE_COUNTRY( r_country.ctry_code );

		l_temp_proc := SQL%ROWCOUNT;
		l_records_country := l_records_country + l_temp_proc;
		l_records_processed := l_records_processed + l_temp_proc;

	END LOOP;

	DELETE FROM TTS.REGIONS
	WHERE REGION_CODE = p_region_code;

	l_temp_proc := SQL%ROWCOUNT;
	l_records_region := l_records_region + l_temp_proc;
	l_records_processed := l_records_processed + l_temp_proc;

	INSERT INTO TTS.log_dhl ( logd_id, process_name, batch, procedure_name, logging_timestamp, log_level, records_processed, parameter_text )
	VALUES ( seq_logd.NEXTVAL, C_PROCESS_NAME, 0, C_PROCEDURE_NAME, SYSDATE, 0, l_records_processed, 'Removed region ' || p_region_code );

	COMMIT;
	
	dbms_output.put_line( 'Removing for region [' || p_region_code  || '] a total of ' || l_records_processed || ' records:' );
	dbms_output.put_line( ' from USER_REGIONS: ' || l_records_ur );
	dbms_output.put_line( ' from APPROVAL_PROCESS: ' || l_records_ap );
	dbms_output.put_line( ' from COUNTRIES: ' || l_records_country );
	dbms_output.put_line( ' from REGIONS: ' || l_records_region );

END;
GO

--* /PATCH
