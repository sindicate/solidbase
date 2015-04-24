
--* // Copyright 2012 René M. de Bloois

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

--*  SCRIPT tableName = "TESTTEST"

--*  IF SCRIPT tableName

--* SECTION "Creating table ${tableName}"
CREATE TABLE ${tableName} ( TEST VARCHAR( 10 ) );
INSERT INTO ${tableName} VALUES ( 'TEST' );
COMMIT;

--* /IF

CREATE TABLE DBPARAMETERS ( KEY VARCHAR( 10 ), VALUE VARCHAR( 100 ) );
INSERT INTO DBPARAMETERS VALUES ( 'TABLENAME', '${tableName}' );

--* SCRIPT tableName = db.selectFirst( "SELECT VALUE FROM DBPARAMETERS WHERE KEY = 'TABLENAME'" )

--* IF SCRIPT !tableName
--* ELSE

--* SECTION "Creating table ${tableName}TEST"
CREATE TABLE ${tableName}TEST ( TEST VARCHAR( 100 ) );

--* SCRIPT ( key, value ) = db.selectFirst( "SELECT KEY, VALUE FROM DBPARAMETERS WHERE KEY = 'TABLENAME'" )

--* SECTION "Found ${key} = ${value}"
INSERT INTO ${tableName}TEST ( TEST ) VALUES ( '${key} = ${value}' );

PRINT SELECT * FROM ${tableName}TEST;

--* END IF

--* // This should not give an exception 
--* IF SCRIPT undefinedVariable

--* END IF
