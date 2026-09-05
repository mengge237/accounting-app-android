package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Note;
import java.util.List;

public class NoteResponse {
    private boolean success;
    private String message;
    private List<Note> notes;
    private Integer noteId;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }

    public Integer getNoteId() { return noteId; }
    public void setNoteId(Integer noteId) { this.noteId = noteId; }
}