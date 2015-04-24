grammar solidbaseimport;

imp	:	'IMPORT' WS+ 'CSV' separated? prepend? using? WS+ 'INTO' WS+ table columns? WS+ 'DATA' WS* NEWLINE data;

separated:	WS+ 'SEPARATED' WS+ 'BY' WS+ ( 'TAB' | NONWS );
prepend	:	WS+ 'PREPEND' WS+ 'LINENUMBER';
using	:	WS+ 'USING' WS+ ( 'PLBLOCK' | 'VALUESLIST' );
columns	:	WS* '(' WS* column ( WS* ',' WS* column )* WS* ')';

column	:	NONWS+;
table	:	NONWS+;
WS	:	' ' | '\t' | '\n' | '\r';
NONWS	:	~WS;
NEWLINE	:	'\r\n' | '\r' | '\n';
data	:	.*;
