package org.rbdc.sra.objects;

import java.util.Calendar;

/**
 * Created by chad on 2/3/15.
 */
public class Note {
    public String _noteType;
    private Calendar _dateCreated;
    private Calendar _dateUpdated;
    private String _title;
    private String _noteContents;
    private String _author;

    public Note (String noteType, String title, String noteContents) {
        _noteType = noteType;
        _dateCreated = _dateUpdated = Calendar.getInstance();
        _title = title;
        _noteContents = noteContents;
        // Need to add author to CRUDflinger so we can store it here
        //_author =
    }

    public void editNoteContents(String contents) {
        _noteContents = contents;
        _dateUpdated = Calendar.getInstance();
    }

    public void editNoteTitle(String title) {
        _title = title;
        _dateUpdated = Calendar.getInstance();
    }

    public String getNoteContents () { return _noteContents;}

    public String getNoteTitle() {return _title;}
}