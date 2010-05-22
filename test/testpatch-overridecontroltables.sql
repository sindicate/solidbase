
--* // Copyright 2010 Ren� M. de Bloois

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



--* DEFINITION
--*		VERSION TABLE SOLIDBASE_VERSION LOG TABLE SOLIDBASE_LOG
--*		DELIMITER IS TRAILING ; 	
--*		INIT "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*	/DEFINITION



--* INIT "" --> "1.1"

CREATE TABLE SOLIDBASE_VERSION
(
	SPEC VARCHAR NOT NULL,
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
);

CREATE TABLE SOLIDBASE_LOG
(
	TYPE VARCHAR NOT NULL,
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
);

--* /INIT



--* UPGRADE "" --> "1.0.1"

CREATE TABLE USERS2
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR NOT NULL,
	USER_PASSWORD VARCHAR NOT NULL
);

INSERT INTO USERS2 ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' );

--* /UPGRADE
