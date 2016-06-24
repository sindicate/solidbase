
--* // Copyright 2011 René M. de Bloois

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
--*	/DEFINITION

--* SETUP "" --> "1.1"
RUN "setup-1.1.sql";
--* /SETUP

--* UPGRADE "" --> "1"
CREATE TABLE TEMP1 ( ID INTEGER, PICTURE BLOB, TEXT VARCHAR(100), TEXT2 CLOB, DATE1 DATE );
--* /UPGRADE

--* UPGRADE "1" --> "2"

 EXPORT CBOR
	FILE "export11.cbor" FROM
SELECT * FROM TEMP1;

EXPORT CBOR
	FILE "export12.cbor" FROM
SELECT ID, PICTURE, TEXT, TEXT2
FROM TEMP1;

 EXPORT CBOR
	DATE AS TIMESTAMP
	FILE "export13.cbor"
	FROM
SELECT * FROM TEMP1;

--* EXPORT CBOR SET ADD_CREATED_DATE = OFF

		EXPORT CBOR
	FILE "export14.cbor"
	FROM
SELECT * FROM TEMP1;

--* EXPORT CBOR SET ADD_CREATED_DATE = ON

EXPORT CBOR
FILE "folder/export15.cbor"
COLUMN DATE1 SKIP FROM
SELECT ID, PICTURE, TEXT, TEXT2, DATE1
FROM TEMP1;

--* /UPGRADE

These cannot be part of a filename (in Windows)
\/:*?"<>|
In Linux a double quote can be part of the filename

