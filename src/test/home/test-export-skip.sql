
--* // Copyright 2015 René M. de Bloois

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

CREATE TABLE TEMP1 ( FIELD1 INTEGER, FIELD2 INTEGER, FIELD3 INTEGER, FIELD4 INTEGER, FIELD5 INTEGER );

insert into temp1 ( field1, field2, field3, field4, field5 ) values ( 1, 2, 3, 4, 5 );

EXPORT CSV
FILE "tmp/export-skip1.csv" ENCODING "ISO-8859-1"
COLUMN FIELD2, field4 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT CSV
COALESCE field2, FiElD4
FILE "tmp/export-skip2.csv" ENCODING "ISO-8859-1"
COLUMN FIELD5 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT JSON
FILE "tmp/export-skip1.json"
COLUMN FIELD2, field4 SKIP
column field5 TO TEXT FILE "tmp/export-skip1-?4.txt"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT JSON
COALESCE field2, FiElD4
FILE "tmp/export-skip2.json"
COLUMN FIELD5 SKIP
column field2 TO TEXT FILE "tmp/export-skip2-?4.txt"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT CBOR
FILE "tmp/export-skip1.cbor"
COLUMN FIELD2, field4 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT CBOR
COALESCE field2, FiElD4
FILE "tmp/export-skip2.cbor"
COLUMN FIELD5 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

delete from temp1;
insert into temp1 ( field1, field2, field3, field4, field5 ) values ( 1, null, 3, 4, 5 );

EXPORT CSV
COALESCE field2, FiElD4
FILE "tmp/export-skip3.csv" ENCODING "ISO-8859-1"
COLUMN FIELD2 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT JSON
COALESCE field2, FiElD4
FILE "tmp/export-skip3.json"
COLUMN FIELD2 SKIP
column field5 TO TEXT FILE "tmp/export-skip3-?2.txt"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT CBOR
COALESCE field2, FiElD4
FILE "tmp/export-skip3.cbor"
COLUMN FIELD2 SKIP
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

