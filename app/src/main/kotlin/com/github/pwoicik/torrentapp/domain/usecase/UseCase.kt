package com.github.pwoicik.torrentapp.domain.usecase

interface UseCase<In, Out> {
    operator fun invoke(input: In): Out
}

interface SuspendUseCase<In, Out> {
    suspend operator fun invoke(input: In): Out
}

operator fun <Out> UseCase<Unit, Out>.invoke() = invoke(Unit)
suspend operator fun <Out> SuspendUseCase<Unit, Out>.invoke() = invoke(Unit)
