package io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

public class IOTasks {

    /**
     * Liefert die ersten (bis zu) 'n' nicht-leeren Zeilen im Text zurück,
     * den der gegebene InputStream liefert. Verwenden Sie UTF-8, um den
     * Text zu decodieren, und achten Sie darauf, den InputStream am
     * Ende zu schliessen.
     */
    public static List<String> firstNonEmptyLines(InputStream in, int n) throws IOException {
        var result = new ArrayList<String>();
        try (var reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && result.size() < n) {
                if (!line.isEmpty()) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    /**
     * Berechnet die ersten 'n' Zweierpotenzen (1, 2, 4, 8...) und
     * schreibt sie als Text in den gegebenen OutputStream, jede auf
     * eine eigene Zeile. Verwenden Sie UTF-8, um den Text zu codieren,
     * und achten Sie darauf, den OutputStream am Ende zu schliessen.
     */
    public static void writePowersOfTwo(OutputStream out, int n) throws IOException {
        try (var writer = new BufferedWriter(new OutputStreamWriter(out, UTF_8))) {
            int p = 1;
            for (int i = 1; i <= n; i++) {
                writer.write(p + "\n");
                p *= 2;
            }
        }
    }

    /**
     * Durchsucht den Text, den der gegebene InputStream liefert, nach
     * Zahlen (ganze oder reelle) und gibt alle in einer Liste zurück.
     * Sie können davon ausgehen, dass sämtliche Zahlen durch Whitespace
     * von anderen Textstücken getrennt sind. Verwenden Sie UTF-8, um
     * den Text zu decodieren, und achten Sie darauf, den InputStream
     * am Ende zu schliessen.
     * <p>
     * Tipp: Die Klasse java.util.Scanner kann Text direkt in einzelne,
     * durch Whitespace getrennte "Tokens" aufteilen und testen, ob ein
     * Token als Double interpretiert werden kann.
     */
    public static List<Double> extractNumbers(InputStream in) throws IOException {
        try (var scanner = new Scanner(in, UTF_8)) {
            var numbers = new ArrayList<Double>();
            while (scanner.hasNext()) {
                if (scanner.hasNextDouble()) {
                    numbers.add(scanner.nextDouble());
                } else {
                    scanner.next(); // skip token
                }
            }
            return numbers;
        }
    }

    /**
     * Parst den Text, den der gegebene InputStream liefert, als CSV
     * und wandelt jede Zeile (ausser der Header-Zeile) in ein Person-
     * Objekt um. Der Text hat folgendes Format:
     * <pre>
     * Name;Age;Positive
     * Maria Mopp;46;1
     * Boris Bopp;23;0
     * Sarah Sutter;39;0
     * Kuno Koriander;78;1
     * Lisa Laufener;17;1
     * </pre>
     * Verwenden Sie UTF-8, um den Text zu decodieren, und achten Sie
     * darauf, den InputStream am Ende zu schliessen.
     */
    public static List<Person> readPeopleFromCsv(InputStream in) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            var people = new ArrayList<Person>();
            var lines = reader.lines().skip(1).toList();
            for (var line : lines) {
                var parts = line.split(";");
                var name = parts[0];
                var age = Integer.parseInt(parts[1]);
                var positive = parts[2].equals("1");
                people.add(new Person(name, age, positive));
            }
            return people;
        }
    }
}
