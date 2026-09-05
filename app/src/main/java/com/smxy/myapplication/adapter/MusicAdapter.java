package com.smxy.myapplication.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.Song;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private static final String TAG = "MusicAdapter";

    private List<Song> songs = new ArrayList<>();
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public MusicAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music, parent, false);
            return new ViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "onCreateViewHolder failed", e);
            View emptyView = new View(parent.getContext());
            return new ViewHolder(emptyView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (position < 0 || position >= songs.size()) {
                Log.w(TAG, "Invalid position: " + position);
                return;
            }

            Song song = songs.get(position);
            if (song == null) {
                Log.w(TAG, "Song is null at position: " + position);
                return;
            }

            if (holder.tvTitle != null) {
                holder.tvTitle.setText(ErrorHandler.getSafeString(song.getTitle(), "未知歌曲"));
            }
            if (holder.tvArtist != null) {
                holder.tvArtist.setText(ErrorHandler.getSafeString(song.getArtist(), "未知艺术家"));
            }
            if (holder.tvDuration != null) {
                String duration = song.getFormattedDuration();
                holder.tvDuration.setText(duration != null ? duration : "00:00");
            }

            holder.itemView.setOnClickListener(v -> {
                try {
                    if (listener != null && song != null) {
                        listener.onSongClick(song);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onSongClick failed", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "onBindViewHolder failed at position: " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        try {
            return songs != null ? songs.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void updateData(List<Song> newSongs) {
        try {
            if (songs == null) {
                songs = new ArrayList<>();
            }
            this.songs.clear();
            if (newSongs != null) {
                this.songs.addAll(newSongs);
            }
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "updateData failed", e);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist, tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                tvTitle = itemView.findViewById(R.id.tv_music_title);
                tvArtist = itemView.findViewById(R.id.tv_music_artist);
                tvDuration = itemView.findViewById(R.id.tv_music_duration);
            } catch (Exception e) {
                Log.e(TAG, "ViewHolder init failed", e);
            }
        }
    }
}