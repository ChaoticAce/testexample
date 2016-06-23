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
import gov.sparrow.contracts.NotebookContract.Notebook;
import gov.sparrow.contracts.NotebookContract.NotebookListPosition;
import gov.sparrow.provider.ListPositionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import static gov.sparrow.provider.services.NotebookService.ACTION_LIST_POSITION_NOTEBOOK_ID_WHERE_CLAUSE;
import static gov.sparrow.provider.services.NotebookService.ACTION_NOTEBOOK_ID_WHERE_CLAUSE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NotebookServiceTest {

    private static final String[] PROJECTION = {"projection"};
    private static final String SELECTION = "selection";
    private static final String[] SELECTION_ARGS = {"args"};
    public static final String SORT = "sort";

    @Mock Context context;
    @Mock ContentResolver resolver;
    @Mock SQLiteQueryBuilder builder;
    @Mock SQLiteDatabase db;
    @Mock ListPositionHelper listPositionHelper;
    @Mock Cursor cursor;
    @Mock ContentValues values;
    private NotebookService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(context.getContentResolver()).thenReturn(resolver);

        subject = new NotebookService(listPositionHelper);
    }

    @Test
    public void queryNotebook_shouldReturnNotebookCursor() throws Exception {
        when(builder.query(db, PROJECTION, SELECTION, SELECTION_ARGS, null, null, SORT))
                .thenReturn(cursor);

        Cursor expectedCursor = subject.queryNotebooks(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT);
        assertThat(expectedCursor).isEqualTo(cursor);

        InOrder inOrder = inOrder(builder);
        inOrder.verify(builder).setTables(Notebook.TABLE_POSITION_JOIN);
        inOrder.verify(builder).query(db, PROJECTION, SELECTION, SELECTION_ARGS, null, null, SORT);
    }

    @Test
    public void insertNotebook_shouldInsertIntoDatabase() throws Exception {
        when(db.insert(Notebook.TABLE_NAME, null, values))
                .thenReturn(123L);

        Uri uri = subject.insertNotebook(context, db, values);
        assertThat(uri.toString()).isEqualTo(Notebook.CONTENT_URI.toString() + "/123");

        verify(db).insert(Notebook.TABLE_NAME, null, values);

        verify(resolver).notifyChange(Notebook.CONTENT_URI, null);
    }

    @Test
    public void updateNotebook_shouldUpdateDatabase() throws Exception {
        when(db.update(Notebook.TABLE_NAME, values, SELECTION, SELECTION_ARGS))
                .thenReturn(777);

        int count = subject.updateNotebook(context, db, values, SELECTION, SELECTION_ARGS);
        assertThat(count).isEqualTo(777);

        verify(db).update(Notebook.TABLE_NAME, values, SELECTION, SELECTION_ARGS);

        verify(resolver).notifyChange(Notebook.CONTENT_URI, null);
    }

    @Test
    public void updateNotebookListPosition_shouldUpdateNotebookPosition_forNotebookId() throws Exception {
        subject.updateNotebookListPosition(context, db, values, SELECTION_ARGS);

        verify(listPositionHelper).update(
                db,
                values,
                NotebookListPosition.TABLE_NAME,
                NotebookListPosition.COLUMN_NAME_POSITION,
                NotebookListPosition.COLUMN_NAME_NOTEBOOK_ID,
                SELECTION_ARGS
        );

        verify(resolver).notifyChange(Notebook.CONTENT_URI, null);
    }

    @Test
    public void deleteNotebook_shouldDeleteFromDatabase() throws Exception {
        subject.deleteNotebooks(db, SELECTION, SELECTION_ARGS);
        verify(db).delete(Notebook.TABLE_NAME, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void archiveNotebook_whenDeleteAll_shouldUpdateDatabase() throws Exception {
        ContentValues notebookValues = new ContentValues();
        notebookValues.put(Notebook.COLUMN_NAME_ARCHIVED, Boolean.toString(true));

        ContentValues noteValues = new ContentValues();
        noteValues.put(Note.COLUMN_NAME_ARCHIVED, Boolean.toString(true));

        ContentValues actionValues = new ContentValues();
        actionValues.put(Action.COLUMN_NAME_ARCHIVED, Boolean.toString(true));

        String[] args = {"args"};
        subject.archiveNotebook(context, db, true, "selection", args);

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(db).update(Notebook.TABLE_NAME, notebookValues, "selection", args);
        inOrder.verify(db).update(Note.TABLE_NAME, noteValues, Note.COLUMN_NAME_NOTEBOOK_ID + "=?", args);
        inOrder.verify(db).update(Action.TABLE_NAME, actionValues, ACTION_NOTEBOOK_ID_WHERE_CLAUSE, args);

        inOrder.verify(db).delete(NotebookListPosition.TABLE_NAME, NotebookListPosition.COLUMN_NAME_NOTEBOOK_ID + "=?", args);
        inOrder.verify(db).delete(ActionListPosition.TABLE_NAME, ACTION_LIST_POSITION_NOTEBOOK_ID_WHERE_CLAUSE, args);

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        verify(resolver).notifyChange(Notebook.CONTENT_URI, null);
        verify(resolver).notifyChange(Note.CONTENT_URI, null);
        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }

    @Test
    public void archiveNotebook_whenNotDeleteAll_shouldUpdateDatabase() throws Exception {
        ContentValues notebookValues = new ContentValues();
        notebookValues.put(Notebook.COLUMN_NAME_ARCHIVED, Boolean.toString(true));

        ContentValues noteValues = new ContentValues();
        noteValues.put(Note.COLUMN_NAME_NOTEBOOK_ID, Notebook.UNASSIGNED_NOTEBOOK_ID);

        String[] args = {"args"};
        subject.archiveNotebook(context, db, false, "selection", args);

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();

        inOrder.verify(db).update(Notebook.TABLE_NAME, notebookValues, "selection", args);
        inOrder.verify(db).update(Note.TABLE_NAME, noteValues, Note.COLUMN_NAME_NOTEBOOK_ID + "=?", args);

        inOrder.verify(db).delete(NotebookListPosition.TABLE_NAME, NotebookListPosition.COLUMN_NAME_NOTEBOOK_ID + "=?", args);

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();

        verify(resolver).notifyChange(Notebook.CONTENT_URI, null);
        verify(resolver).notifyChange(Note.CONTENT_URI, null);
        verify(resolver).notifyChange(Action.CONTENT_URI, null);
    }
}