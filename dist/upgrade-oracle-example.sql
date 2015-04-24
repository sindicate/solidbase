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
--*	/DEFINITION



--* // ========================================================================
--* INIT "" --> "1.1"
--* // ========================================================================

--* SET MESSAGE "    Creating table DBVERSION"

CREATE TABLE DBVERSION
(
	SPEC VARCHAR2(5) NOT NULL,
	VERSION VARCHAR2(20),
	TARGET VARCHAR2(20),
	STATEMENTS INTEGER NOT NULL
)
GO

--* SET MESSAGE "    Creating table DBVERSIONLOG"

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR2(1) NOT NULL,
	SOURCE VARCHAR2(20),
	TARGET VARCHAR2(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP DATE NOT NULL,
	COMMAND VARCHAR2(4000),
	RESULT VARCHAR2(4000)
)
GO

--* /INIT



--* // ========================================================================
--* UPGRADE "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE "    Creating table USERS"

CREATE TABLE USERS
(
	USER_USERNAME VARCHAR2(26) NOT NULL,
	USER_PASSWORD VARCHAR2(30) NOT NULL
)
GO

--* SET MESSAGE "    Inserting admin user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' )
GO

--* SET MESSAGE "    Inserting user"

INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'rené', '*****' )
GO

--* /UPGRADE
