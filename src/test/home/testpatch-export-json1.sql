
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

 EXPORT JSON
	FILE "output/export11.json" FROM
SELECT * FROM TEMP1;

EXPORT JSON
	FILE "output/export12.json" FROM
SELECT ID, PICTURE, TEXT, TEXT2
FROM TEMP1;

 EXPORT JSON
	DATE AS TIMESTAMP
	FILE "output/export13.json"
	COLUMN PICTURE TO BINARY FILE "output/folder/export13.bin"
	COLUMN TEXT2 TO TEXT FILE "output/folder/export13.txt"
FROM
SELECT * FROM TEMP1;

--* EXPORT JSON SET ADD_CREATED_DATE = OFF

		EXPORT JSON
	FILE "output/export14.json"
	FROM
SELECT * FROM TEMP1;

--* EXPORT JSON SET ADD_CREATED_DATE = ON

EXPORT JSON
FILE "output/folder/export15.json"
COLUMN PICTURE TO BINARY FILE "output/folder/export15-blob-?1.txt"
COLUMN TEXT2 TO TEXT FILE "output/folder/export15-text-?1.txt" THRESHOLD 100
COLUMN DATE1 SKIP
FROM
SELECT ID, PICTURE, TEXT, TEXT2, DATE1
FROM TEMP1;

--* /UPGRADE

These cannot be part of a filename (in Windows)
\/:*?"<>|
In Linux a double quote can be part of the filename

