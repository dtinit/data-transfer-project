package org.dataportabilityproject.transfer.microsoft.transformer;

import java.util.List;

/**
 * The result of a transformation operation.
 */
public class TransformResult<T> {
    private List<String> problems;
    private T transformed;

    public TransformResult(T transformed, List<String> problems) {
        this.problems = problems;
        this.transformed = transformed;
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    public List<String> getProblems() {
        return problems;
    }

    public T getTransformed() {
        return transformed;
    }
}
