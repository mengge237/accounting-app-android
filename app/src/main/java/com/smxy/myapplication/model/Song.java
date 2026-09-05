package com.smxy.myapplication.model;

public class Song {
    private String title;
    private String artist;
    private String path;
    private long duration;
    private int id;

    public Song(int id, String title, String artist, String path, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getFormattedDuration() {
        long minutes = duration / 1000 / 60;
        long seconds = duration / 1000 % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}