package org.dataportabilityproject.transfer.microsoft.transformer;

import java.util.List;

/**
 * Provides facilities for performing recursive transformations and collated error reporting.
 */
public interface TransformerContext {

    /**
     * Transform the input instance into an instance of a result type.
     * <p>
     * This method supports null input values; if the input is null, the same will be returned.
     *
     * @param resultType the type to transform to
     * @param input the input instance
     */
    <T> T transform(Class<T> resultType, Object input, TransformerContext context);

    /**
     * Adds a problem to the current processing context.
     *
     * @param message the problem
     */
    void problem(String message);

    /**
     * Returns problems encountered during transformation.
     */
    List<String> getProblems();


}
