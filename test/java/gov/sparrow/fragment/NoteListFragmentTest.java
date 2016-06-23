package gov.sparrow.fragment;

import android.content.ClipData;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.fakes.RoboCursor;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.NotebookContract;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.listeners.NoteTouchListener;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.util.TestActivity;

import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class NoteListFragmentTest {

    @Mock Cursor cursor;
    @Mock SparrowProvider contentProvider;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    private NoteListFragment subject;

    @Before
    public void setUp() {
        initMocks(this);
        when(contentProvider.insert(any(Uri.class), any(ContentValues.class))).thenReturn(Uri.parse("content://notUsed/1"));
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);
        subject = NoteListFragment.newInstance(142L, 123L);
        startFragment(subject, TestActivity.class);
    }

    @Test
    public void onLoadFinished_whenNoAdapterExists_shouldMakeNewListAdapter() {
        subject.noteList.setAdapter(null);

        subject.onLoadFinished(null, cursor);

        assertThat(subject.noteList.getAdapter()).isNotNull();
        assertThat(((CursorAdapter) subject.noteList.getAdapter()).getCursor()).isEqualTo(cursor);
    }

    @Test
    public void onLoadFinished_shouldSwapNewData() {
        Cursor oldCursor = ((CursorAdapter) subject.noteList.getAdapter()).getCursor();
        assertThat(oldCursor).isNull();

        subject.onLoadFinished(null, cursor);
        assertThat(((CursorAdapter) subject.noteList.getAdapter()).getCursor()).isEqualTo(cursor);
    }

    @Test
    public void onItemSelected_shouldReplaceNotePaneFragment() {
        populateNoteList();

        subject.noteList.performItemClick(null, 1, 0);

        assertThat(TestActivity.ITEM_CLICK_HANDLED).isTrue();
        assertThat(TestActivity.CLICKED_NOTE_ID).isEqualTo(2L);
    }

    @Test
    public void onAddNoteClicked_shouldInsertNewNote() {
        subject.noteListAddButton.performClick();

        assertThat(TestActivity.ADD_NOTE_CLICKED_HANDLED).isTrue();
        assertThat(TestActivity.ADD_NOTE_NOTEBOOK_ID).isEqualTo(142L);
    }

    @Test
    public void afterListFinishesLoading_shouldHighlightLastSavedNote() throws Exception {
        subject = NoteListFragment.newInstance(455L, 2L);
        startFragment(subject, TestActivity.class);

        RoboCursor roboCursor = new RoboCursor();
        roboCursor.setColumnNames(asList(
                NoteContract.Note._ID,
                NoteContract.Note.COLUMN_NAME_TITLE,
                NoteContract.Note.COLUMN_NAME_BODY,
                NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID,
                NoteContract.Note.COLUMN_NAME_ARCHIVED,
                NoteContract.Note.COLUMN_NAME_CREATED_AT,
                NoteContract.Note.COLUMN_NAME_UPDATED_AT));
        roboCursor.setResults(new Object[][]{
                {1L, "title 1", "test body 1", 11L, "true 1", "test created at 1", "test updated at 1"},
                {2L, "title 2", "test body 2", 22L, "true 2", "test created at 2", "test updated at 2"}});
        roboCursor.moveToFirst();

        subject.noteList.setAdapter(null);
        subject.onLoadFinished(mock(Loader.class), roboCursor);

        assertThat(subject.noteList.getCheckedItemIds()).containsOnly(2L);
    }

    @Test
    public void onCreateLoader_whenAllNotebook_selectsAllNonArchivedNotes() throws Exception {
        subject = NoteListFragment.newInstance(NotebookContract.Notebook.UNASSIGNED_NOTEBOOK_ID, 0L);
        startFragment(subject, TestActivity.class);
        Loader<Cursor> result = subject.onCreateLoader(0, null);

        String expectedSelection = NoteContract.Note.TABLE_NAME + "." + NoteContract.Note.COLUMN_NAME_ARCHIVED + "=?";
        assertThat(((CursorLoader) result).getSelection()).isEqualTo(expectedSelection);
        assertThat(((CursorLoader) result).getSelectionArgs()).isEqualTo(new String[]{"false"});
    }

    @Test
    public void onCreateLoader_filtersOutArchivedNotes() throws Exception {
        subject = NoteListFragment.newInstance(123L, 45L);
        startFragment(subject, TestActivity.class);
        Loader<Cursor> result = subject.onCreateLoader(0, null);

        String expectedSelection = NoteContract.Note.TABLE_NAME + "." + NoteContract.Note.COLUMN_NAME_ARCHIVED + "=?" + " AND " +
                NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID + "=?";
        assertThat(((CursorLoader) result).getSelection()).isEqualTo(expectedSelection);
        assertThat(((CursorLoader) result).getSelectionArgs()).isEqualTo(new String[]{"false", "123"});
    }

    @Test
    public void onItemLongClick_shouldCallTouchListener() throws Exception {
        assertThat(subject.noteList.getOnItemLongClickListener()).isInstanceOf(NoteTouchListener.class);
    }

    private void populateNoteList() {
        // Using ArrayAdapters to populate the list because Cursors and CursorAdapters are hard to mock
        Note note0 = NoteBuilder.noteBuilder()
                .id(1L)
                .title("title 1")
                .body("boom 1")
                .build();
        Note note1 = NoteBuilder.noteBuilder()
                .id(2L)
                .title("title 2")
                .body("boom 2")
                .build();
        List<Cursor> cursors = getCursorList(asList(note0, note1));
        ArrayAdapter<Cursor> adapter = new ArrayAdapter<>(subject.getActivity(), R.layout.note_list_item, R.id.note_list_item_title, cursors);
        subject.noteList.setAdapter(adapter);
        shadowOf(subject.noteList).populateItems();
    }

    private List<Cursor> getCursorList(List<Note> notes) {
        List<Cursor> cursors = new ArrayList<>(notes.size());
        for (Note note : notes) {
            cursors.add(getCursorForNote(note));
        }
        return cursors;
    }

    private Cursor getCursorForNote(Note note) {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note._ID)).thenReturn(0);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_TITLE)).thenReturn(1);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_BODY)).thenReturn(2);
        when(cursor.getLong(0)).thenReturn(note.getId());
        when(cursor.getString(1)).thenReturn(note.getTitle());
        when(cursor.getString(2)).thenReturn(note.getBody());
        return cursor;
    }
}
