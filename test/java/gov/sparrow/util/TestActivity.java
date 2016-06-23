package gov.sparrow.util;

import android.support.v7.app.ActionBarActivity;
import gov.sparrow.datasync.SparrowRestoreManager;
import gov.sparrow.fragment.*;
import gov.sparrow.listeners.NotebookDragListener;

public class TestActivity extends ActionBarActivity implements
        NotebookListFragment.NotebookListClickListener,
        NoteListFragment.NoteListClickListener,
        ActionPaneFragment.ActionPaneClickListener,
        DeleteNoteDialogFragment.DeleteNoteClickListener,
        NotebookDragListener.NoteDragCompleteListener,
        RestoreDialogFragment.RestoreStartedListener,
        RestoreProgressDialogFragment.RestoreCompleteListener,
        EditNotebookDialogFragment.NotebookDeleteListener,
        SetNotebookListSelection {

    public static boolean NOTEBOOK_DRAG_EVENT_COMPLETED = false;
    public static boolean ARCHIVE_CLICKED = false;
    public static boolean CLICKED_ACTION_ITEM = false;
    public static boolean NOTEBOOK_LIST_ITEM_CLICKED = false;
    public static boolean ADD_NOTE_CLICKED_HANDLED = false;
    public static boolean ITEM_CLICK_HANDLED = false;
    public static Long CLICKED_NOTEBOOK_ID;
    public static Long CLICKED_NOTE_ID;
    public static Long ADD_NOTE_NOTEBOOK_ID;
    public static Long CLICKED_ACTION_ITEM_NOTE_ID;
    public static boolean RESTORE_STARTED_CALLED = false;
    public static boolean NOTEBOOK_DELETE_CALLED = false;
    public static Long NOTEBOOK_LIST_SELECTION = null;
    public static boolean RESTORE_COMPLETE_CALLED = false;

    @Override
    public void onNotebookListActionButtonClicked() {

    }

    @Override
    public void onNoteListItemClicked(Long noteId) {
        ITEM_CLICK_HANDLED = true;
        CLICKED_NOTE_ID = noteId;
    }

    @Override
    public void onNotebookListItemClicked(Long notebookId) {
        NOTEBOOK_LIST_ITEM_CLICKED = true;
        CLICKED_NOTEBOOK_ID = notebookId;
    }

    @Override
    public void onCreateNewNoteClicked(Long notebookId) {
        ADD_NOTE_CLICKED_HANDLED = true;
        ADD_NOTE_NOTEBOOK_ID = notebookId;
    }

    @Override
    public void onActionItemNoteLinkClicked(Long noteId) {
        CLICKED_ACTION_ITEM = true;
        CLICKED_ACTION_ITEM_NOTE_ID = noteId;
    }

    @Override
    public void onArchiveCompleted() {
        ARCHIVE_CLICKED = true;
    }

    @Override
    public void noteDragEventCompleted() {
        NOTEBOOK_DRAG_EVENT_COMPLETED = true;
    }

    @Override
    public void onRestoreComplete() {
        RESTORE_COMPLETE_CALLED = true;
    }

    @Override
    public void onRestoreStarted(final SparrowRestoreManager.RestoreData restoreData) {
        RESTORE_STARTED_CALLED = true;
    }

    @Override
    public void onNotebookDelete() {
        NOTEBOOK_DELETE_CALLED = true;
    }

    @Override
    public void setNotebookListSelection(long id) {
        NOTEBOOK_LIST_SELECTION = id;
    }

}