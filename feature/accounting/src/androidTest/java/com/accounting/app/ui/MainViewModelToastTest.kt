package com.accounting.app.ui

import androidx.test.core.app.ApplicationProvider
import com.accounting.app.data.local.entity.CategoryEntity
import com.accounting.app.data.repository.AppRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainViewModelToastTest {

    @Test
    fun addCategory_setsToastToAdded() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repository = AppRepository(context)
        val viewModel = MainViewModel(context, repository)

        // 确保初始状态为 null
        assertNull(viewModel.uiState.value.toast)

        viewModel.addCategory("expense", "测试分类", null)

        // 等待协程执行完毕
        kotlinx.coroutines.delay(500)

        assertEquals("分类已添加", viewModel.uiState.value.toast)
    }

    @Test
    fun updateCategory_setsToastToUpdated() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repository = AppRepository(context)
        val viewModel = MainViewModel(context, repository)

        viewModel.updateCategory(
            CategoryEntity(
                id = 1, type = "expense", name = "测试", parentId = null,
                sortOrder = 0, isSystem = false, createdAt = 0, updatedAt = 0
            )
        )

        kotlinx.coroutines.delay(500)

        assertEquals("分类已更新", viewModel.uiState.value.toast)
    }

    @Test
    fun deleteCategory_setsToastToDeleted() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repository = AppRepository(context)
        val viewModel = MainViewModel(context, repository)

        viewModel.deleteCategory(1)

        kotlinx.coroutines.delay(500)

        assertEquals("分类已删除", viewModel.uiState.value.toast)
    }

    @Test
    fun clearToast_clearsToastState() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repository = AppRepository(context)
        val viewModel = MainViewModel(context, repository)

        viewModel.addCategory("expense", "测试", null)
        kotlinx.coroutines.delay(500)
        assertEquals("分类已添加", viewModel.uiState.value.toast)

        viewModel.clearToast()
        assertNull(viewModel.uiState.value.toast)
    }
}
