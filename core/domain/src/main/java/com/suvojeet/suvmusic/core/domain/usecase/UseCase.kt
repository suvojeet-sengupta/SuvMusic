package com.suvojeet.suvmusic.core.domain.usecase

/**
 * Base interface for all use cases.
 * Use cases should implement this interface to define their input and output types.
 */
interface UseCase<in Input, out Output> {
    suspend operator fun invoke(input: Input): Output
}

/**
 * Base interface for use cases that don't require input parameters.
 */
interface ParameterlessUseCase<out Output> {
    suspend operator fun invoke(): Output
}

/**
 * Base interface for use cases that don't return a value.
 */
interface ConsumerUseCase<in Input> {
    suspend operator fun invoke(input: Input)
}

/**
 * Base interface for use cases that neither take input nor return output.
 */
interface ActionUseCase {
    suspend operator fun invoke()
}
