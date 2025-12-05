package com.example.gallery2;

import java.util.LinkedList;
import java.util.List;

/**
 * 管理待删除的照片，支持撤销操作
 * 实际删除会在退出Activity时执行
 */
public class PendingDeleteManager {
    private LinkedList<PendingDelete> pendingDeletes = new LinkedList<>();

    public static class PendingDelete {
        public static final int TYPE_DELETE = 1;
        public static final int TYPE_DELAY = 2;

        private Photo photo;
        private int originalPosition;
        private int actionType;
        private long newPhotoId; // 仅用于TYPE_DELAY

        public PendingDelete(Photo photo, int originalPosition, int actionType) {
            this.photo = photo;
            this.originalPosition = originalPosition;
            this.actionType = actionType;
            this.newPhotoId = -1;
        }

        public PendingDelete(Photo photo, int originalPosition, int actionType, long newPhotoId) {
            this.photo = photo;
            this.originalPosition = originalPosition;
            this.actionType = actionType;
            this.newPhotoId = newPhotoId;
        }

        public Photo getPhoto() {
            return photo;
        }

        public int getOriginalPosition() {
            return originalPosition;
        }

        public int getActionType() {
            return actionType;
        }

        public long getNewPhotoId() {
            return newPhotoId;
        }
    }

    /**
     * 添加待删除记录
     */
    public void addPendingDelete(PendingDelete pendingDelete) {
        pendingDeletes.addFirst(pendingDelete);
    }

    /**
     * 撤销最近的删除操作
     * @return 被撤销的待删除记录，如果没有则返回null
     */
    public PendingDelete undo() {
        if (pendingDeletes.isEmpty()) {
            return null;
        }
        return pendingDeletes.removeFirst();
    }

    /**
     * 获取待删除数量
     */
    public int getCount() {
        return pendingDeletes.size();
    }

    /**
     * 是否有可撤销的操作
     */
    public boolean canUndo() {
        return !pendingDeletes.isEmpty();
    }

    /**
     * 获取所有待删除记录（用于最终执行删除）
     */
    public List<PendingDelete> getAllPendingDeletes() {
        return new LinkedList<>(pendingDeletes);
    }

    /**
     * 清空所有待删除记录
     */
    public void clear() {
        pendingDeletes.clear();
    }
}
