--* ENCODING "UTF-8"

--* // Copyright 2010 René M. de Bloois

--* // Licensed under the Apache License, Version 2.0 (the "License");
--* // you may not use this file except in compliance with the License.
--* // You may obtain a copy of the License at

--* //     http://www.apache.org/licenses/LICENSE-2.0

--* // Unless required by applicable law or agreed to in writing, software
--* // distributed under the License is distributed on an "AS IS" BASIS,
--* // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--* // See the License for the specific language governing permissions and
--* // limitations under the License.

--* // ========================================================================



--*	DEFINITION
--*		INIT "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*	/DEFINITION



--* // ========================================================================
--* INIT "" --> "1.1"
--* // ========================================================================

--* SET MESSAGE "    Creating table DBVERSION"

CREATE TABLE DBVERSION
(
	SPEC VARCHAR(5) NOT NULL,
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS DECIMAL(4) NOT NULL
);

--* SET MESSAGE "    Creating table DBVERSIONLOG"

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR(1) NOT NULL,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT DECIMAL(4) NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);

CREATE INDEX DBVERSIONLOG_INDEX1 ON DBVERSIONLOG ( TYPE, TARGET );

--* /INIT



--* // ========================================================================
--* UPGRADE "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE "    Creating tables"

--* // Apache Derby creates backing indexes on primary key, unique and foreign key constraints

CREATE TABLE USERS
(
	USER_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT USER_PK PRIMARY KEY,
	USER_USERNAME VARCHAR(16) NOT NULL CONSTRAINT USER_AK UNIQUE,
	USER_NAME VARCHAR(60) NOT NULL,
	USER_PASSWORD VARCHAR(16),
	USER_EMAIL VARCHAR(40)
);

CREATE TABLE USER_ROLES
(
	USER_ID INTEGER NOT NULL,
	ROLE_CODE VARCHAR(3) NOT NULL, 
	CONSTRAINT USRO_PK PRIMARY KEY ( USER_ID, ROLE_CODE ),
	CONSTRAINT USRO_FK_USER FOREIGN KEY ( USER_ID ) REFERENCES USERS ( USER_ID )
);

CREATE TABLE CLIENTS
(
	CLIE_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT CLIE_PK PRIMARY KEY,
	CLIE_NAME VARCHAR(60) NOT NULL
);

CREATE TABLE ISSUES
(
	ISSU_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT ISSU_PK PRIMARY KEY,
	CLIE_ID INTEGER NOT NULL,
	ISSU_TEXT VARCHAR(100) NOT NULL,
	ISSU_STATUS VARCHAR(3) NOT NULL,
	CONSTRAINT ISSU_FK_CLIENT FOREIGN KEY ( CLIE_ID ) REFERENCES CLIENTS ( CLIE_ID )
);

CREATE TABLE NOTES
(
	NOTE_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT NOTE_PK PRIMARY KEY,
	CLIE_ID INTEGER,
	ISSU_ID INTEGER,
	USER_ID_CREATOR INTEGER NOT NULL,
	NOTE_TIMESTAMP TIMESTAMP NOT NULL,
	NOTE_TEXT VARCHAR(1000) NOT NULL,
	CONSTRAINT NOTE_FK_USER_CREATOR FOREIGN KEY ( USER_ID_CREATOR ) REFERENCES USERS ( USER_ID )
);

CREATE TABLE FLAGS
(
	FLAG_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT FLAG_PK PRIMARY KEY,
	NOTE_ID INTEGER NOT NULL,
	USER_ID_ASSIGNED INTEGER,
	FLAG_TEXT VARCHAR(100) NOT NULL,
	FLAG_PRIORITY VARCHAR(1) NOT NULL,
	CONSTRAINT FLAG_FK_NOTE FOREIGN KEY ( NOTE_ID ) REFERENCES NOTES ( NOTE_ID ),
	CONSTRAINT FLAG_FK_USER_ASSIGNED FOREIGN KEY ( USER_ID_ASSIGNED ) REFERENCES USERS ( USER_ID )
);

--* /UPGRADE



