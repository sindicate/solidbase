
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
--*		SETUP "" --> "1.1"
--*		UPGRADE "" --> "1"
--*	/DEFINITION

--* SETUP "" --> "1.1"
CREATE TABLE DBVERSION ( SPEC VARCHAR(5) NOT NULL, VERSION VARCHAR(20), TARGET VARCHAR(20), STATEMENTS INTEGER NOT NULL );
CREATE TABLE DBVERSIONLOG ( TYPE VARCHAR(1) NOT NULL, SOURCE VARCHAR(20), TARGET VARCHAR(20) NOT NULL, STATEMENT INTEGER NOT NULL, STAMP TIMESTAMP NOT NULL, COMMAND VARCHAR(4000), RESULT VARCHAR(4000) );
--* /SETUP

--* UPGRADE "" --> "1"
CREATE TABLE TEMP ( TEMP1 VARCHAR(40) NOT NULL, TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

IMPORT CSV INTO TEMP;
1,2,3
4,5,6
7,8,9

INSERT INTO TEMP SELECT * FROM TEMP; -- 6
INSERT INTO TEMP SELECT * FROM TEMP; -- 12
INSERT INTO TEMP SELECT * FROM TEMP; -- 24
INSERT INTO TEMP SELECT * FROM TEMP; -- 48
INSERT INTO TEMP SELECT * FROM TEMP; -- 96
--INSERT INTO TEMP SELECT * FROM TEMP; -- 192;

EXPORT CSV
LOG EVERY 10 RECORDS
FILE "export.csv" ENCODING "UTF-8"
SELECT TOP 100 * FROM TEMP, TEMP, TEMP;

--* /UPGRADE
