--* ENCODING "ISO-8859-1"

--* // Copyright 2006 Ren� M. de Bloois

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
--*	/DEFINITION



--* // ========================================================================
--* SETUP "" --> "1.1"
--* // ========================================================================

--* SECTION "Creating table DBVERSION"

CREATE TABLE DBVERSION
(
	SPEC VARCHAR,
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
);

--* SECTION "Creating table DBVERSIONLOG"

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR NOT NULL,
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
);

CREATE INDEX DBVERSIONLOG_INDEX1 ON DBVERSIONLOG ( TYPE, TARGET );

--* /SETUP



--* // ========================================================================
--* UPGRADE "" --> "1.0.1"
--* // ========================================================================

--* SCRIPT EXPANSION ON

--* SECTION "Creating table ${users1}"

CREATE TABLE ${users1}
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR NOT NULL,
	USER_PASSWORD VARCHAR NOT NULL
);

--* SECTION "Inserting admin user"

INSERT INTO ${users1} ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' );

--* SECTION "Inserting user"

INSERT INTO ${users1} ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'ren�', '*****' );

--* /UPGRADE
