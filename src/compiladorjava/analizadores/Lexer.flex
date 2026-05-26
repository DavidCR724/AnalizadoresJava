/* --------------------------
   Sección 1: Código de Usuario
   -------------------------- */
package compiladorjava.analizadores; 
import java_cup.runtime.*;

/* --------------------------
   Sección 2: Opciones y Declaraciones
   -------------------------- */
%%
%class Lexer
%unicode
%cup
%line
%column
%public

%xstate COMENTARIO_MULTILINEA

%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }
    
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }
%}

/* --------------------------
   Sección 3: Macros
   -------------------------- */
Letra = [a-zA-Z_]
Digito = [0-9]
Identificador = {Letra}({Letra}|{Digito})*
Entero = {Digito}+
Flotante = {Digito}+"."{Digito}+
Espacio = [ \t\r\n\f]
ComentarioLinea = "//"[^\r\n]*
StringLiteral = \"[^\"]*\"

/* --------------------------
   Sección 4: Reglas Léxicas
   -------------------------- */
%%

<YYINITIAL> {
    
    /* Modificadores y Palabras Reservadas Principales */
    "public"       { return symbol(sym.PUBLIC); }
    "private"      { return symbol(sym.PRIVATE); }
    "protected"    { return symbol(sym.PROTECTED); }
    "static"       { return symbol(sym.STATIC); }
    "class"        { return symbol(sym.CLASS); }
    "if"           { return symbol(sym.IF); }
    "else"         { return symbol(sym.ELSE); }
    "while"        { return symbol(sym.WHILE); }
    "do"           { return symbol(sym.DO); }
    "for"          { return symbol(sym.FOR); }
    "return"       { return symbol(sym.RETURN); }

    /* Palabras para consola (Impresión) */
    "System"       { return symbol(sym.SYSTEM); }
    "out"          { return symbol(sym.OUT); }
    "println"      { return symbol(sym.PRINTLN); }
    "print"        { return symbol(sym.PRINT); }

    /* Tipos de Datos */
    "int"          { return symbol(sym.INT); }
    "boolean"      { return symbol(sym.BOOLEAN); }
    "void"         { return symbol(sym.VOID); }
    "byte"         { return symbol(sym.BYTE); }
    "short"        { return symbol(sym.SHORT); }
    "long"         { return symbol(sym.LONG); }
    "float"        { return symbol(sym.FLOAT); }
    "double"       { return symbol(sym.DOUBLE); }
    "char"         { return symbol(sym.CHAR); }
    "String"       { return symbol(sym.STRING_TYPE); }

    /* Booleanos */
    "true"         { return symbol(sym.TRUE, Boolean.TRUE); }
    "false"        { return symbol(sym.FALSE, Boolean.FALSE); }

    /* Operadores Aritméticos */
    "+"            { return symbol(sym.SUMA); }
    "-"            { return symbol(sym.RESTA); }
    "*"            { return symbol(sym.MULTIPLICACION); }
    "/"            { return symbol(sym.DIVISION); }
    "++"           { return symbol(sym.INCREMENTO); }
    "--"           { return symbol(sym.DECREMENTO); }

    /* Operadores Relacionales y Asignación */
    "="            { return symbol(sym.ASIGNACION); }
    "=="           { return symbol(sym.IGUAL_QUE); }
    "<"            { return symbol(sym.MENOR_QUE); }
    ">"            { return symbol(sym.MAYOR_QUE); }
    ">="           { return symbol(sym.MAYOR_IGUAL); }
    "<="           { return symbol(sym.MENOR_IGUAL); }
    "!="           { return symbol(sym.DIFERENTE); }

    /* Operadores Lógicos */
    "&&"           { return symbol(sym.AND); }
    "||"           { return symbol(sym.OR); }
    "!"            { return symbol(sym.NOT); }

    /* Delimitadores */
    ";"            { return symbol(sym.PUNTO_COMA); }
    ":"            { return symbol(sym.DOS_PUNTOS); }
    ","            { return symbol(sym.COMA); }
    "."            { return symbol(sym.PUNTO); }
    "{"            { return symbol(sym.LLAVE_A); }
    "}"            { return symbol(sym.LLAVE_C); }
    "("            { return symbol(sym.PARENTESIS_A); }
    ")"            { return symbol(sym.PARENTESIS_C); }

    /* Nuevo Literal para Char */
    \'[^\']\'      { return symbol(sym.LITERAL_CHAR, yytext()); }
    
    /* Literales e Identificadores */
    {Entero}         { return symbol(sym.LITERAL_ENTERO, new Integer(yytext())); }
    {Flotante}       { return symbol(sym.LITERAL_FLOTANTE, new Double(yytext())); }
    {StringLiteral}  { return symbol(sym.LITERAL_STRING, yytext()); }
    
    /* Nota: Identificador debe ir al final para no hacer match con palabras reservadas */
    {Identificador}  { return symbol(sym.ID, yytext()); }

    {Espacio}+           { /* Ignorar */ }
    {ComentarioLinea}    { /* Ignorar */ }

    "/*"                 { yybegin(COMENTARIO_MULTILINEA); }
    [^]                  { System.err.println("Error Léxico: Carácter ilegal <"+yytext()+"> en línea "+(yyline+1)+", columna "+(yycolumn+1)); }
}

<COMENTARIO_MULTILINEA> {
    "*/"                 { yybegin(YYINITIAL); } 
    [^]                  { /* Ignorar */ }
}