
--* // Copyright 2016 René M. de Bloois

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
--*		UPGRADE "1" --> "2"
--*		UPGRADE "2" --> "3"
--*	/DEFINITION



--* SETUP "" --> "1.1"
RUN "setup-1.1.sql";
--* /SETUP



--* UPGRADE "" --> "1"

CREATE TABLE TEMP1 (
	TINYINT TINYINT, -- byte
	SMALLINT SMALLINT, -- short
	INTEGER INTEGER, -- int
	BIGINT BIGINT, -- long
	DECIMAL DECIMAL, -- BigDecimal
	FLOAT FLOAT, -- double
	BOOLEAN BOOLEAN,
	CHAR CHAR, -- char
	VARCHAR VARCHAR( 1000 ), -- String
	CLOB CLOB, -- Reader
	BINARY BINARY, -- byte
	VARBINARY VARBINARY( 1000 ), -- byte[]
	BLOB BLOB, -- InputStream
	DATE DATE,
	TIME TIME,
	TIMESTAMP TIMESTAMP,
	DATEZ DATE,
	TIMEZ TIME(6) WITH TIME ZONE,
	TIMESTAMPZ TIMESTAMP(6) WITH TIME ZONE,
);

--* END UPGRADE



--* UPGRADE "1" --> "2"

EXPORT CSV WITH HEADER FILE "export5.csv" ENCODING "UTF-8"
SELECT * FROM TEMP1;

DUMP JSON DATE AS TIMESTAMP FILE "export5.json"
BINARY FILE "export5.json.bin"
COLUMN PICTURE TO BINARY FILE "export5.bin"
COLUMN TEXT2 TO TEXT FILE "export5.txt"
SELECT * FROM TEMP1;

DUMP CBOR FILE "export5.cbor" FROM
SELECT * FROM TEMP1;

--* END UPGRADE



--* UPGRADE "2" --> "3"

IMPORT CBOR
INTO TEMP1 ( TINYINT, SMALLINT, INTEGER, BIGINT, DECIMAL, FLOAT, BOOLEAN, CHAR, VARCHAR, CLOB, BINARY, VARBINARY, BLOB, DATE, TIME, TIMESTAMP, DATEZ, TIMEZ, TIMESTAMPZ )
FILE "export5.cbor";

--* END UPGRADE



