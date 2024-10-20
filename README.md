# Z Programming Language Compiler
Welcome to the Z Programming Language Compiler project! This project involves building a compiler for the Z programming language, a fun and modern language designed for Generation Z. The Z language features simple syntax and unique semantics to make coding easy, accessible, and engaging for users new to programming.

## Overview
This compiler was developed with two major components in mind:

Front-end – Responsible for analyzing the Z source code and validating its structure through various stages including lexical, syntax, and semantic analysis.

Back-end – Focused on generating intermediate and target machine code from the valid Z source code using Three Address Code (TAC) and MIPS assembly.

## Modules
The compiler consists of several key modules:

Pre-Compiler: Prepares the Z source code by expanding macros and removing unnecessary elements like comments and tabs.

Lexical Analyzer: Converts the Z source code into a sequence of tokens using custom tokenization rules.

Syntax Analyzer: Checks if the token sequence follows the grammatical rules of Z and generates a parse tree.

Semantic Analyzer: Verifies the correctness of the parse tree and ensures the source code adheres to the semantic rules of Z.

Intermediate Code Generation: Produces a high-level Three Address Code (TAC) representation.

Target Code Generation: Translates TAC into MIPS machine code.

## Getting Started
To compile a Z source code file:

1. Run the compiler on the code to generate MIPS assembly.

2. If there are, solve the errors so the source code is correctly formatted according to the Z language specifications.

3. Load the assembly code into a MIPS emulator to execute the program.

## Grammar and Language Specifications
Z uses a modern and simplified syntax inspired by Gen Z culture, including:

Conditional Statements: Use "like" for if, "whatever" for else.

Loops: "vibe check" for while, "4" for for.

Datatypes: "bro" for integers, "sis" for floats, and "bipolar" for booleans.

Refer to the report for a detailed breakdown of the grammar, syntax, and semantics.

## Conclusion
This compiler project showcases the process of turning high-level source code into executable machine code. By following the modular design of compilers, this project is an exploration of how modern programming languages can be built and implemented.

For further information on the detailed implementation of the compiler, please refer to the project report.
