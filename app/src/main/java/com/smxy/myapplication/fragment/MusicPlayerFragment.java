package com.smxy.myapplication.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.MusicAdapter;
import com.smxy.myapplication.model.MusicService;
import com.smxy.myapplication.model.Song;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicPlayerFragment extends Fragment implements MusicService.OnMusicStateListener {

    private static final String TAG = "MusicPlayerFragment";

    private MusicService musicService;
    private boolean isBound = false;
    private final AtomicBoolean isFragmentActive = new AtomicBoolean(false);

    private TextView tvSongTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private ImageView ivPlayPause, ivNext, ivPrevious, ivShuffle, ivRepeat;
    private SeekBar seekBar;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (!isFragmentActive.get()) return;

                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                musicService.setListener(MusicPlayerFragment.this);
                isBound = true;
                updateUI();
            } catch (Exception e) {
                ErrorHandler.showToast(getContext(), "音乐服务连接失败");
                android.util.Log.e(TAG, "onServiceConnected failed", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_music_player, container, false);
        } catch (Exception e) {
            ErrorHandler.showToast(getContext(), "加载音乐页面失败");
            android.util.Log.e(TAG, "onCreateView failed", e);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentActive.set(true);
        try {
            initViews(view);
            setupRecyclerView();
            setupListeners();
        } catch (Exception e) {
            ErrorHandler.showToast(getContext(), "初始化音乐播放器失败");
            android.util.Log.e(TAG, "onViewCreated failed", e);
        }
    }

    private void initViews(View view) {
        tvSongTitle = view.findViewById(R.id.tv_song_title);
        tvArtist = view.findViewById(R.id.tv_artist);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        ivPlayPause = view.findViewById(R.id.iv_play_pause);
        ivNext = view.findViewById(R.id.iv_next);
        ivPrevious = view.findViewById(R.id.iv_previous);
        ivShuffle = view.findViewById(R.id.iv_shuffle);
        ivRepeat = view.findViewById(R.id.iv_repeat);
        seekBar = view.findViewById(R.id.seek_bar);
        recyclerView = view.findViewById(R.id.recycler_music);
    }

    private void setupRecyclerView() {
        if (recyclerView == null || getContext() == null) return;

        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new MusicAdapter(song -> {
                if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;

                try {
                    List<Song> playlist = musicService.getPlaylist();
                    if (playlist == null || playlist.isEmpty()) return;

                    int index = playlist.indexOf(song);
                    if (index >= 0) {
                        musicService.setPlaylist(playlist);
                        musicService.play();
                    }
                } catch (Exception e) {
                    ErrorHandler.showToast(getContext(), "播放失败");
                    android.util.Log.e(TAG, "play song failed", e);
                }
            });
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            ErrorHandler.showToast(getContext(), "初始化播放列表失败");
            android.util.Log.e(TAG, "setupRecyclerView failed", e);
        }
    }

    private void setupListeners() {
        try {
            if (ivPlayPause != null) {
                ivPlayPause.setOnClickListener(v -> {
                    if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;
                    try {
                        if (musicService.isPlaying()) {
                            musicService.pause();
                        } else {
                            musicService.play();
                        }
                    } catch (Exception e) {
                        ErrorHandler.showToast(getContext(), "播放控制失败");
                    }
                });
            }

            if (ivNext != null) {
                ivNext.setOnClickListener(v -> {
                    if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;
                    try {
                        musicService.next();
                    } catch (Exception e) {
                        ErrorHandler.showToast(getContext(), "切换歌曲失败");
                    }
                });
            }

            if (ivPrevious != null) {
                ivPrevious.setOnClickListener(v -> {
                    if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;
                    try {
                        musicService.previous();
                    } catch (Exception e) {
                        ErrorHandler.showToast(getContext(), "切换歌曲失败");
                    }
                });
            }

            if (ivShuffle != null) {
                ivShuffle.setOnClickListener(v -> {
                    if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;
                    try {
                        boolean shuffle = !musicService.isShuffle();
                        musicService.setShuffle(shuffle);
                        ivShuffle.setSelected(shuffle);
                        ErrorHandler.showToast(getContext(), shuffle ? "随机播放已开启" : "随机播放已关闭");
                    } catch (Exception e) {
                        ErrorHandler.showToast(getContext(), "设置随机播放失败");
                    }
                });
            }

            if (ivRepeat != null) {
                ivRepeat.setOnClickListener(v -> {
                    if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;
                    try {
                        boolean repeat = !musicService.isRepeat();
                        musicService.setRepeat(repeat);
                        ivRepeat.setSelected(repeat);
                        ErrorHandler.showToast(getContext(), repeat ? "单曲循环已开启" : "单曲循环已关闭");
                    } catch (Exception e) {
                        ErrorHandler.showToast(getContext(), "设置循环播放失败");
                    }
                });
            }

            if (seekBar != null) {
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && ErrorHandler.isFragmentValid(MusicPlayerFragment.this) && musicService != null) {
                            try {
                                musicService.seekTo(progress);
                            } catch (Exception e) {
                                android.util.Log.w(TAG, "seekTo failed", e);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "setupListeners failed", e);
        }
    }

    private void updateUI() {
        if (!ErrorHandler.isFragmentValid(this) || musicService == null) return;

        try {
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                if (tvSongTitle != null) {
                    tvSongTitle.setText(ErrorHandler.getSafeString(currentSong.getTitle(), ""));
                }
                if (tvArtist != null) {
                    tvArtist.setText(ErrorHandler.getSafeString(currentSong.getArtist(), ""));
                }
                if (tvTotalTime != null) {
                    tvTotalTime.setText(currentSong.getFormattedDuration());
                }
            }

            boolean playing = musicService.isPlaying();
            if (ivPlayPause != null) {
                ivPlayPause.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }

            if (ivShuffle != null) {
                ivShuffle.setSelected(musicService.isShuffle());
            }
            if (ivRepeat != null) {
                ivRepeat.setSelected(musicService.isRepeat());
            }

            updateProgress();
        } catch (Exception e) {
            android.util.Log.e(TAG, "updateUI failed", e);
        }
    }

    private void updateProgress() {
        if (!ErrorHandler.isFragmentValid(this) || musicService == null || seekBar == null) return;

        try {
            int current = musicService.getPlayProgress();
            int duration = musicService.getDuration();

            if (duration > 0 && current >= 0) {
                seekBar.setMax(duration);
                seekBar.setProgress(current);
                if (tvCurrentTime != null) {
                    tvCurrentTime.setText(formatTime(current));
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "updateProgress failed", e);
        }
    }

    private String formatTime(int milliseconds) {
        try {
            if (milliseconds < 0) milliseconds = 0;
            long minutes = milliseconds / 1000 / 60;
            long seconds = milliseconds / 1000 % 60;
            return String.format("%02d:%02d", minutes, seconds);
        } catch (Exception e) {
            return "00:00";
        }
    }

    private void startProgressUpdate() {
        try {
            stopProgressUpdate();
            if (progressRunnable == null) {
                progressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (ErrorHandler.isFragmentValid(MusicPlayerFragment.this)) {
                            updateProgress();
                            handler.postDelayed(this, 500);
                        }
                    }
                };
            }
            handler.postDelayed(progressRunnable, 500);
        } catch (Exception e) {
            android.util.Log.w(TAG, "startProgressUpdate failed", e);
        }
    }

    private void stopProgressUpdate() {
        try {
            if (progressRunnable != null && handler != null) {
                handler.removeCallbacks(progressRunnable);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "stopProgressUpdate failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive.set(true);
        try {
            startProgressUpdate();
            Context context = getContext();
            if (context != null) {
                Intent intent = new Intent(context, MusicService.class);
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        } catch (Exception e) {
            ErrorHandler.showToast(getContext(), "启动音乐服务失败");
            android.util.Log.e(TAG, "onResume bindService failed", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive.set(false);
        try {
            stopProgressUpdate();
            if (isBound && getContext() != null) {
                getContext().unbindService(serviceConnection);
                isBound = false;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "onPause unbindService failed", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive.set(false);
        stopProgressUpdate();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onStateChanged(boolean isPlaying, Song song, int position) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateUI);
        }
    }

    @Override
    public void onProgressUpdate(int progress, int duration) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (seekBar != null && tvCurrentTime != null) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        if (duration > 0) {
                            seekBar.setMax(duration);
                            seekBar.setProgress(progress);
                            tvCurrentTime.setText(formatTime(progress));
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "onProgressUpdate failed", e);
                    }
                });
            }
        }
    }

    @Override
    public void onPlaylistUpdated(List<Song> playlist) {
        if (!ErrorHandler.isFragmentValid(this) || adapter == null) return;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    adapter.updateData(playlist);
                } catch (Exception e) {
                    android.util.Log.w(TAG, "onPlaylistUpdated failed", e);
                }
            });
        }
    }
}