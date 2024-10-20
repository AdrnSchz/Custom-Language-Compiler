package main;

import errors.ErrorHandler;
import stages.PreCompiler;
import stages.frontend.SyntaxAnalyser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: zzz file.z");
            return;
        }

        if (args[1] == null) {
            System.out.println("Error during pre-compilation");
            return;
        }

        String outputFile;
        if (args.length == 3) outputFile = args[2];
        else outputFile = "a.out";

        compileZZZ(args[1], outputFile);
    }

    public static void compileZZZ(String inputFileName, String outputFileName) {

        String inputFile = "src/test/src/"+inputFileName;

        StringBuilder srcCode = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                srcCode.append(line).append('\n');
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("----------------------- Bombastic side eye to your file ¬_¬ -----------------------");
            System.exit(1);
        }
        PreCompiler preCompiler = new PreCompiler();
        String cleanCode = preCompiler.commentRemover(srcCode.toString());
        ErrorHandler errorHandler = new ErrorHandler();
        SyntaxAnalyser syntaxAnalyser = new SyntaxAnalyser(cleanCode, errorHandler, outputFileName);

        System.out.println(errorHandler.getErrorWall());
    }
}