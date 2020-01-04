package com.arialyy.aria.core.loader;

import android.os.Handler;
import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.inf.IThreadStateManager;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.task.IThreadTaskAdapter;
import com.arialyy.aria.core.task.ThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbsNormalTTBuilder implements IThreadTaskBuilder {
  protected String TAG = CommonUtil.getClassName(this);

  private Handler mStateHandler;
  private AbsTaskWrapper mWrapper;
  private TaskRecord mRecord;
  private int mTotalThreadNum;
  private File mTempFile;
  private int mStartThreadNum;

  public AbsNormalTTBuilder(AbsTaskWrapper wrapper) {
    if (wrapper instanceof DGTaskWrapper) {
      throw new AssertionError("NormalTTBuilder 不适用于组合任务");
    }
    mWrapper = wrapper;
    mTempFile = new File(((AbsNormalEntity) wrapper.getEntity()).getFilePath());
  }

  protected File getTempFile() {
    return mTempFile;
  }

  protected AbsNormalEntity getEntity() {
    return (AbsNormalEntity) mWrapper.getEntity();
  }

  /**
   * 创建线程任务适配器
   */
  public abstract IThreadTaskAdapter getAdapter(SubThreadConfig config);

  /**
   * 处理新任务
   *
   * @param record 任务记录
   * @param totalThreadNum 任务的线程总数
   * @return {@code true}创建新任务成功
   */
  public abstract boolean handleNewTask(TaskRecord record, int totalThreadNum);

  /**
   * 创建线程任务
   */
  private IThreadTask createThreadTask(SubThreadConfig config) {
    ThreadTask task = new ThreadTask(config);
    task.setAdapter(getAdapter(config));
    return task;
  }

  /**
   * 启动断点任务时，创建单线程任务
   *
   * @param record 线程记录
   * @param startNum 启动的线程数
   */
  private IThreadTask createSingThreadTask(ThreadRecord record, int startNum) {
    SubThreadConfig config = new SubThreadConfig();
    config.url = getEntity().isRedirect() ? getEntity().getRedirectUrl() : getEntity().getUrl();
    config.tempFile =
        mRecord.isBlock ? new File(
            String.format(IRecordHandler.SUB_PATH, mTempFile.getPath(), record.threadId))
            : mTempFile;
    config.isBlock = mRecord.isBlock;
    config.startThreadNum = startNum;
    config.taskWrapper = mWrapper;
    config.record = record;
    config.stateHandler = mStateHandler;
    config.threadType = SubThreadConfig.getThreadType(mWrapper.getRequestType());
    config.updateInterval = SubThreadConfig.getUpdateInterval(mWrapper.getRequestType());
    return createThreadTask(config);
  }

  /**
   * 处理不支持断点的任务
   */
  private List<IThreadTask> handleNoSupportBP() {
    List<IThreadTask> list = new ArrayList<>();
    mStartThreadNum = 1;

    IThreadTask task = createSingThreadTask(mRecord.threadRecords.get(0), 1);
    if (task == null) {
      ALog.e(TAG, "创建线程任务失败");
      return null;
    }
    list.add(task);
    return list;
  }

  /**
   * 处理支持断点的任务
   */
  private List<IThreadTask> handleBreakpoint() {
    long fileLength = getEntity().getFileSize();
    long blockSize = fileLength / mTotalThreadNum;
    long currentProgress = 0;
    List<IThreadTask> threadTasks = new ArrayList<>(mTotalThreadNum);

    mRecord.fileLength = fileLength;
    if (mWrapper.isNewTask() && !handleNewTask(mRecord, mTotalThreadNum)) {
      ALog.e(TAG, "初始化线程任务失败");
      return null;
    }

    for (ThreadRecord tr : mRecord.threadRecords) {
      if (!tr.isComplete) {
        mStartThreadNum++;
      }
    }

    for (int i = 0; i < mTotalThreadNum; i++) {
      long startL = i * blockSize, endL = (i + 1) * blockSize;
      ThreadRecord tr = mRecord.threadRecords.get(i);

      if (tr.isComplete) {//该线程已经完成
        currentProgress += endL - startL;
        ALog.d(TAG, String.format("任务【%s】线程__%s__已完成", mWrapper.getKey(), i));
        mStateHandler.obtainMessage(IThreadStateManager.STATE_COMPLETE).sendToTarget();
        continue;
      }

      //如果有记录，则恢复任务
      long r = tr.startLocation;
      //记录的位置需要在线程区间中
      if (startL < r && r <= (i == (mTotalThreadNum - 1) ? fileLength : endL)) {
        currentProgress += r - startL;
      }
      ALog.d(TAG, String.format("任务【%s】线程__%s__恢复任务", getEntity().getFileName(), i));

      IThreadTask task = createSingThreadTask(tr, mStartThreadNum);
      if (task == null) {
        ALog.e(TAG, "创建线程任务失败");
        return null;
      }
      threadTasks.add(task);
    }
    if (currentProgress != getEntity().getCurrentProgress()) {
      ALog.d(TAG, String.format("进度修正，当前进度：%s", currentProgress));
      getEntity().setCurrentProgress(currentProgress);
    }
    //mStateManager.updateProgress(currentProgress);
    return threadTasks;
  }

  private List<IThreadTask> handleTask() {
    if (mWrapper.isSupportBP()) {
      return handleBreakpoint();
    } else {
      return handleNoSupportBP();
    }
  }

  @Override public List<IThreadTask> buildThreadTask(TaskRecord record, Handler stateHandler) {
    mRecord = record;
    mStateHandler = stateHandler;
    mTotalThreadNum = mRecord.threadNum;
    return handleTask();
  }

  @Override public int getCreatedThreadNum() {
    return mStartThreadNum;
  }

  @Override public void accept(ILoaderVisitor visitor) {
    visitor.addComponent(this);
  }
}
