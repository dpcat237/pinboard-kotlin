package com.fibelatti.pinboard.features.posts.presentation

import com.fibelatti.core.archcomponents.test.extension.currentEventShouldBe
import com.fibelatti.core.archcomponents.test.extension.currentValueShouldBe
import com.fibelatti.core.archcomponents.test.extension.shouldNeverReceiveValues
import com.fibelatti.core.extension.empty
import com.fibelatti.core.functional.Failure
import com.fibelatti.core.functional.Success
import com.fibelatti.core.provider.ResourceProvider
import com.fibelatti.core.test.extension.givenSuspend
import com.fibelatti.core.test.extension.mock
import com.fibelatti.core.test.extension.safeAny
import com.fibelatti.core.test.extension.verifySuspend
import com.fibelatti.pinboard.BaseViewModelTest
import com.fibelatti.pinboard.MockDataProvider.createPost
import com.fibelatti.pinboard.MockDataProvider.mockTagString1
import com.fibelatti.pinboard.MockDataProvider.mockTagString2
import com.fibelatti.pinboard.MockDataProvider.mockTags
import com.fibelatti.pinboard.MockDataProvider.mockUrlDescription
import com.fibelatti.pinboard.MockDataProvider.mockUrlInvalid
import com.fibelatti.pinboard.MockDataProvider.mockUrlTitle
import com.fibelatti.pinboard.MockDataProvider.mockUrlValid
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.features.appstate.AppStateRepository
import com.fibelatti.pinboard.features.appstate.PostSaved
import com.fibelatti.pinboard.features.posts.domain.usecase.AddPost
import com.fibelatti.pinboard.features.posts.domain.usecase.GetSuggestedTags
import com.fibelatti.pinboard.features.posts.domain.usecase.InvalidUrlException
import com.fibelatti.pinboard.features.prepareToReceiveMany
import com.fibelatti.pinboard.features.shouldHaveReceived
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never

internal class EditPostViewModelTest : BaseViewModelTest() {

    private val mockAppStateRepository = mock<AppStateRepository>()
    private val mockGetSuggestedTags = mock<GetSuggestedTags>()
    private val mockAddPost = mock<AddPost>()
    private val mockResourceProvider = mock<ResourceProvider>()

    private val editPostViewModel = EditPostViewModel(
        mockAppStateRepository,
        mockGetSuggestedTags,
        mockAddPost,
        mockResourceProvider
    )

    @BeforeEach
    fun setup() {
        given(mockResourceProvider.getString(R.string.validation_error_invalid_url))
            .willReturn("R.string.validation_error_invalid_url")
        given(mockResourceProvider.getString(R.string.validation_error_empty_url))
            .willReturn("R.string.validation_error_empty_url")
        given(mockResourceProvider.getString(R.string.validation_error_empty_title))
            .willReturn("R.string.validation_error_empty_title")
    }

    @Test
    fun `GIVEN getSuggestedTags will fail WHEN searchForTag is called THEN suggestedTags should never receive values`() {
        // GIVEN
        givenSuspend { mockGetSuggestedTags(safeAny()) }
            .willReturn(Failure(Exception()))

        // WHEN
        editPostViewModel.searchForTag(mockTagString1, mock())

        // THEN
        editPostViewModel.suggestedTags.shouldNeverReceiveValues()
    }

    @Test
    fun `GIVEN getSuggestedTags will succeed WHEN searchForTag is called THEN suggestedTags should receive its response`() {
        // GIVEN
        val result = listOf(mockTagString1, mockTagString2)
        givenSuspend { mockGetSuggestedTags(safeAny()) }
            .willReturn(Success(result))

        // WHEN
        editPostViewModel.searchForTag(mockTagString1, mock())

        // THEN
        editPostViewModel.suggestedTags.currentValueShouldBe(result)
    }

    @Test
    fun `GIVEN url is blank WHEN saveLink is called THEN invalidUrlError will receive a value`() {
        // WHEN
        editPostViewModel.saveLink(
            url = "",
            title = mockUrlTitle,
            description = mockUrlDescription,
            private = true,
            readLater = true,
            tags = mockTags
        )

        // THEN
        editPostViewModel.invalidUrlError.currentValueShouldBe("R.string.validation_error_empty_url")
        editPostViewModel.saved.shouldNeverReceiveValues()
        verifySuspend(mockAppStateRepository, never()) { runAction(safeAny()) }
    }

    @Test
    fun `GIVEN title is blank WHEN saveLink is called THEN invalidUrlTitleError will received a value`() {
        // WHEN
        editPostViewModel.saveLink(
            url = mockUrlValid,
            title = "",
            description = mockUrlDescription,
            private = true,
            readLater = true,
            tags = mockTags
        )

        // THEN
        editPostViewModel.invalidUrlTitleError.currentValueShouldBe("R.string.validation_error_empty_title")
        editPostViewModel.saved.shouldNeverReceiveValues()
        verifySuspend(mockAppStateRepository, never()) { runAction(safeAny()) }
    }

    @Test
    fun `GIVEN addPost returns InvalidUrlException WHEN saveLink is called THEN invalidUrlError will receive a value`() {
        // GIVEN
        givenSuspend {
            mockAddPost(AddPost.Params(
                url = mockUrlInvalid,
                title = mockUrlTitle,
                description = mockUrlDescription,
                private = true,
                readLater = true,
                tags = mockTags
            ))
        }.willReturn(Failure(InvalidUrlException()))

        val loadingObserver = editPostViewModel.loading.prepareToReceiveMany()

        // WHEN
        editPostViewModel.saveLink(
            url = mockUrlInvalid,
            title = mockUrlTitle,
            description = mockUrlDescription,
            private = true,
            readLater = true,
            tags = mockTags
        )

        // THEN
        editPostViewModel.loading.shouldHaveReceived(loadingObserver, true, false)
        editPostViewModel.invalidUrlError.currentValueShouldBe("R.string.validation_error_invalid_url")
        editPostViewModel.invalidUrlTitleError.currentValueShouldBe(String.empty())
        editPostViewModel.saved.shouldNeverReceiveValues()
        verifySuspend(mockAppStateRepository, never()) { runAction(safeAny()) }
    }

    @Test
    fun `GIVEN addPost returns an error WHEN saveLink is called THEN error will receive a value`() {
        // GIVEN
        val error = Exception()
        givenSuspend {
            mockAddPost(AddPost.Params(
                url = mockUrlValid,
                title = mockUrlTitle,
                description = mockUrlDescription,
                private = true,
                readLater = true,
                tags = mockTags
            ))
        }.willReturn(Failure(error))

        val loadingObserver = editPostViewModel.loading.prepareToReceiveMany()

        // WHEN
        editPostViewModel.saveLink(
            url = mockUrlValid,
            title = mockUrlTitle,
            description = mockUrlDescription,
            private = true,
            readLater = true,
            tags = mockTags
        )

        // THEN
        editPostViewModel.loading.shouldHaveReceived(loadingObserver, true, false)
        editPostViewModel.invalidUrlError.currentValueShouldBe(String.empty())
        editPostViewModel.invalidUrlTitleError.currentValueShouldBe(String.empty())
        editPostViewModel.error.currentValueShouldBe(error)
        editPostViewModel.saved.shouldNeverReceiveValues()
        verifySuspend(mockAppStateRepository, never()) { runAction(safeAny()) }
    }

    @Test
    fun `GIVEN addPost is successful WHEN saveLink is called THEN AppStateRepository should run PostSaved`() {
        // GIVEN
        val post = createPost()
        givenSuspend {
            mockAddPost(AddPost.Params(
                url = mockUrlValid,
                title = mockUrlTitle,
                description = mockUrlDescription,
                private = true,
                readLater = true,
                tags = mockTags
            ))
        }.willReturn(Success(post))

        val loadingObserver = editPostViewModel.loading.prepareToReceiveMany()

        // WHEN
        editPostViewModel.saveLink(
            url = mockUrlValid,
            title = mockUrlTitle,
            description = mockUrlDescription,
            private = true,
            readLater = true,
            tags = mockTags
        )

        // THEN
        editPostViewModel.loading.shouldHaveReceived(loadingObserver, true)
        editPostViewModel.invalidUrlError.currentValueShouldBe(String.empty())
        editPostViewModel.invalidUrlTitleError.currentValueShouldBe(String.empty())
        editPostViewModel.error.shouldNeverReceiveValues()
        editPostViewModel.saved.currentEventShouldBe(Unit)
        verifySuspend(mockAppStateRepository) { runAction(PostSaved(post)) }
    }
}
