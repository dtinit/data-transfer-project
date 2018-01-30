package org.dataportabilityproject.transfer.microsoft.transformer;

/**
 * Transforms instances into different types.
 */
public interface TransformerService {

    /**
     * Transform the input instance into an instance of a result type.
     *
     * @param resultType the type to transform to
     * @param input the input instance
     */
    <T> TransformResult<T> transform(Class<T> resultType, Object input);

}
