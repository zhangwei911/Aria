package com.arialyy.simple.core.download.m3u8;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.m3u8.M3U8Entity;
import com.arialyy.frame.base.BaseFragment;
import com.arialyy.simple.R;
import com.arialyy.simple.databinding.FragmentVideoPlayerBinding;
import java.io.IOException;
import java.util.List;

@SuppressLint("ValidFragment")
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoPlayerFragment extends BaseFragment<FragmentVideoPlayerBinding> {

  static final int SEEK_BAR_PROGRESS_KEY = 0xC1;

  private M3U8VodModule mModule;
  private DownloadEntity mEntity;
  private int mPeerIndex;
  SparseArray<String> mPlayers = new SparseArray<>();
  private SurfaceHolder mSurfaceHolder;
  private NextMediaPlayer nexPlayer;

  VideoPlayerFragment(int peerIndex, DownloadEntity entity) {
    mEntity = entity;
    mPeerIndex = peerIndex;
    List<M3U8Entity.PeerInfo> peerInfos = entity.getM3U8Entity().getCompletedPeer();
    if (peerInfos != null) {
      for (M3U8Entity.PeerInfo info : peerInfos) {
        mPlayers.put(info.peerId, info.peerPath);
      }
    }
  }

  @Override protected void init(Bundle savedInstanceState) {
    mModule = ViewModelProviders.of(this).get(M3U8VodModule.class);
    final MediaPlayer firstPlayer = new MediaPlayer();
    firstPlayer.setScreenOnWhilePlaying(true);

    getBinding().surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        firstPlayer.setSurface(holder.getSurface());
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

      }

      @Override public void surfaceDestroyed(SurfaceHolder holder) {

      }
    });
    getBinding().seekBar.setMax(mEntity.getM3U8Entity().getPeerNum());
    getBinding().seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {
        dataCallback(SEEK_BAR_PROGRESS_KEY, seekBar.getProgress());
      }
    });
    getBinding().controlBt.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        try {
          firstPlayer.setDataSource(mPlayers.valueAt(0));
          firstPlayer.prepareAsync();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    firstPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override public void onPrepared(MediaPlayer mp) {
        mp.start();
        setNextMediaPlayer(mp);
      }
    });

    firstPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override public void onCompletion(MediaPlayer mp) {
        mp.setDisplay(null);
        nexPlayer.getPlayer().setSurface(mSurfaceHolder.getSurface());
        nexPlayer.start();
      }
    });
  }

  /**
   * 设置下一个分片
   */
  private void setNextMediaPlayer(final MediaPlayer lastPlayer) {
    mPeerIndex++;
    String nextPeerPath = mPlayers.get(mPeerIndex);
    if (!TextUtils.isEmpty(nextPeerPath)) {
      nexPlayer = new NextMediaPlayer(nextPeerPath);
      nexPlayer.prepareAsync();

      nexPlayer.setListener(new NextMediaPlayer.StateListener() {
        @Override public void onStart(MediaPlayer mp) {
          setNextMediaPlayer(mp);
        }

        @Override public void onCompletion(MediaPlayer mp) {
          mp.setDisplay(null);
          nexPlayer.getPlayer().setSurface(mSurfaceHolder.getSurface());
          nexPlayer.start();
        }

        @Override public void onPrepared(MediaPlayer mp) {
          lastPlayer.setNextMediaPlayer(nexPlayer.getPlayer());
        }
      });
    }
  }

  @Override protected int setLayoutId() {
    return R.layout.fragment_video_player;
  }

  public void addPlayer(int peerIndex, String peerPath) {
    mPlayers.put(peerIndex, peerPath);
  }

  private static class NextMediaPlayer implements MediaPlayer.OnPreparedListener,
      MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private StateListener listener;

    public interface StateListener {
      void onStart(MediaPlayer mp);

      void onCompletion(MediaPlayer mp);

      void onPrepared(MediaPlayer mp);
    }

    private MediaPlayer player;
    private String videoPath;

    private NextMediaPlayer(String videoPath) {
      player = new MediaPlayer();
      player.setAudioStreamType(AudioManager.STREAM_MUSIC);
      player.setOnPreparedListener(this);
      player.setOnErrorListener(this);
      player.setOnCompletionListener(this);
      player.setScreenOnWhilePlaying(true);
      this.videoPath = videoPath;
      player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override public void onPrepared(MediaPlayer mp) {
          if (listener != null) {
            listener.onPrepared(mp);
          }
        }
      });
    }

    private void prepareAsync() {
      try {
        player.setDataSource(videoPath);
        player.prepareAsync();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void setListener(StateListener listener) {
      this.listener = listener;
    }

    public String getVideoPath() {
      return videoPath;
    }

    public void start() {
      player.start();
      if (listener != null) {
        listener.onStart(player);
      }
    }

    @Override public void onCompletion(MediaPlayer mp) {
      if (listener != null) {
        listener.onCompletion(mp);
      }
    }

    @Override public boolean onError(MediaPlayer mp, int what, int extra) {
      return false;
    }

    @Override public void onPrepared(MediaPlayer mp) {
    }

    private MediaPlayer getPlayer() {
      return player;
    }
  }
}
