package org.dataportabilityproject.shared;

import java.io.IOException;
import java.util.List;

/**
 * Interface allowing services to display information, or ask questions to users
 */
public interface IOInterface {
    void print(String text);

    String ask(String prompt) throws IOException;

    /**
     * Asks the user a multiple choice question.
     */
    <T> T ask(String prompt, List<T> choices) throws IOException;
}
