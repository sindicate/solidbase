
--* // Copyright 2009 Ren� M. de Bloois

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
--*		UPGRADE "" --> "1.0.1"
--*		UPGRADE "1.0.1" --> "1.0.2"
--*		UPGRADE "1.0.2" --> "1.0.3"
--*		UPGRADE "1.0.3" --> "1.0.4"
--*		UPGRADE "1.0.3" --> "1.0.5"
--*	/DEFINITION







--* UPGRADE "" --> "1.0.1"

--* SECTION "Creating control tables"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL 
);

CREATE TABLE DBVERSIONLOG
(
	ID INTEGER IDENTITY, -- An index might be needed here to let the identity perform
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);

--* /UPGRADE







--* UPGRADE "1.0.1" --> "1.0.2"

CREATE TABLE TEMP ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

--* SECTION.0 "Starting import"

IMPORT CSV INTO TEMP DATA
;

IMPORT CSV INTO TEMP DATA
"1","2","3";

--* // Newline enclosed in double quotes was recognized as the end of the record
IMPORT CSV INTO TEMP;
"1","2","3"
"1","
","3"
"1","2","3"

IMPORT CSV SEPARATED BY TAB INTO TEMP
DATA
"1"	"2"	"3"
"1"	"2"	"3"
"1"	"2"	
;

IMPORT CSV SEPARATED BY ; IGNORE WHITESPACE INTO TEMP DATA
 "1" ; "2" ; "3" 
 x ; "2
"; "3"
	y	;	2 2	;	" 3 "	;

--* SECTION "Generating SQLException"

--* IGNORE SQL ERROR 42504

--* // A dot should still be printed when an error is ignored
CREATE TABLE TEMP ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

--* /IGNORE SQL ERROR

--* SECTION.1 "Importing with linenumber"

CREATE TABLE TEMP2 ( LINENUMBER INTEGER, TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

IMPORT CSV SEPARATED BY ; PREPEND LINENUMBER INTO TEMP2 DATA
"1";"2";"3"
"1";"2";"3"
"1";"2";"3";

PRINT SELECT LINENUMBER FROM TEMP2;

--* SECTION.2 "Importing with column list"
--* SECTION.3 "And deeper"

CREATE TABLE TEMP3 ( TEMP1 INTEGER, TEMP2 VARCHAR(40), TEMP3 VARCHAR(40), TEMP4 VARCHAR(40) );

IMPORT CSV
SEPARATED BY |
INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )
VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-", 
TEST ''X' )
DATA
1|2
3|4;

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing from external file"

IMPORT CSV SKIP HEADER INTO TEMP2 FILE "data.csv" ENCODING "UTF-8";

--* /UPGRADE




--* UPGRADE "1.0.2" --> "1.0.3"

CREATE TABLE TEMP4 ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

--* // Empty string should become NULL 
IMPORT CSV INTO TEMP4 DATA
,"2","3";

CREATE TABLE TEMP5 ( TEMP1 VARCHAR(40) );

IMPORT CSV INTO TEMP5 DATA
1
""
3;

--* /UPGRADE




--* UPGRADE "1.0.3" --> "1.0.4"

IMPORT CSV SKIP HEADER INTO TEMP2 FILE "notexist.csv" ENCODING "UTF-8";

--* /UPGRADE




--* UPGRADE "1.0.3" --> "1.0.5"

CREATE TABLE TEMP6 ( ID INTEGER, CODE INTEGER, NAME VARCHAR(100), START_DATE DATE, END_DATE DATE );

LOAD JSON INTO TEMP6 FILE "data.json";

--* /UPGRADE
