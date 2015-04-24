
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

--*	PATCHES
--*		PATCH "" --> "0"
--*		PATCH "0" --> "1.0.1"
--*	/PATCHES







--* // ========================================================================
--* PATCH "" --> "0"
--* // ========================================================================

--* SET MESSAGE "Creating SolidBase tables"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
)
GO

CREATE TABLE DBVERSIONLOG
(
	ID INTEGER IDENTITY, -- An index might be needed here to let the identity perform
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT VARCHAR NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
)
GO

CREATE TABLE DBCHANGELOG
(
	STAMP DATE NOT NULL,
	NAME VARCHAR2(40) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	COMMAND VARCHAR2(4000),
	RESULT VARCHAR2(4000)
)
GO

--* /PATCH







--* // Consolidation
--* // ========================================================================
--* PATCH "0" --> "1.0.1"
--* // ========================================================================

--* CHANGE "RENE-001"

--* SET MESSAGE "Creating table USERS"

CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR NOT NULL,
	USER_PASSWORD VARCHAR NOT NULL
)
GO

--* /CHANGE

--* CHANGE "RENE-002"

--* SET MESSAGE "Inserting admin user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' )
GO

--* /CHANGE

--* /PATCH







--* // ================================
--* CHANGE "RENE-003"
--* // ================================

--* SET MESSAGE "Inserting another user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'rene', '*****' )
GO

--* /CHANGE







