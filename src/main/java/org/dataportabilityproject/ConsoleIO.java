package org.dataportabilityproject;

import org.dataportabilityproject.shared.IOInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * An {@link IOInterface} that interacts with the user via the console.
 */
public class ConsoleIO implements IOInterface {
    public void print(String text) {
        System.out.println(text);
    }

    public String ask(String prompt) throws IOException {
        System.out.println(prompt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        return reader.readLine();
    }

    /**
     * Asks the user a multiple choice question.
     */
    public <T> T ask(String prompt, List<T> choices) throws IOException {
        System.out.println(prompt + " (enter the number of your choice):");
        for (int i=0; i < choices.size(); i++) {
            System.out.println("\t" + i + ") " + choices.get(i));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        int userPick = Integer.parseInt(reader.readLine());
        return choices.get(userPick);
    }
}
