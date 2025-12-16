package com.example.gallery2

import java.util.LinkedList

/**
 * 管理待删除的照片，支持撤销操作
 * 实际删除会在退出Activity时执行
 */
class PendingDeleteManager {
    private val pendingDeletes = LinkedList<PendingDelete>()

    data class PendingDelete(
        val photo: Photo,
        val originalPosition: Int,
        val actionType: Int,
        val newPhotoId: Long = -1 // 仅用于TYPE_DELAY
    ) {
        companion object {
            const val TYPE_DELETE = 1
            const val TYPE_DELAY = 2
        }
    }

    /**
     * 添加待删除记录
     */
    fun addPendingDelete(pendingDelete: PendingDelete) {
        pendingDeletes.addFirst(pendingDelete)
    }

    /**
     * 撤销最近的删除操作
     * @return 被撤销的待删除记录，如果没有则返回null
     */
    fun undo(): PendingDelete? {
        if (pendingDeletes.isEmpty()) {
            return null
        }
        return pendingDeletes.removeFirst()
    }

    /**
     * 获取待删除数量
     */
    fun getCount(): Int = pendingDeletes.size

    /**
     * 是否有可撤销的操作
     */
    fun canUndo(): Boolean = pendingDeletes.isNotEmpty()

    /**
     * 获取所有待删除记录（用于最终执行删除）
     */
    fun getAllPendingDeletes(): List<PendingDelete> = LinkedList(pendingDeletes)

    /**
     * 清空所有待删除记录
     */
    fun clear() {
        pendingDeletes.clear()
    }
}
