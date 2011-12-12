
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
CREATE TABLE TEMP1 ( ID INTEGER, PICTURE BLOB, TEXT VARCHAR(100) );
--* /UPGRADE

--* UPGRADE "1" --> "2"

EXPORT CSV
FILE "export1.csv" ENCODING "UTF-8"
BINARY FILE "export1.bin"
SELECT * FROM TEMP1;

EXPORT CSV
FILE "export2.csv" ENCODING "UTF-8"
BINARY FILE "folder/export2-blob-?1.txt"
SELECT * FROM TEMP1;

--* /UPGRADE

These cannot be part of a filename (in Windows)
\/:*?"<>|
In Linux a double quote can be part of the filename

