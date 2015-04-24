
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
--*		UPGRADE "" --> "1"
--*		UPGRADE "1" --> "2"
--*		UPGRADE "1" --> "3"
--*	/DEFINITION

--* UPGRADE "" --> "1"
CREATE TABLE DBVERSION ( VERSION VARCHAR(20), TARGET VARCHAR(20), STATEMENTS INTEGER NOT NULL );
CREATE TABLE DBVERSIONLOG ( ID INTEGER IDENTITY, SOURCE VARCHAR(20), TARGET VARCHAR(20) NOT NULL, STATEMENT INTEGER NOT NULL, STAMP TIMESTAMP NOT NULL, COMMAND VARCHAR(4000), RESULT VARCHAR(4000) );
CREATE TABLE DUAL ( DUMMY VARCHAR(5) );
INSERT INTO DUAL VALUES ( 'DUMMY' );
--* /UPGRADE

--* UPGRADE "1" --> "2"
PRINT SELECT {d '2010-01-01'} FROM DUAL;
--* /UPGRADE

--* UPGRADE "1" --> "3"
--* jdbc escape processing on
PRINT SELECT {d '2010-01-01'} FROM DUAL;
--* /UPGRADE
