
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



CREATE TABLE DBPARAMETERS ( KEY VARCHAR( 10 ), VALUE VARCHAR( 100 ) );
INSERT INTO DBPARAMETERS VALUES ( 'TABLENAME', 'TESTTEST' );

--* // No record found means NULL
--* SET VARIABLE TABLENAME = SELECT VALUE FROM DBPARAMETERS WHERE KEY = 'TABLENAME'

--* IF VARIABLE TABLENAME IS NOT NULL

--* SECTION "Creating table &tablename"
CREATE TABLE &TableName ( TEST VARCHAR( 10 ) );
INSERT INTO &{TABLENAME} VALUES ( 'TEST' );
COMMIT;
--* /IF

--* // Is a problem during upgrade. Is it transient or not? It is both, so INCLUDE is better.
--* // Best during SQL execution
--* // During an upgrade it is counted as 1 statement?
--* // CALL "OTHERSQL.SQL"
 RUN "TESTSQL1.SQL";
--* // EXECUTE "OTHERSQL.SQL"

--* // Best for upgrade
--* // INCLUDE "OTHERSQL.SQL"

--* // The seems to be like: import but do nothing
--* // IMPORT "OTHERSQL.SQL"

--* IF VARIABLE TABLENAME IS NULL

CREATE TABLE &TableName ( TEST VARCHAR( 10 ) );

--* ELSE

--* SECTION "Creating table &{TableName}TEST"
CREATE TABLE &{TableName}TEST ( TEST VARCHAR( 10 ) );

--* /IF
