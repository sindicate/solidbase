
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

insert into temp1 ( field1, field2, field3, field4, field5 ) values
( 1, 2, 3, 4, 5 ),
( NULL, 12, 13, 14, 15 ),
( NULL, NULL, 23, 24, 25 );

EXPORT CSV WITH HEADER
COALESCE FIELD1, FIELD2, FIELD3
COALESCE FIELD2, FIELD5
FILE "export-coalesce1.csv" ENCODING "UTF-8"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT JSON
COALESCE FIELD1, FIELD2, FIELD3
COALESCE FIELD2, FIELD5
FILE "export-coalesce1.json"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;

EXPORT CBOR
COALESCE FIELD1, FIELD2, FIELD3
COALESCE FIELD2, FIELD5
FILE "export-coalesce1.cbor"
FROM
SELECT field1, field2, field3, field4, field5 FROM TEMP1;
