
--* // Copyright 2010 René M. de Bloois

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
--*	/DEFINITION

--* SETUP "" --> "1.1"
RUN "setup-1.1.sql";
--* /SETUP

--* UPGRADE "" --> "1"
CREATE TABLE TEMP ( TEMP1 VARCHAR(40) NOT NULL, TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );

IMPORT CBOR INTO TEMP;
9F
	83 01 02 03
	83 04 05 06
	83 07 08 09
FF

INSERT INTO TEMP SELECT * FROM TEMP; -- 6
INSERT INTO TEMP SELECT * FROM TEMP; -- 12
INSERT INTO TEMP SELECT * FROM TEMP; -- 24
INSERT INTO TEMP SELECT * FROM TEMP; -- 48
INSERT INTO TEMP SELECT * FROM TEMP; -- 96
--INSERT INTO TEMP SELECT * FROM TEMP; -- 192;

EXPORT CBOR
log every 10 records
FILE "export3.cbor"
FROM
SELECT * FROM TEMP;

EXPORT CBOR
LOG EVERY 1 SECONDS
FILE "export3.cbor"
FROM
SELECT * FROM TEMP;

IMPORT CBOR
LOG EVERY 10 RECORDS
INTO TEMP
FILE "export3.cbor";

IMPORT CBOR
LOG EVERY 1 SECONDS
INTO TEMP
FILE "export3.cbor";

--* /UPGRADE
