package gov.sparrow.provider.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract.Action;
import gov.sparrow.contracts.ActionContract.ActionListPosition;
import gov.sparrow.contracts.NoteContract.Note;
import gov.sparrow.contracts.StyleContract;
import gov.sparrow.database.NoteMergeCursor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NoteServiceTest {

    @Mock Context context;
    @Mock ContentResolver resolver;
    @Mock SQLiteQueryBuilder builder;
    @Mock SQLiteDatabase db;
    @Captor ArgumentCaptor<String[]> queryCaptor;
    private NoteService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(context.getContentResolver()).thenReturn(resolver);

        subject = new NoteService();
    }

    @Test
    public void queryNote_shouldReturnNoteCursor() throws Exception {
        Cursor noteCursor = mock(Cursor.class);
        when(noteCursor.getCount()).thenReturn(20).thenReturn(34);

        when(builder.query(
                any(SQLiteDatabase.class),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(noteCursor);

        Cursor cursor = subject.queryNote(db, builder, null, Note._ID + "=?", new String[]{"1"}, null);

        InOrder inOrder = inOrder(db, builder);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(builder).setTables(Note.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(Note._ID + "=?"), eq(new String[]{"1"}), anyString(), anyString(), anyString());

        inOrder.verify(builder).setTables(Action.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(Action.COLUMN_NAME_NOTE_ID + "=? AND " + Action.COLUMN_NAME_ARCHIVED + "='false'"), eq(new String[]{"1"}), anyString(), anyString(), anyString());

        inOrder.verify(builder).setTables(StyleContract.Style.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(StyleContract.Style.COLUMN_NAME_NOTE_ID + "=?"), eq(new String[]{"1"}), anyString(), anyString(), anyString());

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        assertThat(cursor).isInstanceOf(NoteMergeCursor.class);

        NoteMergeCursor noteMergeCursor = (NoteMergeCursor) cursor;
        assertThat(noteMergeCursor.getActionLinkCount()).isEqualTo(20);
        assertThat(noteMergeCursor.getStyleCount()).isEqualTo(34);
    }

    @Test
    public void queryLastSavedNote_shouldReturnNoteCursor() throws Exception {
        Cursor noteCursor = mock(Cursor.class);
        when(noteCursor.moveToFirst()).thenReturn(true);
        when(noteCursor.getColumnIndexOrThrow(Note._ID)).thenReturn(0);
        when(noteCursor.getString(0)).thenReturn("1");
        when(noteCursor.getCount()).thenReturn(20).thenReturn(34);

        when(builder.query(
                any(SQLiteDatabase.class),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(noteCursor);

        Cursor cursor = subject.queryLastSavedNote(db, builder, null, Note._ID + "=?", new String[]{"124"}, null);

        InOrder inOrder = inOrder(builder);
        inOrder.verify(builder).setTables(Note.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(Note._ID + "=?"), eq(new String[]{"124"}), anyString(), anyString(), anyString());

        inOrder.verify(builder).setTables(Action.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(Action.COLUMN_NAME_NOTE_ID + "=? AND " + Action.COLUMN_NAME_ARCHIVED + "='false'"), eq(new String[]{"1"}), anyString(), anyString(), anyString());

        inOrder.verify(builder).setTables(StyleContract.Style.TABLE_NAME);
        inOrder.verify(builder).query(eq(db), any(String[].class), eq(StyleContract.Style.COLUMN_NAME_NOTE_ID + "=?"), eq(new String[]{"1"}), anyString(), anyString(), anyString());

        assertThat(cursor).isInstanceOf(NoteMergeCursor.class);

        NoteMergeCursor noteMergeCursor = (NoteMergeCursor) cursor;
        assertThat(noteMergeCursor.getActionLinkCount()).isEqualTo(20);
        assertThat(noteMergeCursor.getStyleCount()).isEqualTo(34);
    }

    @Test
    public void queryLastSavedNote_whenNoNoteExists_shouldReturnNull() throws Exception {
        Cursor noteCursor = mock(Cursor.class);
        when(noteCursor.moveToFirst()).thenReturn(false);

        when(builder.query(
                any(SQLiteDatabase.class),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(noteCursor);

        Cursor cursor = subject.queryLastSavedNote(db, builder, null, Note._ID + "=?", new String[]{"124"}, null);
        assertThat(cursor).isNull();;
    }

    @Test
    public void queryNotes_shouldReturnCursor() throws Exception {
        Cursor expectedCursor = mock(Cursor.class);
        when(builder.query(
                any(SQLiteDatabase.class),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(expectedCursor);

        Cursor cursor = subject.queryNotes(db, builder, new String[]{"projection"}, "selection", new String[]{"args"}, "sort");
        assertThat(cursor).isEqualTo(expectedCursor);

        InOrder inOrder = inOrder(builder);
        inOrder.verify(builder).setTables(Note.TABLE_NOTEBOOK_JOIN);
        inOrder.verify(builder).query(db, new String[]{"projection"}, "selection", new String[]{"args"}, null, null, "sort");
    }

    @Test
    public void insertNote_shouldInsertIntoDatabase() {
        when(db.insert(anyString(), anyString(), any(ContentValues.class)))
                .thenReturn(123L);

        ContentValues values = new ContentValues();
        values.put("title", "fakeTitle");

        Uri uri = subject.insertNote(context, db, values);

        assertThat(uri).isNotNull();
        assertThat(uri.toString()).isEqualTo(Note.CONTENT_URI.toString() + "/123");

        verify(db).insert(Note.TABLE_NAME, null, values);
        verify(resolver).notifyChange(Note.CONTENT_URI, null);
    }

    @Test
    public void updateNote_shouldUpdateDatabase() {
        when(db.update(anyString(), any(ContentValues.class), anyString(), any(String[].class)))
                .thenReturn(777);

        ContentValues values = new ContentValues();
        values.put("title", "fakeTitle");

        int count = subject.updateNote(context, db, values, "selection", new String[]{"args"});

        assertThat(count).isEqualTo(777);

        verify(db).update(Note.TABLE_NAME, values, "selection", new String[]{"args"});
        verify(resolver).notifyChange(Note.CONTENT_URI, null);
    }

    @Test
    public void archiveNote_shouldUpdateDatabase() throws Exception {
        when(db.delete(anyString(), anyString(), any(String[].class))).thenReturn(10);
        when(db.update(eq(Action.TABLE_NAME), any(ContentValues.class), anyString(), any(String[].class))).thenReturn(20);
        when(db.update(eq(Note.TABLE_NAME), any(ContentValues.class), anyString(), any(String[].class))).thenReturn(30);

        String selection = "selection";
        String[] selectionArgs = {"1"};

        int count = subject.archiveNote(context, db, selection, selectionArgs);
        assertThat(count).isEqualTo(60);

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(db).delete(ActionListPosition.TABLE_NAME, NoteService.ACTION_LIST_POSITION_WHERE_CLAUSE, new String[]{"1"});

        ContentValues actionValues = new ContentValues();
        actionValues.put(Action.COLUMN_NAME_ARCHIVED, Boolean.TRUE.toString());
        inOrder.verify(db).update(Action.TABLE_NAME, actionValues, Action.COLUMN_NAME_NOTE_ID + "=?", new String[]{"1"});

        ContentValues noteValues = new ContentValues();
        noteValues.put(Note.COLUMN_NAME_ARCHIVED, Boolean.TRUE.toString());
        inOrder.verify(db).update(Note.TABLE_NAME, noteValues, selection, selectionArgs);

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        verify(resolver).notifyChange(Note.CONTENT_URI, null);
        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void deleteNotes_shouldDeleteFromDatabase() throws Exception {
        when(db.delete(anyString(), anyString(), any(String[].class))).thenReturn(123);
        assertThat(subject.deleteNotes(db, "test selection", new String[]{"test"})).isEqualTo(123);
        verify(db).delete(Note.TABLE_NAME, "test selection", new String[]{"test"});
    }

}