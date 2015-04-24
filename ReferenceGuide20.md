# Contents #
## Annotations ##
  * [INCLUDE](#INCLUDE.md) (upgrade only)
## Commands ##
  * [EXPORT CSV](#EXPORT_CSV.md)
  * [IMPORT CSV](#IMPORT_CSV.md) (enhancements)
  * [RUN](#RUN.md)


---

## EXPORT CSV ##
Exports the results of a query to a CSV file.
### Since ###
2.0.0-beta3
### Syntax ###
```
EXPORT CSV
[ WITH HEADER ]
[ SEPARATED BY (TAB|SPACE|<character>) ]
[ DATE AS TIMESTAMP ]
[ COALESCE <field1>, <field2> [ , <fieldn> ] ] *
FILE "<filename>" ENCODING "<encoding>"
<query> <current delimiter>
```
The supported encodings vary between different implementations of the Java 2 platform, but every implementation is required to support US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE and UTF-16. See http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html.

### Examples ###
```
EXPORT CSV WITH HEADER SEPARATED BY ;
FILE "export1.csv" ENCODING "UTF-8"
SELECT * FROM TEMP1;
```


---

## IMPORT CSV ##
This command has been enhanced. It is now possible to execute any legal SQL statement for the rows found in the CSV data. Furthermore, a SPACE is added as a possible separator.
### Since ###
2.0.0-beta3
### Syntax ###
#### Syntax 4 ####
CSV data comes after the import statement and is read directly from the source file.
```
IMPORT CSV
[ SKIP HEADER ]
[ SEPARATED BY (TAB|SPACE|<character>) ]
[ IGNORE WHITESPACE ]
[ NOBATCH ]
EXECUTE <sql> <current delimiter>
... csv data ...
<empty line>
```
#### Syntax 5 ####
CSV data is in another file.
```
IMPORT CSV
[ SKIP HEADER ]
[ SEPARATED BY (TAB|SPACE|<character>) ]
[ IGNORE WHITESPACE ]
[ NOBATCH ]
FILE "<filename>" ENCODING "<encoding>"
EXECUTE <sql> <current delimiter>
```

### Examples ###
```
--* // MERGE
IMPORT CSV
EXECUTE MERGE INTO ATABLE
USING ( VALUES ( CAST( :1 AS INTEGER ), CAST( :2 AS VARCHAR(40) ) ) ) AS VALS( ID, DESC )
ON ATABLE.ID = VALS.ID
WHEN MATCHED THEN UPDATE SET ATABLE.DESC = VALS.DESC
WHEN NOT MATCHED THEN INSERT VALUES VALS.ID, VALS.DESC;
"2","The second record"
"3","The third record"

--* // Delete from a file containing ids of records
IMPORT CSV FILE "ids.csv" ENCODING "UTF-8"
EXECUTE DELETE FROM ATABLE WHERE ID = :1;
```


---

## INCLUDE ##
Includes an external file into the upgrade. The statements in the included file are considered part of the including file.
### Since ###
2.0.0-beta3
### Syntax ###
```
--* INCLUDE "<file>"
```

### Examples ###
```
--* INCLUDE "procedures.sql"
```


---

## RUN ##
Runs an external SQL file.
### Since ###
2.0.0-beta3
### Syntax ###
```
RUN "<file>" <current delimiter>
```

### Examples ###
```
RUN "compile.sql";
```