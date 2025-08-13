package com.example.myapplication.domain.usecase

/**
 * Base interface for all use cases in the application.
 * Use cases represent the business logic of the application and are part of the domain layer.
 * They act as intermediaries between the presentation layer (ViewModels) and the data layer (Repositories).
 *
 * @param P The type of the input parameters
 * @param R The type of the result
 */
interface UseCase<in P, out R> {
    /**
     * Executes the use case with the given parameters.
     *
     * @param params The input parameters for the use case
     * @return The result of the use case execution
     */
    suspend operator fun invoke(params: P): R
}

/**
 * Base interface for use cases that don't require input parameters.
 *
 * @param R The type of the result
 */
interface NoParamsUseCase<out R> {
    /**
     * Executes the use case without parameters.
     *
     * @return The result of the use case execution
     */
    suspend operator fun invoke(): R
}

/**
 * Base interface for use cases that don't return a result.
 *
 * @param P The type of the input parameters
 */
interface CompletableUseCase<in P> {
    /**
     * Executes the use case with the given parameters without returning a result.
     *
     * @param params The input parameters for the use case
     */
    suspend operator fun invoke(params: P)
}

/**
 * Base interface for use cases that neither require input parameters nor return a result.
 */
interface NoParamsCompletableUseCase {
    /**
     * Executes the use case without parameters and without returning a result.
     */
    suspend operator fun invoke()
}