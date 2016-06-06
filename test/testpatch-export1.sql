
--* // Copyright 2011 Ren� M. de Bloois

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

 EXPORT CSV
	WITH HEADER
	FILE "export1.csv" ENCODING "UTF-8"
SELECT * FROM TEMP1;

EXPORT CSV
	FILE "export2.csv" ENCODING "UTF-8"
SELECT ID, PICTURE, TEXT, TEXT2
FROM TEMP1;

 DUMP JSON
	DATE AS TIMESTAMP
	FILE "export3.json"
	COLUMN PICTURE TO BINARY FILE "folder/export3.bin"
	COLUMN TEXT2 TO TEXT FILE "folder/export3.txt"
SELECT * FROM TEMP1;

--* DUMP CBOR DATE_CREATED OFF

		DUMP CBOR
	FILE "export3.cbor"
SELECT * FROM TEMP1;

--* DUMP JSON DATE_CREATED OFF

DUMP JSON
FILE "folder/export4.json"
COLUMN PICTURE TO BINARY FILE "folder/export4-blob-?1.txt"
COLUMN TEXT2 TO TEXT FILE "folder/export4-text-?1.txt" THRESHOLD 100
COLUMN DATE1 SKIP
SELECT ID, PICTURE, TEXT, TEXT2, DATE1
FROM TEMP1;

--* /UPGRADE

These cannot be part of a filename (in Windows)
\/:*?"<>|
In Linux a double quote can be part of the filename

