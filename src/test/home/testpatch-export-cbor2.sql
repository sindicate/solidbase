
--* // Copyright 2016 Ren� M. de Bloois

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

--* EXPORT CBOR SET ADD_CREATED_DATE = OFF

EXPORT CBOR FILE "tmp/export21.cbor"
FROM SELECT * FROM TEMP1;

EXPORT CBOR DATE AS TIMESTAMP FILE "tmp/export22.cbor"
FROM SELECT * FROM TEMP1;

--* END UPGRADE



--* UPGRADE "2" --> "3"

IMPORT CBOR
INTO TEMP1 ( TINYINT, SMALLINT, INTEGER, BIGINT, DECIMAL, FLOAT, BOOLEAN, CHAR, VARCHAR, CLOB, BINARY, VARBINARY, BLOB, DATE, TIME, TIMESTAMP, DATEZ, TIMEZ, TIMESTAMPZ )
FILE "tmp/export21.cbor";

--* END UPGRADE



