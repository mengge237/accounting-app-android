package com.smxy.myapplication.model;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.smxy.myapplication.R;
import com.smxy.myapplication.activity.MainContainerActivity;
import com.smxy.myapplication.utils.ErrorHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MusicService";

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_SEEK = "ACTION_SEEK";

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "music_player_channel";

    private MediaPlayer mediaPlayer;
    private List<Song> playlist = new ArrayList<>();
    private int currentPosition = 0;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private final IBinder binder = new MusicBinder();
    private OnMusicStateListener listener;

    public interface OnMusicStateListener {
        void onStateChanged(boolean isPlaying, Song song, int position);
        void onProgressUpdate(int progress, int duration);
        void onPlaylistUpdated(List<Song> playlist);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializeMediaPlayer();
        loadBuiltinSongs();
        loadLocalSongs();
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    private boolean hasReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 以下不需要通知权限
    }

    @Nullable
    @SuppressWarnings("MissingPermission")
    private android.database.Cursor safeQueryMediaStore() {
        if (!hasReadPermission()) {
            Log.w(TAG, "没有读取权限，无法查询媒体库");
            return null;
        }

        try {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            return getContentResolver().query(uri, projection, selection, null, sortOrder);
        } catch (SecurityException e) {
            Log.e(TAG, "查询媒体库时发生安全异常", e);
            return null;
        }
    }

    private void loadBuiltinSongs() {
        try {
            Class<?> rawClass;
            try {
                rawClass = Class.forName("com.smxy.myapplication.R$raw");
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "没有内置音乐资源（R$raw 类不存在）");
                return;
            }

            java.lang.reflect.Field[] fields = rawClass.getFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    String name = field.getName();
                    int resourceId = field.getInt(null);

                    AssetFileDescriptor afd = getResources().openRawResourceFd(resourceId);
                    if (afd == null) continue;

                    MediaPlayer tempPlayer = new MediaPlayer();
                    tempPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    tempPlayer.prepare();
                    long duration = tempPlayer.getDuration();
                    tempPlayer.release();
                    afd.close();

                    String path = "android.resource://" + getPackageName() + "/" + resourceId;

                    String title = name.replace("_", " ");
                    String artist = getString(R.string.music_builtin_artist);

                    Song song = new Song(resourceId, title, artist, path, duration);
                    playlist.add(song);
                    Log.d(TAG, "加载内置音乐: " + title);
                } catch (Exception e) {
                    Log.w(TAG, "加载内置音乐失败: " + field.getName(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "加载内置音乐列表失败", e);
        }
    }

    private void loadLocalSongs() {
        try {
            int builtinCount = playlist.size();

            if (!hasReadPermission()) {
                Log.w(TAG, "缺少读取音乐权限");
                notifyPlaylistUpdated();
                return;
            }

            try (android.database.Cursor cursor = safeQueryMediaStore()) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                    if (idColumn < 0 || titleColumn < 0 || artistColumn < 0
                            || dataColumn < 0 || durationColumn < 0) {
                        Log.w(TAG, "媒体库列索引无效，跳过加载");
                        notifyPlaylistUpdated();
                        return;
                    }

                    while (cursor.moveToNext()) {
                        try {
                            int id = cursor.getInt(idColumn);
                            String title = cursor.getString(titleColumn);
                            String artist = cursor.getString(artistColumn);
                            String path = cursor.getString(dataColumn);
                            long duration = cursor.getLong(durationColumn);

                            if (path != null && !path.isEmpty() && duration > 0) {
                                Song song = new Song(id,
                                        ErrorHandler.getSafeString(title, getString(R.string.music_unknown_title)),
                                        ErrorHandler.getSafeString(artist, getString(R.string.music_unknown_artist)),
                                        path, duration);
                                playlist.add(song);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "解析单条歌曲数据失败，跳过", e);
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "读取媒体数据时发生安全异常", e);
            } catch (Exception e) {
                Log.e(TAG, "加载本地歌曲时发生异常", e);
            }
        } finally {
            notifyPlaylistUpdated();
        }
    }

    private void notifyPlaylistUpdated() {
        if (listener != null) {
            try {
                listener.onPlaylistUpdated(playlist);
            } catch (Exception e) {
                Log.e(TAG, "通知播放列表更新失败", e);
            }
        }
    }

    private void notifyStateChanged(boolean playing, Song song, int position) {
        if (listener != null) {
            try {
                listener.onStateChanged(playing, song, position);
            } catch (Exception e) {
                Log.e(TAG, "通知状态变更失败", e);
            }
        }
    }

    private void notifyError(String errorMessage) {
        Log.e(TAG, errorMessage);
        // 仅在应用前台时显示 Toast，避免进程销毁时 DeadObjectException
        try {
            ErrorHandler.showToast(this, errorMessage);
        } catch (Exception e) {
            Log.w(TAG, "notifyError showToast failed", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_NEXT:
                        next();
                        break;
                    case ACTION_PREVIOUS:
                        previous();
                        break;
                    case ACTION_STOP:
                        stopPlayback();
                        break;
                    case ACTION_SEEK:
                        int progress = intent.getIntExtra("progress", 0);
                        seekTo(progress);
                        break;
                    default:
                        Log.d(TAG, "未知操作: " + action);
                        break;
                }
            }
        }
        return START_STICKY;
    }

    public void play() {
        try {
            if (playlist.isEmpty()) {
                loadBuiltinSongs();
                loadLocalSongs();
                if (playlist.isEmpty()) {
                    Log.w(TAG, "播放列表为空");
                    notifyError("播放列表为空");
                    return;
                }
            }

            if (mediaPlayer == null) {
                initializeMediaPlayer();
            }

            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer 初始化失败");
                notifyError("播放器初始化失败");
                return;
            }

            if (currentPosition < 0 || currentPosition >= playlist.size()) {
                currentPosition = 0;
            }

            Song song = playlist.get(currentPosition);
            if (song == null || song.getPath() == null || song.getPath().isEmpty()) {
                Log.w(TAG, "当前歌曲无效，跳过");
                next();
                return;
            }

            try {
                if (mediaPlayer.isPlaying()) {
                    return;
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "MediaPlayer 状态异常，重置后重新播放", e);
                mediaPlayer.reset();
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepareAsync();
            isPlaying = true;

            if (hasNotificationPermission()) {
                try {
                    startForeground(NOTIFICATION_ID, createNotification(song));
                } catch (Exception e) {
                    Log.w(TAG, "启动前台服务失败", e);
                }
            } else {
                Log.w(TAG, "没有通知权限，无法显示播放通知");
            }

            notifyStateChanged(true, song, currentPosition);

        } catch (IOException e) {
            Log.e(TAG, "播放失败 - IO异常", e);
            isPlaying = false;
            notifyError("播放失败: " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, "播放失败 - 状态异常", e);
            isPlaying = false;
            notifyError("播放状态异常");
        } catch (Exception e) {
            Log.e(TAG, "播放失败 - 未知错误", e);
            isPlaying = false;
            notifyError("播放失败");
        }
    }

    public void pause() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                Song currentSong = getCurrentSong();
                notifyStateChanged(false, currentSong, currentPosition);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "暂停播放时状态异常", e);
            isPlaying = false;
        } catch (Exception e) {
            Log.e(TAG, "暂停播放失败", e);
        }
    }

    public void next() {
        if (playlist == null || playlist.isEmpty()) return;

        try {
            int size = playlist.size();
            if (isShuffle) {
                Random random = new Random();
                int newPosition;
                do {
                    newPosition = random.nextInt(size);
                } while (newPosition == currentPosition && size > 1);
                currentPosition = newPosition;
            } else {
                currentPosition = (currentPosition + 1) % size;
            }

            play();
            updateNotification();
        } catch (Exception e) {
            Log.e(TAG, "切换到下一首失败", e);
        }
    }

    public void previous() {
        if (playlist == null || playlist.isEmpty()) return;

        try {
            if (isShuffle) {
                next();
                return;
            }

            int size = playlist.size();
            currentPosition = (currentPosition - 1 + size) % size;
            play();
            updateNotification();
        } catch (Exception e) {
            Log.e(TAG, "切换到上一首失败", e);
        }
    }

    public void seekTo(int progress) {
        try {
            if (mediaPlayer != null && progress >= 0) {
                mediaPlayer.seekTo(progress);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "seekTo 状态异常", e);
        } catch (Exception e) {
            Log.e(TAG, "seekTo 失败", e);
        }
    }

    public void stopPlayback() {
        try {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "stopPlayback 停止时状态异常", e);
                }
                mediaPlayer.reset();
                isPlaying = false;
            }
            stopForeground(true);
            notifyStateChanged(false, null, currentPosition);
        } catch (Exception e) {
            Log.e(TAG, "停止播放失败", e);
        }
    }

    public boolean isPlaying() {
        try {
            if (mediaPlayer != null) {
                return isPlaying && mediaPlayer.isPlaying();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "isPlaying 状态异常", e);
        }
        return false;
    }

    public Song getCurrentSong() {
        if (playlist != null && currentPosition >= 0 && currentPosition < playlist.size()) {
            return playlist.get(currentPosition);
        }
        return null;
    }

    public int getPlayProgress() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "getCurrentPosition 状态异常", e);
        }
        return 0;
    }

    public int getDuration() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "getDuration 状态异常", e);
        }
        return 0;
    }

    public List<Song> getPlaylist() {
        return playlist != null ? playlist : new ArrayList<>();
    }

    public void setPlaylist(List<Song> playlist) {
        if (playlist != null) {
            this.playlist = playlist;
        } else {
            this.playlist = new ArrayList<>();
        }
        currentPosition = 0;
        notifyPlaylistUpdated();
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
    }

    public void setRepeat(boolean repeat) {
        this.isRepeat = repeat;
    }

    public boolean isShuffle() { return isShuffle; }
    public boolean isRepeat() { return isRepeat; }

    public void setListener(OnMusicStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            mp.start();
            Song currentSong = getCurrentSong();
            notifyStateChanged(true, currentSong, currentPosition);
        } catch (Exception e) {
            Log.e(TAG, "onPrepared 启动播放失败", e);
            isPlaying = false;
            notifyError("播放准备失败");
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            if (isRepeat) {
                play();
            } else {
                next();
            }
        } catch (Exception e) {
            Log.e(TAG, "onCompletion 处理失败", e);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.music_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.music_notification_channel_description));
            channel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(Song song) {
        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 1, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 2, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 3, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent contentIntent = new Intent(this, MainContainerActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = song.getTitle();
        String artist = song.getArtist();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous, getString(R.string.music_previous), previousPendingIntent)
                .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        isPlaying ? getString(R.string.music_pause) : getString(R.string.music_play), playPendingIntent)
                .addAction(android.R.drawable.ic_media_next, getString(R.string.music_next), nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.music_stop), stopPendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        Song song = getCurrentSong();
        if (song != null && hasNotificationPermission()) {
            try {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification(song));
            } catch (SecurityException e) {
                Log.w(TAG, "更新通知失败", e);
            }
        }
    }
}