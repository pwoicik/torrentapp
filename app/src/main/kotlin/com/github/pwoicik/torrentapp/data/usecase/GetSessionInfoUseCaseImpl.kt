package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.data.SessionInfoRepository
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import me.tatarka.inject.annotations.Inject

@Inject
class GetSessionInfoUseCaseImpl(
    private val repo: SessionInfoRepository,
) : GetSessionInfoUseCase {
    override fun invoke(input: Unit) = repo.getInfo()
}
