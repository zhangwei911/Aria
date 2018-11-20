package com.arialyy.aria.core.command.normal;

import android.text.TextUtils;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.manager.TEManager;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.UploadTaskEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
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
final class ResumeAllCmd<T extends AbsTaskEntity> extends AbsNormalCmd<T> {
  private List<AbsTaskEntity> mWaitList = new ArrayList<>();

  ResumeAllCmd(T entity, int taskType) {
    super(entity, taskType);
  }

  @Override public void executeCmd() {
    if (!NetUtils.isConnected(AriaManager.APP)) {
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
          //if (TextUtils.isEmpty(entity.getDownloadPath())){
          //  continue;
          //}
          resumeTask(TEManager.getInstance().getTEntity(DownloadTaskEntity.class, entity.getKey()));
        }
      }
    } else if (type == 2) {
      List<DownloadGroupEntity> entities =
          DbEntity.findDatas(DownloadGroupEntity.class, "state!=? ORDER BY stopTime DESC", "1");
      if (entities != null && !entities.isEmpty()) {
        for (DownloadGroupEntity entity : entities) {
          resumeTask(
              TEManager.getInstance().getGTEntity(DownloadGroupTaskEntity.class, entity.getUrls()));
        }
      }
    } else if (type == 3) {
      List<UploadEntity> entities =
          DbEntity.findDatas(UploadEntity.class, "state!=? ORDER BY stopTime DESC", "1");
      if (entities != null && !entities.isEmpty()) {
        for (UploadEntity entity : entities) {
          resumeTask(TEManager.getInstance().getTEntity(UploadTaskEntity.class, entity.getKey()));
        }
      }
    }
  }

  /**
   * 恢复任务
   */
  private void resumeTask(AbsTaskEntity te) {
    if (te == null || te.getEntity() == null) return;
    int state = te.getState();
    if (state == IEntity.STATE_STOP || state == IEntity.STATE_OTHER) {
      resumeEntity(te);
    } else if (state == IEntity.STATE_WAIT || state == IEntity.STATE_FAIL) {
      mWaitList.add(te);
    } else {
      if (!mQueue.taskIsRunning(te.getEntity().getKey())) {
        mWaitList.add(te);
      }
    }
  }

  /**
   * 处理等待状态的任务
   */
  private void resumeWaitTask() {
    int maxTaskNum = mQueue.getMaxTaskNum();
    if (mWaitList == null || mWaitList.isEmpty()) return;
    for (AbsTaskEntity te : mWaitList) {
      if (te instanceof DownloadTaskEntity) {
        mQueue = DownloadTaskQueue.getInstance();
      } else if (te instanceof UploadTaskEntity) {
        mQueue = UploadTaskQueue.getInstance();
      } else if (te instanceof DownloadGroupTaskEntity) {
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

  /**
   * 恢复实体任务
   *
   * @param te 任务实体
   */
  private void resumeEntity(AbsTaskEntity te) {
    if (te instanceof DownloadTaskEntity) {
      if (te.getRequestType() == AbsTaskEntity.D_FTP
          || te.getRequestType() == AbsTaskEntity.U_FTP) {
        te.setUrlEntity(CommonUtil.getFtpUrlInfo(te.getEntity().getKey()));
      }
      mQueue = DownloadTaskQueue.getInstance();
    } else if (te instanceof UploadTaskEntity) {
      mQueue = UploadTaskQueue.getInstance();
    } else if (te instanceof DownloadGroupTaskEntity) {
      mQueue = DownloadGroupTaskQueue.getInstance();
    }
    int exeNum = mQueue.getCurrentExePoolNum();
    if (exeNum == 0 || exeNum < mQueue.getMaxTaskNum()) {
      startTask(createTask(te));
    } else {
      te.getEntity().setState(IEntity.STATE_WAIT);
      AbsTask task = createTask(te);
      sendWaitState(task);
    }
  }
}
