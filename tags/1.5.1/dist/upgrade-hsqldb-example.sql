--* ENCODING "ISO-8859-1"

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
--*		INIT "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*		UPGRADE "1.0.1" --> "1.0.2"
--*	/DEFINITION



--* // ========================================================================
--* INIT "" --> "1.1"
--* // ========================================================================

--* SET MESSAGE "    Creating table DBVERSION"

CREATE TABLE DBVERSION
(
	SPEC VARCHAR,
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
)
GO

--* SET MESSAGE "    Creating table DBVERSIONLOG"

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR NOT NULL,
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
)
GO

CREATE INDEX DBVERSIONLOG_INDEX1 ON DBVERSIONLOG ( TYPE, TARGET )
GO

--* /INIT



--* // ========================================================================
--* UPGRADE "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE "    Creating table USERS"

CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR NOT NULL,
	USER_PASSWORD VARCHAR NOT NULL
)
GO

--* SET MESSAGE "    Inserting admin user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' )
GO

--* SET MESSAGE "    Inserting user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'rené', '*****' )
GO

--* /UPGRADE



--* // ========================================================================
--* UPGRADE "1.0.1" --> "1.0.2"
--* // ========================================================================

--* SET MESSAGE "    Creating queue"

--* SELECT CONNECTION QUEUES

CREATE TABLE QUEUE1
(
	PRIORITY INT NOT NULL,
	MESSAGE VARCHAR NOT NULL
)
GO

--* /UPGRADE
