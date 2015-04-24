
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


--*	DEFINITION
--*		SETUP "" --> "1.1"
--*		UPGRADE "" --> "1"
--*	END DEFINITION


--* SETUP "" --> "1.1"
RUN "setup-1.1.sql";
--* END SETUP


--* UPGRADE "" --> "1"
CREATE TABLE TEST ( COL1 VARCHAR( 10 ) );
INSERT INTO TEST VALUES ( '${par1}' );
PRINT SELECT COL1 FROM TEST;
--* END UPGRADE
