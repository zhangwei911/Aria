package com.arialyy.aria.core.command;

import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.NetUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by AriaL on 2017/6/13.
 * 恢复所有停止的任务
 * 1.如果执行队列没有满，则开始下载任务，直到执行队列满
 * 2.如果队列执行队列已经满了，则将所有任务添加到等待队列中
 * 3.如果队列中只有等待状态的任务，如果执行队列没有满，则会启动等待状态的任务，如果执行队列已经满了，则会将所有等待状态的任务加载到缓存队列中
 * 4.恢复下载的任务规则是，停止时间越晚的任务启动越早，按照DESC来进行排序
 */
final class ResumeAllCmd<T extends AbsTaskWrapper> extends AbsNormalCmd<T> {
  private List<AbsTaskWrapper> mWaitList = new ArrayList<>();

  ResumeAllCmd(T entity, int taskType) {
    super(entity, taskType);
  }

  @Override public void executeCmd() {
    if (!NetUtils.isConnected(AriaManager.getInstance().getAPP())) {
      ALog.w(TAG, "恢复任务失败，网络未连接");
      return;
    }
    if (isDownloadCmd) {
      findTaskData(1);
      findTaskData(2);
    } else {
      findTaskData(3);
    }
    resumeWaitTask();
  }

  /**
   * 查找数据库中的所有任务数据
   *
   * @param type {@code 1}单任务下载任务；{@code 2}任务组下载任务；{@code 3} 单任务上传任务
   */
  private void findTaskData(int type) {
    if (type == 1) {
      List<DownloadEntity> entities =
          DbEntity.findDatas(DownloadEntity.class,
              "isGroupChild=? AND state!=? ORDER BY stopTime DESC", "false", "1");
      if (entities != null && !entities.isEmpty()) {
        for (DownloadEntity entity : entities) {
          addResumeEntity(TaskWrapperManager.getInstance()
              .getNormalTaskWrapper(DTaskWrapper.class, entity.getId()));
        }
      }
    } else if (type == 2) {
      List<DownloadGroupEntity> entities =
          DbEntity.findDatas(DownloadGroupEntity.class, "state!=? ORDER BY stopTime DESC", "1");
      if (entities != null && !entities.isEmpty()) {
        for (DownloadGroupEntity entity : entities) {
          addResumeEntity(
              TaskWrapperManager.getInstance()
                  .getGroupWrapper(DGTaskWrapper.class, entity.getId()));
        }
      }
    } else if (type == 3) {
      List<UploadEntity> entities =
          DbEntity.findDatas(UploadEntity.class, "state!=? ORDER BY stopTime DESC", "1");
      if (entities != null && !entities.isEmpty()) {
        for (UploadEntity entity : entities) {
          addResumeEntity(TaskWrapperManager.getInstance()
              .getNormalTaskWrapper(UTaskWrapper.class, entity.getId()));
        }
      }
    }
  }

  /**
   * 添加恢复实体
   */
  private void addResumeEntity(AbsTaskWrapper te) {
    if (te == null || te.getEntity() == null) {
      return;
    }
    if (!mQueue.taskExists(te.getKey())) {
      mWaitList.add(te);
    }
  }

  /**
   * 处理等待状态的任务
   */
  private void resumeWaitTask() {
    int maxTaskNum = mQueue.getMaxTaskNum();
    if (mWaitList == null || mWaitList.isEmpty()) return;
    for (AbsTaskWrapper te : mWaitList) {
      if (te instanceof DTaskWrapper) {
        mQueue = DownloadTaskQueue.getInstance();
      } else if (te instanceof UTaskWrapper) {
        mQueue = UploadTaskQueue.getInstance();
      } else if (te instanceof DGTaskWrapper) {
        mQueue = DownloadGroupTaskQueue.getInstance();
      }
      if (mQueue.getCurrentExePoolNum() < maxTaskNum) {
        startTask(createTask(te));
      } else {
        te.getEntity().setState(IEntity.STATE_WAIT);
        AbsTask task = createTask(te);
        sendWaitState(task);
      }
    }
  }
}
