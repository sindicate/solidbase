
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
RUN "setup-1.1.sql";
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
FILE "output/export3.csv" ENCODING "UTF-8"
FROM SELECT * FROM TEMP;

EXPORT CSV
LOG EVERY 1 SECONDS
FILE "output/export3.csv" ENCODING "UTF-8"
FROM SELECT * FROM TEMP;

IMPORT CSV
log every 10 records
INTO TEMP
FILE "output/export3.csv" ENCODING "UTF-8";

IMPORT CSV
LOG EVERY 1 seconds
INTO TEMP
FILE "output/export3.csv" ENCODING "UTF-8";

--* /UPGRADE
