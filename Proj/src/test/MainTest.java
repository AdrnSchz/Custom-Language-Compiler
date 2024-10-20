package test;

import org.junit.jupiter.api.Test;

import java.io.*;
import static main.Main.compileZZZ;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private String readFile(String file) {
        StringBuilder builder = new StringBuilder();
        String expectedOutput;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            reader.close();
            expectedOutput = builder.toString();

        } catch (IOException ignored) {
            expectedOutput = "";
        }
        return expectedOutput;
    }

    @Test
    void fibonacci() {
        compileZZZ("fibonacci.txt", "a.out");
        assertEquals(readFile("src/test/expected/fibonacci.txt"), readFile("a.out"));
    }

    @Test
    void area() {
        compileZZZ("area.txt", "a.out");
        assertEquals(readFile("src/test/expected/area.txt"), readFile("a.out"));
    }

    @Test
    void fibNoRec() {
        compileZZZ("fibNoRec.txt", "a.out");
        assertEquals(readFile("src/test/expected/fibNoRec.txt"), readFile("a.out"));
    }


    @Test
    void functionCall() {
        compileZZZ("functionCall.txt", "a.out");
        assertEquals(readFile("src/test/expected/functionCall.txt"), readFile("a.out"));
    }

    @Test
    void operationsFile() {
        compileZZZ("operations.txt", "a.out");
        assertEquals(readFile("src/test/expected/operations.txt"), readFile("a.out"));
    }

    @Test
    void loopsFile() {
        compileZZZ("loops.txt", "a.out");
        assertEquals(readFile("src/test/expected/loops.txt"), readFile("a.out"));
    }

    @Test
    void conditionalsFile() {
        compileZZZ("conditionals.txt", "a.out");
        assertEquals(readFile("src/test/expected/conditionals.txt"), readFile("a.out"));
    }

    @Test
    void semantics() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        compileZZZ("semanticErrors.txt", "a.out");

        System.setOut(originalOut);

        assertEquals(readFile("src/test/expected/semanticErrors.txt"), baos.toString().replace("\r\n", "\n"));
    }

    @Test
    void errorRecovery() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        compileZZZ("errorRecovery.txt", "a.out");

        System.setOut(originalOut);

        assertEquals(readFile("src/test/expected/errorRecovery.txt"), baos.toString().replace("\r\n", "\n"));
    }

}