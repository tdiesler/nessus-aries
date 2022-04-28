package io.nessus.aries.util;

/**
 * A Consumer that allows to throw exceptions
 */
@FunctionalInterface
public interface SafeConsumer<T> {
    
    void accept(T item) throws Exception;
}