
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

--* // ========================================================================

--*	DEFINITION
--*		SETUP "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*		UPGRADE "1.0.1" --> "1.0.2"
--*		UPGRADE "1.0.1" --> "1.1.0"
--*		DOWNGRADE "1.1.0" --> "1.0.1"
--*	/DEFINITION







--* // ========================================================================
--* SETUP "" --> "1.1"
--* // ========================================================================

--* SECTION "Creating table DBVERSION"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL,
	SPEC VARCHAR(5) NOT NULL
);

--* // The patch tool expects to be able to use the DBVERSION table after the *first* sql statement

--* SECTION "Creating table DBVERSIONLOG"

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR(1) NOT NULL,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);

CREATE INDEX DBVERSIONLOG_INDEX1 ON DBVERSIONLOG ( TYPE, TARGET );

--* // The existence of DBVERSIONLOG will automatically be detected at the end of this patch

--* /SETUP







--* UPGRADE "" --> "1.0.1"
--* UPGRADE "1.0.1" --> "1.0.2"

--* /UPGRADE







--* // ========================================================================
--* UPGRADE "1.0.1" --> "1.1.0"
--* // ========================================================================

--* USE CONNECTION USER

--* // We need at least one sql without a message. This is a test too.

CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR(40) NOT NULL,
	USER_PASSWORD VARCHAR(40) NOT NULL
);

--* SECTION "Inserting admin users"

--*// Need to do three statements to test if the dots come on one line

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );

--* /UPGRADE

--* // ========================================================================







--* DOWNGRADE "1.1.0" --> "1.0.1"

--* /DOWNGRADE
