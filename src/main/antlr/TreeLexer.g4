/*
 [The "BSD licence"]
 Copyright (c) 2023 Boris Zhguchev
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
lexer grammar TreeLexer;

ROOT: 'root';
PARALLEL : 'parallel';

SEQUENCE : 'sequence';
MSEQUENCE : 'm_sequence';
RSEQUENCE : 'r_sequence';

FALLBACK: 'fallback';
RFALLBACK : 'r_fallback';

ARRAY_T: 'array';
NUM_T: 'num';
OBJECT_T: 'object';
STRING_T: 'string';
BOOL_T: 'bool';
TREE_T: 'tree';
ANY_T: 'any';
IMPORT: 'import';

ID : [-_a-zA-Z]+ (INT | [-_a-zA-Z]+)*  ;

COMMA : ',';
COLON : ':';
SEMI : ';';
DOT_DOT : '..';

EQ  : '=';
EQ_A  : '=>';

LPR  : '(';
RPR  : ')';

LBC  : '{';
RBC  : '}';

LBR  : '[';
RBR  : ']';

TRUE : 'TRUE';

FALSE : 'FALSE';

STRING  : '"' (ESC | SAFECODEPOINT)* '"' ;

NUMBER  : '-'? INT ('.' [0-9] +)? EXP? ;

Whitespace: [ \t\r\n\u000C]+ -> channel(HIDDEN);

BlockComment :   '/*' .*? '*/' -> channel(2) ;

LineComment :   '//' ~[\r\n]* -> channel(2) ;

fragment ESC : '\\' (["\\/bfnrt] | UNICODE) ;

fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

fragment SAFECODEPOINT : ~ ["\\\u0000-\u001F] ;
fragment INT : '0' | [1-9] [0-9]* ;
fragment EXP : [Ee] [+\-]? [0-9]+ ;

ERRCHAR
	:	.	-> channel(HIDDEN)
	;