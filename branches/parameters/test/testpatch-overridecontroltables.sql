
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



--* DEFINITION
--*		VERSION TABLE SOLIDBASE_VERSION LOG TABLE SOLIDBASE_LOG
--*		DELIMITER IS TRAILING ; 	
--*		SETUP "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*	/DEFINITION



--* SETUP "" --> "1.1"

CREATE TABLE SOLIDBASE_VERSION
(
	SPEC VARCHAR(5) NOT NULL,
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL 
);

CREATE TABLE SOLIDBASE_LOG
(
	TYPE VARCHAR(1) NOT NULL,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);

--* /SETUP



--* UPGRADE "" --> "1.0.1"

CREATE TABLE USERS2
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR(40) NOT NULL,
	USER_PASSWORD VARCHAR(40) NOT NULL
);

INSERT INTO USERS2 ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' );

--* /UPGRADE
