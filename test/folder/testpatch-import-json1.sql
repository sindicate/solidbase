
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

--* // No data
IMPORT JSON INTO TEMP;

IMPORT JSON INTO PUBLIC.TEMP;
["1","2","3"]

--* // Newline enclosed in double quotes was recognized as the end of the record
IMPORT JSON INTO TEMP;
[ "1", "2",  "3" ]
[ "1", "\n", "3" ]
[ "1", "2",  "3" ]

IMPORT JSON INTO TEMP;
{"version":"1.0","fields":[{"type":"VARCHAR"},{"type":"VARCHAR"},{"type":"VARCHAR"}]}
 ["1" , "2" , "3"] 
 ["x" , "2\n", "3"]
	["y"	,	"2 2"	,	" 3 "]	

--* SECTION.1 "Importing with linenumber"

CREATE TABLE TEMP2 ( LINENUMBER INTEGER, TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

	IMPORT JSON PREPEND LINENUMBER INTO TEMP2;
["1","2","3"]
["1","2","3"]
["1","2","3"]

PRINT SELECT LINENUMBER FROM TEMP2;

--* SECTION.2 "Importing with column list"

CREATE TABLE TEMP3 ( TEMP1 INTEGER, TEMP2 VARCHAR(40), TEMP3 VARCHAR(40), TEMP4 VARCHAR(40) );

IMPORT JSON
INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )
VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-", 
TEST ''X' );
[1,2]
[3,4]

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing from external file"

IMPORT JSON INTO TEMP2 FILE "data.json";

--* // Skip the inline data
--* SKIP
IMPORT JSON INTO TEMP;
["1","2","3"]
["1","\t","3"]
["1","2","3"]

--* END SKIP

--* SECTION.2 "Importing through update"

IMPORT
JSON
TO 
UPDATE TEMP3
SET TEMP3 = 10 * :2 
WHERE TEMP1 = :1;
[2, 6]
[4, 7]
[3, 1]
[5, 1]

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing through delete"

IMPORT JSON FILE "data.json" TO DELETE FROM TEMP3 WHERE TEMP1 = 2 + :3;
PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing through merge"

CREATE TABLE TEMP7 ( ID INTEGER, DESC VARCHAR(40) );

IMPORT JSON INTO TEMP7;
["1","The first record"]
["2","The 2nd record"]

IMPORT JSON
TO MERGE INTO TEMP7
USING ( VALUES ( CAST( :1 AS INTEGER ), CAST( :2 AS VARCHAR(40) ) ) ) AS VALS( ID, DESC )
ON TEMP7.ID = VALS.ID
WHEN MATCHED THEN UPDATE SET TEMP7.DESC = VALS.DESC
WHEN NOT MATCHED THEN INSERT VALUES VALS.ID, VALS.DESC;
["2","The second record"]
["3","The third record"]

PRINT SELECT ID ||' '|| DESC FROM TEMP7;

--* /UPGRADE




--* UPGRADE "1.0.2" --> "1.0.3"

CREATE TABLE TEMP4 ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

--* // Nulls 
IMPORT JSON INTO TEMP4;
[null,"2","3"]

CREATE TABLE TEMP5 ( TEMP1 VARCHAR(40) );

IMPORT CSV INTO TEMP5;
[1]
[null]
[3]

--* /UPGRADE




--* UPGRADE "1.0.3" --> "1.0.4"

IMPORT JSON INTO TEMP2 FILE "notexist.json";

--* /UPGRADE
