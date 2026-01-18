package dev.iris.parser.lexer

// Idea ругается на кириллицу в коде
enum class TokenKind {
    IDENT, INT_LITERAL,

    KW_INT, KW_BOOL,    // целч бул
    KW_IF, KW_ELSE,     // если иначе
    KW_WHILE,           // пока
    KW_RETURN,          // вернуть
    KW_CONTINUE,        // продолжить
    KW_BREAK,           // выйти
    KW_PROCEDURE,       // процедура
    KW_FUNCTION,        // функция
    KW_STRUCTURE,       // структура
    KW_NEW,             // новый
    KW_PRINT,           // печать
    KW_START, KW_END,   // нч кц
    KW_TRUE, KW_FALSE,  // правда ложь
    KW_AND, KW_OR,      // и или
    KW_NULL,            // нуль

    PLUS, MINUS, STAR, SLASH, PERCENT,  // + - * / %
    BANG, QUESTION,                     // ! ?
    EQ, EQEQ, NEQ,                      // = == !=
    LT, GT, GTEQ, LTEQ,                 // < > <= >=
    DOT, COMMA, COLON, SEMICOLON,       // . , : ;
    LPAREN, RPAREN,                     // ( )

    ERROR,              // (для ошибок)
    EOF,
}