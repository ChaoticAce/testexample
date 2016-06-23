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
import gov.sparrow.provider.ListPositionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ActionServiceTest {

    @Mock Context context;
    @Mock ContentResolver resolver;
    @Mock SQLiteQueryBuilder builder;
    @Mock SQLiteDatabase db;
    @Mock ListPositionHelper listPositionHelper;
    @Captor ArgumentCaptor<String[]> queryCaptor;
    @Captor ArgumentCaptor<Object[]> actionUpdateCaptor;
    private ActionService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(context.getContentResolver()).thenReturn(resolver);

        subject = new ActionService(listPositionHelper);
    }

    @Test
    public void queryActions_shouldReturnActionCursor() throws Exception {
        Cursor expectedCursor = mock(Cursor.class);
        when(builder.query(
                eq(db),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(expectedCursor);

        Cursor cursor = subject.queryActions(db, builder, new String[]{"projection"}, "selection", new String[]{"args"}, "sort");
        assertThat(cursor).isEqualTo(expectedCursor);

        InOrder inOrder = inOrder(builder);
        inOrder.verify(builder).setTables(Action.TABLE_NOTE_POSITION_JOIN);
        inOrder.verify(builder).query(db, new String[]{"projection"}, "selection", new String[]{"args"}, null, null, "sort");
    }

    @Test
    public void upsertAction_shouldInsertIntoDatabase() {
        ContentValues values = new ContentValues();
        values.put("key", "value");

        when(db.insertWithOnConflict(
                eq(Action.TABLE_NAME),
                isNull(String.class),
                eq(values),
                anyInt())
        ).thenReturn(123l);

        Uri uri = subject.upsertAction(context, db, values);

        assertThat(uri).isEqualTo(Action.CONTENT_URI(123L));
        verify(db).insertWithOnConflict(Action.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void upsertAction_whenActionExists_shouldUpdateDatabase() {
        ContentValues values = new ContentValues();
        values.put(Action.COLUMN_NAME_LINK_ID, "test link id");
        values.put(Action.COLUMN_NAME_NOTE_ID, 2L);

        when(db.update(
                eq(Action.TABLE_NAME),
                eq(values),
                anyString(),
                any(String[].class))
        ).thenReturn(1);

        when(db.insertWithOnConflict(
                eq(Action.TABLE_NAME),
                isNull(String.class),
                eq(values),
                anyInt())
        ).thenReturn(123l);

        Uri uri = subject.upsertAction(context, db, values);
        assertThat(uri).isEqualTo(Action.CONTENT_URI(123L));

        verify(db).update(
                Action.TABLE_NAME,
                values,
                Action.COLUMN_NAME_LINK_ID + "=? AND " + Action.COLUMN_NAME_NOTE_ID + "=?",
                new String[]{"test link id", Long.toString(2L)});
        verify(db).insertWithOnConflict(Action.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void updateAction_shouldUpdateDatabase() {
        ContentValues values = new ContentValues();
        values.put("key", "value");

        when(db.update(eq(Action.TABLE_NAME), eq(values), anyString(), any(String[].class)))
                .thenReturn(777);

        int count = subject.updateAction(context, db, values, "selection", new String[]{"args"});
        assertThat(count).isEqualTo(777);

        verify(db).update(Action.TABLE_NAME, values, "selection", new String[]{"args"});

        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void updateLinkedAction_shouldUpdateAction_andLinkedNote_andLinkedNoteStyles() {
        String actionTitle = "foo";
        String actionId = "1";
        String noteId = "11";

        ContentValues values = new ContentValues();
        values.put(Action.COLUMN_NAME_TITLE, actionTitle);

        when(db.update(eq(Action.TABLE_NAME), eq(values), anyString(), any(String[].class)))
                .thenReturn(777);

        int count = subject.updateLinkedAction(
                context,
                db,
                Action.CONTENT_URI(11L, 1L),
                values,
                "selection",
                new String[]{"args"});
        assertThat(count).isEqualTo(777);

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(db).execSQL(eq(Action.UPDATE_LINKED_NOTE_BODY_SQL), actionUpdateCaptor.capture());
        assertThat(actionUpdateCaptor.getValue()).containsExactly(actionTitle, actionId, noteId);

        inOrder.verify(db).execSQL(eq(Action.UPDATE_NOTE_STYLES_SQL), actionUpdateCaptor.capture());
        assertThat(actionUpdateCaptor.getValue()).containsExactly(actionTitle.length(), actionId, actionTitle.length(), actionId, noteId, actionId);

        inOrder.verify(db).execSQL(eq(Action.UPDATE_LINKED_NOTE_ACTION_LINKS_SQL), actionUpdateCaptor.capture());
        assertThat(actionUpdateCaptor.getValue()).containsExactly(actionTitle.length(), actionId, actionTitle.length(), actionId, noteId, actionId);

        inOrder.verify(db).execSQL(eq(Action.UPDATE_ACTION_LINK_START_END_SQL), actionUpdateCaptor.capture());
        assertThat(actionUpdateCaptor.getValue()).containsExactly(actionTitle.length(), actionId);

        inOrder.verify(db).update(Action.TABLE_NAME, values, "selection", new String[]{"args"});

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        verify(resolver).notifyChange(Note.CONTENT_URI, null);
        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void updateActionListPosition_shouldUpdateActionPosition_forActionId() throws Exception {
        ContentValues values = mock(ContentValues.class);
        values.put("key", "value");

        subject.updateActionListPosition(context, db, values, new String[]{"2"});

        verify(listPositionHelper).update(
                db,
                values,
                ActionListPosition.TABLE_NAME,
                ActionListPosition.COLUMN_NAME_POSITION,
                ActionListPosition.COLUMN_NAME_ACTION_ID,
                new String[]{"2"});

        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void archiveAction_shouldUpdateDatabase() throws Exception {
        ContentValues values = new ContentValues();
        values.put(Action.COLUMN_NAME_ARCHIVED, Boolean.toString(true));

        subject.archiveAction(context, db, "selection", new String[]{"args"});

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(db).update(Action.TABLE_NAME, values, "selection", new String[]{"args"});
        inOrder.verify(db).delete(ActionListPosition.TABLE_NAME, ActionListPosition.COLUMN_NAME_ACTION_ID + "=?", new String[]{"args"});

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void deleteAction_shouldDeleteFromDatabase() {
        subject.deleteActions(db, "test selection", new String[]{"test"});
        verify(db).delete(Action.TABLE_NAME, "test selection", new String[]{"test"});
    }

}