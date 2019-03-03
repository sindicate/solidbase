
--* // Copyright 2009 René M. de Bloois

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
IMPORT CBOR INTO TEMP;

IMPORT CBOR INTO PUBLIC.TEMP;
9F            -- IARRAY
	83        -- ARRAY 3
		61 31 -- TEXT 1: "1"
		61 32
		61 33
FF            -- BREAK

--* // Newline enclosed in double quotes was recognized as the end of the record
IMPORT CBOR INTO TEMP;
9F
	83 6131 6132 6133
	83 6131 610A 6133
	83 6131 6132 6133
FF

IMPORT CBOR INTO TEMP;
A1 -- MAP 1
	66 6669656c6473 -- fields
	83 -- ARRAY 3
		A1
			64 74 79 70 65 -- type
			67 44 45 43 49 4d 41 4c -- VARCHAR
		A1
			64 74 79 70 65 -- type
			67 44 45 43 49 4d 41 4c -- VARCHAR
		A1
			64 74 79 70 65 -- type
			67 44 45 43 49 4d 41 4c -- VARCHAR
9F
	83 6131 6132 6133
	83 6178 62320A 6133
	83 6179 63322032 63203320
FF
			
--* SECTION.1 "Importing with linenumber"

CREATE TABLE TEMP2 ( LINENUMBER INTEGER, TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

	IMPORT CBOR PREPEND RECORDNUMBER INTO TEMP2;
9F
	83 6131 6132 6133
	83 6131 6132 6133
	83 6131 6132 6133
FF

PRINT SELECT LINENUMBER FROM TEMP2;

--* SECTION.2 "Importing with column list"

CREATE TABLE TEMP3 ( TEMP1 INTEGER, TEMP2 VARCHAR(40), TEMP3 VARCHAR(40), TEMP4 VARCHAR(40) );

IMPORT CBOR
INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )
VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-", 
TEST ''X' );
9F
	82 01 02
	82 03 04
FF

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing from external file"

IMPORT CBOR INTO TEMP2 FILE "data.cbor";

--* // Skip the inline data
--* SKIP
IMPORT CBOR INTO TEMP;
9F
	83 6131 6132 6133
	83 6131 6132 6133
	83 6131 6132 6133
FF

--* END SKIP

--* SECTION.2 "Importing through update"

IMPORT
CBOR
NOBATCH
EXEC 
UPDATE TEMP3
SET TEMP3 = 10 * :2 
WHERE TEMP1 = :1;
9F
	82 02 06
	82 04 07
	82 03 01
	82 05 01
FF

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing through delete"

IMPORT CBOR FILE "data.cbor" EXEC DELETE FROM TEMP3 WHERE TEMP1 = 2 + :3;
PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3;

--* SECTION.2 "Importing through merge"

CREATE TABLE TEMP7 ( ID INTEGER, DESC VARCHAR(40) );

IMPORT CBOR INTO TEMP7;
9F
	82 61 31 70 546865206669727374207265636F7264 -- ["1","The first record"] 
	82 61 32 6E 54686520326E64207265636F7264 -- ["2","The 2nd record"] 
FF

IMPORT CBOR
EXEC MERGE INTO TEMP7
USING ( VALUES ( CAST( :1 AS INTEGER ), CAST( :2 AS VARCHAR(40) ) ) ) AS VALS( ID, DESC )
ON TEMP7.ID = VALS.ID
WHEN MATCHED THEN UPDATE SET TEMP7.DESC = VALS.DESC
WHEN NOT MATCHED THEN INSERT VALUES VALS.ID, VALS.DESC;
9F
	82 61 32 71 546865207365636F6E64207265636F7264 -- ["2","The second record"] 
	82 61 33 70 546865207468697264207265636F7264 -- ["3","The third record"] 
FF

PRINT SELECT ID ||' '|| DESC FROM TEMP7;

--* /UPGRADE




--* UPGRADE "1.0.2" --> "1.0.3"

CREATE TABLE TEMP4 ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

--* // Nulls 
IMPORT CBOR INTO TEMP4;
9F83F661326133FF -- [null,"2","3"]

CREATE TABLE TEMP5 ( TEMP1 VARCHAR(40) );

IMPORT CBOR INTO TEMP5;
9F
	8101 -- 1
	81F6 -- null
	8103 -- 3
FF

--* /UPGRADE




--* UPGRADE "1.0.3" --> "1.0.4"

IMPORT CBOR INTO TEMP2 FILE "notexist.cbor";

--* /UPGRADE
