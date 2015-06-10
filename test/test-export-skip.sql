
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
( 1, 2, 3, 4, 5 );

DUMP JSON
FILE "export-skip1.json"
COLUMN FIELD2, field4 SKIP
column field5 TO TEXT FILE "export-skip1-?4.txt" 
SELECT field1, field2, field3, field4, field5 FROM TEMP1;
