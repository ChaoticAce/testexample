package gov.sparrow.repository;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.StyleContract;
import gov.sparrow.database.NoteMergeCursor;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.models.links.ActionLink;
import gov.sparrow.models.links.StyleLink;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gov.sparrow.repository.NoteRepository.*;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(SparrowTestRunner.class)
public class NoteRepositoryListenerTest {

    private NoteRepositoryListener subject;

    @Before
    public void setUp() {
        subject = new NoteRepositoryListener();
    }

    @Test
    public void onInsertComplete_callsCreateNoteListener() throws Exception {
        NoteRepository.CreateNoteListener listener = mock(NoteRepository.CreateNoteListener.class);
        subject.onInsertComplete(
                CREATE_NOTE_TOKEN,
                listener,
                Uri.parse("content://test.authority/note/123"));
        verify(listener).onCreateNoteComplete(123L);
    }

    @Test
    public void onInsertComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_NOTE_TOKEN,
                    null,
                    Uri.parse("content://test.authority/note/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onInsertComplete_ignoresNullListener() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_NOTE_TOKEN,
                    null,
                    Uri.parse("content://test.authority/note/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onUpdateComplete_callsUpdateNoteListener() throws Exception {
        NoteRepository.UpdateNoteListener listener = mock(NoteRepository.UpdateNoteListener.class);
        subject.onUpdateComplete(
                UPDATE_NOTE_TOKEN,
                listener,
                123);
        verify(listener).onUpdateNoteComplete(123);
    }

    @Test
    public void onUpdateComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onUpdateComplete(
                    UPDATE_NOTE_TOKEN,
                    null,
                    123);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onUpdateComplete_ignoresNullListener() throws Exception {
        try {
            subject.onUpdateComplete(
                    UPDATE_NOTE_TOKEN,
                    null,
                    123);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onDeleteComplete_callsDeleteNoteListener() throws Exception {
        NoteRepository.DeleteNoteListener listener = mock(NoteRepository.DeleteNoteListener.class);
        subject.onDeleteComplete(
                DELETE_NOTE_TOKEN,
                listener,
                123);
        verify(listener).onDeleteNoteComplete();
    }

    @Test
    public void onDeleteComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onDeleteComplete(
                    DELETE_NOTE_TOKEN,
                    null,
                    123);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onDeleteComplete_ignoresNullListener() throws Exception {
        try {
            subject.onDeleteComplete(
                    DELETE_NOTE_TOKEN,
                    null,
                    123);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onQueryComplete_callsQueryNoteListener() throws Exception {
        CursorWrapper cursor = mock(CursorWrapper.class);
        NoteMergeCursor noteMergeCursor = mock(NoteMergeCursor.class);

        when(cursor.getWrappedCursor()).thenReturn(noteMergeCursor);

        when(noteMergeCursor.moveToFirst()).thenReturn(true);
        when(noteMergeCursor.moveToNext()).thenReturn(true);

        when(noteMergeCursor.getColumnIndexOrThrow(NoteContract.Note._ID)).thenReturn(0);
        when(noteMergeCursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_TITLE)).thenReturn(1);
        when(noteMergeCursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_BODY)).thenReturn(2);
        when(noteMergeCursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID)).thenReturn(3);
        when(noteMergeCursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_UPDATED_AT)).thenReturn(4);

        when(noteMergeCursor.getLong(0)).thenReturn(1L);
        when(noteMergeCursor.getString(1)).thenReturn("test title");
        when(noteMergeCursor.getString(2)).thenReturn("test body");
        when(noteMergeCursor.getLong(3)).thenReturn(11L);
        when(noteMergeCursor.getString(4)).thenReturn("test last saved");

        when(noteMergeCursor.getColumnIndexOrThrow(ActionContract.Action.COLUMN_NAME_LINK_ID)).thenReturn(5);
        when(noteMergeCursor.getColumnIndexOrThrow(ActionContract.Action.COLUMN_NAME_LINK_START)).thenReturn(6);
        when(noteMergeCursor.getColumnIndexOrThrow(ActionContract.Action.COLUMN_NAME_LINK_END)).thenReturn(7);
        when(noteMergeCursor.getColumnIndexOrThrow(ActionContract.Action.COLUMN_NAME_COMPLETED)).thenReturn(8);

        when(noteMergeCursor.getString(5)).thenReturn("test action id");
        when(noteMergeCursor.getInt(6)).thenReturn(45);
        when(noteMergeCursor.getInt(7)).thenReturn(67);
        when(noteMergeCursor.getString(8)).thenReturn("false");

        when(noteMergeCursor.getColumnIndexOrThrow(StyleContract.Style.COLUMN_NAME_TYPE)).thenReturn(9);
        when(noteMergeCursor.getColumnIndexOrThrow(StyleContract.Style.COLUMN_NAME_START)).thenReturn(10);
        when(noteMergeCursor.getColumnIndexOrThrow(StyleContract.Style.COLUMN_NAME_END)).thenReturn(11);

        when(noteMergeCursor.getString(9)).thenReturn("test span type");
        when(noteMergeCursor.getInt(10)).thenReturn(1);
        when(noteMergeCursor.getInt(11)).thenReturn(123);

        when(noteMergeCursor.getActionLinkCount()).thenReturn(1);
        when(noteMergeCursor.getStyleCount()).thenReturn(1);

        NoteRepository.QueryNoteListener listener = mock(NoteRepository.QueryNoteListener.class);

        Note expectedNote = NoteBuilder.noteBuilder()
                .id(1L)
                .title("test title")
                .body("test body")
                .notebookId(11L)
                .lastSaved("test last saved")
                .actionLinks(asList(new ActionLink("test action id", 45, 67, false)))
                .styleLinks(asList(new StyleLink("test span type", 1, 123)))
                .build();

        subject.onQueryComplete(
                QUERY_NOTE_TOKEN,
                listener,
                cursor);
        verify(listener).onQueryNoteComplete(expectedNote);
        verify(cursor).close();
    }

    @Test
    public void onQueryComplete_ignoresDeadReference() throws Exception {
        Cursor cursor = mock(Cursor.class);
        try {
            subject.onQueryComplete(
                    QUERY_NOTE_TOKEN,
                    null,
                    cursor);
        } catch (NullPointerException e) {
            Assert.fail();
        }
        verify(cursor).close();
    }

    @Test
    public void onQueryComplete_ignoresNullListener() throws Exception {
        Cursor cursor = mock(Cursor.class);
        try {
            subject.onQueryComplete(
                    QUERY_NOTE_TOKEN,
                    null,
                    cursor);
        } catch (NullPointerException e) {
            Assert.fail();
        }
        verify(cursor).close();
    }

    @Test
    public void onQueryComplete_ignoresNullCursor() throws Exception {
        NoteRepository.QueryNoteListener listener = mock(NoteRepository.QueryNoteListener.class);
        subject.onQueryComplete(QUERY_NOTE_TOKEN, listener, null);
        verify(listener).onQueryNoteComplete(null);
    }

}