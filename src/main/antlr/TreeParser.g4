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
parser grammar TreeParser;
options { tokenVocab=TreeLexer; }

file
    : (definition | importSt)* EOF
    ;

import_name
    : id (EQ_A id)?
    ;

importCalls
    : LBC (import_name (COMMA import_name)* COMMA?)? RBC
    ;

importSt
    : IMPORT string importCalls?
    ;

definition
    : tree_type id params? (calls? | SEMI)
    ;

call
    : invocation
    | lambda
    ;

invocation
    : id (args | LPR DOT_DOT RPR)
    ;


lambda
    : tree_type args? calls
    ;

calls
    : LBC call* RBC
    | call
    ;


arg
    : id (EQ (message | id | call))?
    | message
    | call
    ;

args
    : LPR (arg (COMMA arg)* COMMA?)? RPR
    ;

params
    : LPR (param (COMMA param)*)? COMMA? RPR
    ;

param
    : id COLON mes_type
    ;

message
    : string
    | num
    | bool
    | array
    | object
    ;

mes_type
    : NUM_T
    | ARRAY_T
    | OBJECT_T
    | STRING_T
    | BOOL_T
    | TREE_T
    | ANY_T
    ;

tree_type
    : static_type
    | id          // ambigulty
    ;


static_type
    : ROOT
    | PARALLEL
    | SEQUENCE
    | MSEQUENCE
    | RSEQUENCE
    | FALLBACK
    | RFALLBACK
    ;


object
    : LBC (objectPair (COMMA objectPair)* COMMA? )? RBC
    ;

objectPair
    : string COLON message
    ;


array
    : LBR (message (COMMA message)* COMMA? )? RBR
    ;

bool
    : TRUE
    | FALSE
    ;

num
    : NUMBER
    ;

string
    : STRING
    ;
id
    : ID
    ;