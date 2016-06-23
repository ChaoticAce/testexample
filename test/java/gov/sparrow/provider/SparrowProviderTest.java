package gov.sparrow.provider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.ActionContract.Action;
import gov.sparrow.contracts.ActionContract.ActionListPosition;
import gov.sparrow.contracts.BackupContract;
import gov.sparrow.contracts.NoteContract.Note;
import gov.sparrow.contracts.NotebookContract.Notebook;
import gov.sparrow.contracts.NotebookContract.NotebookListPosition;
import gov.sparrow.contracts.SearchContract.Searchable;
import gov.sparrow.contracts.StyleContract.Style;
import gov.sparrow.database.SparrowDatabaseHelper;
import gov.sparrow.provider.services.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;
import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SparrowProviderTest {

    public static final String[] PROJECTION = new String[]{"projection"};
    public static final String SELECTION = "selection";
    public static final String[] SELECTION_ARGS = new String[]{"args"};
    public static final String SORT_ORDER = "sort";

    @Rule public final ExpectedException exception = ExpectedException.none();

    @Inject SparrowDatabaseHelper sparrowDatabaseHelper;
    @Inject SQLiteQueryBuilderFactory sqLiteQueryBuilderFactory;
    @Inject NotebookService notebookService;
    @Inject NoteService noteService;
    @Inject ActionService actionService;
    @Inject StyleService styleService;
    @Inject SearchService searchService;
    @Inject BackupService backupService;

    @Mock SQLiteQueryBuilder builder;
    @Mock Cursor expectedCursor;
    @Mock SQLiteDatabase db;
    @Mock ContentValues values;

    private SparrowProvider subject;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(notebookService.queryNotebooks(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(notebookService.insertNotebook(any(Context.class), eq(db), any(ContentValues.class)))
                .thenReturn(Action.CONTENT_URI(777L));

        when(noteService.queryNotes(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(noteService.queryNote(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(noteService.queryLastSavedNote(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(noteService.insertNote(any(Context.class), eq(db), any(ContentValues.class)))
                .thenReturn(Note.CONTENT_URI(777L));

        when(actionService.queryActions(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(actionService.upsertAction(any(Context.class), eq(db), any(ContentValues.class)))
                .thenReturn(Action.CONTENT_URI(777L));

        when(styleService.queryStyles(
                eq(db),
                any(SQLiteQueryBuilder.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString()
        )).thenReturn(expectedCursor);

        when(searchService.query(
                eq(db),
                any(SQLiteQueryBuilder.class),
                anyString(),
                anyString()
        )).thenReturn(expectedCursor);

        when(backupService.queryBackup(eq(db), any(SQLiteQueryBuilder.class)))
                .thenReturn(expectedCursor);

        subject = new SparrowProvider();
        subject.onCreate();

        when(sparrowDatabaseHelper.getWritableDatabase())
                .thenReturn(db);

        when(sqLiteQueryBuilderFactory.getInstance())
                .thenReturn(builder);
    }

    @Test
    public void query_withMatch_shouldSetNotificationUri() throws Exception {
        Uri[] uris = {
                Notebook.CONTENT_URI,
                Note.CONTENT_URI(1L),
                Note.CONTENT_URI(2L, true),
                Note.CONTENT_URI,
                Action.CONTENT_URI,
                Style.CONTENT_URI,
                Searchable.CONTENT_URI,
                BackupContract.BACKUP_URI
        };

        for (Uri uri : uris) {
            subject.query(uri, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
            verify(expectedCursor).setNotificationUri(subject.getContext().getContentResolver(), uri);
        }
    }

    @Test
    public void queryNoMatch_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        subject.query(Uri.parse("content://somefake.uri/bogus"), null, null, null, null, null);
        verifyZeroInteractions(db);
    }

    @Test
    public void query_shouldUseANewSQLiteQueryBuilderEachTime() {
        SQLiteQueryBuilder[] sqLiteQueryBuilders = new SQLiteQueryBuilder[4];

        for (int i = 0; i < sqLiteQueryBuilders.length; i++) {
            sqLiteQueryBuilders[i] = mock(SQLiteQueryBuilder.class);

            when(sqLiteQueryBuilders[i].query(
                    eq(db),
                    (String[]) any(),
                    anyString(),
                    (String[]) any(),
                    anyString(),
                    anyString(),
                    anyString()
            )).thenReturn(expectedCursor);
        }

        when(sqLiteQueryBuilderFactory.getInstance()).thenReturn(
                sqLiteQueryBuilders[0],
                sqLiteQueryBuilders[1],
                sqLiteQueryBuilders[2],
                sqLiteQueryBuilders[3]);

        for (Uri contentUri : asList(Note.CONTENT_URI, Note.CONTENT_URI, Notebook.CONTENT_URI, Action.CONTENT_URI)) {
            subject.query(contentUri, null, null, null, null, null);
        }

        verify(noteService).queryNotes(eq(db), eq(sqLiteQueryBuilders[0]), any(String[].class), anyString(), any(String[].class), anyString());
        verify(noteService).queryNotes(eq(db), eq(sqLiteQueryBuilders[1]), any(String[].class), anyString(), any(String[].class), anyString());
        verify(notebookService).queryNotebooks(eq(db), eq(sqLiteQueryBuilders[2]), any(String[].class), anyString(), any(String[].class), anyString());
        verify(actionService).queryActions(eq(db), eq(sqLiteQueryBuilders[3]), any(String[].class), anyString(), any(String[].class), anyString());
    }

    @Test
    public void insertNoMatch_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        subject.insert(Uri.parse("fakeUri"), null);
        verifyZeroInteractions(db);
    }

    @Test
    public void updateNoMatch_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        subject.update(Uri.parse("content://a.fake.uri/fake"), null, null, null);
        verifyZeroInteractions(db);
    }

    @Test
    public void deleteNoMatch_shouldThrowException() throws Exception {
        exception.expect(IllegalArgumentException.class);
        subject.delete(Uri.parse("content://a.fake.uri/fake"), null, null);
        verifyZeroInteractions(db);
    }

    @Test
    public void applyBatch_runsAllOperationsInOneTransaction() throws Exception {
        ContentValues noteContentValues = new ContentValues();
        noteContentValues.put("title", "fakeTitle");

        ContentValues actionContentValues = new ContentValues();
        actionContentValues.put("completed", "true");

        ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList();
        contentProviderOperations.addAll(asList(
                ContentProviderOperation
                        .newInsert(Note.CONTENT_URI)
                        .withValues(noteContentValues)
                        .build(),
                ContentProviderOperation
                        .newInsert(Action.CONTENT_URI)
                        .withValues(actionContentValues)
                        .build()));

        subject.applyBatch(contentProviderOperations);

        InOrder inOrder = inOrder(db, noteService, actionService);
        inOrder.verify(db).beginTransaction();
        inOrder.verify(noteService).insertNote(subject.getContext(), db, noteContentValues);
        inOrder.verify(actionService).upsertAction(subject.getContext(), db, actionContentValues);
        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();
    }

    @Test
    public void queryNoteIdMatch_callsNoteService_queryNote() throws Exception {
        subject.query(Note.CONTENT_URI(123L), PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(noteService).queryNote(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void queryLastSavedNoteMatch_callsNoteService_queryLastSavedNote() throws Exception {
        subject.query(Note.CONTENT_URI(2L, true), PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(noteService).queryLastSavedNote(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void queryLastSavedNoteMatch_whenNotLastSaved_shouldThrowException() throws Exception {
        exception.expect(IllegalArgumentException.class);
        subject.query(Note.CONTENT_URI(2L, false), PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
        verifyZeroInteractions(db);
    }

    @Test
    public void queryNoteMatch_callsNoteService_queryNotes() throws Exception {
        subject.query(Note.CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(noteService).queryNotes(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void updateNoteMatch_callsNoteService_updateNote() throws Exception {
        subject.update(Note.CONTENT_URI, values, SELECTION, SELECTION_ARGS);

        verify(noteService).updateNote(subject.getContext(), db, values, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void insertNoteMatch_callsNoteService_insertNote() throws Exception {
        subject.insert(Note.CONTENT_URI, values);

        verify(noteService).insertNote(subject.getContext(), db, values);
    }

    @Test
    public void deleteNoteIdMatch_callsNoteService_archiveNote() throws Exception {
        subject.delete(Note.CONTENT_URI(123L), SELECTION, SELECTION_ARGS);

        verify(noteService).archiveNote(subject.getContext(), db, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void deleteNoteMatch_callsNoteService_deleteNotes() throws Exception {
        subject.delete(Note.CONTENT_URI, SELECTION, SELECTION_ARGS);

        verify(noteService).deleteNotes(db, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void queryActionMatch_callsActionService_queryAction() throws Exception {
        subject.query(Action.CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(actionService).queryActions(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void insertActionMatch_callsActionService_upsertAction() throws Exception {
        subject.insert(Action.CONTENT_URI, values);

        verify(actionService).upsertAction(subject.getContext(), db, values);
    }

    @Test
    public void updateLinkedActionMatch_callsActionService_updateLinkedAction() throws Exception {
        ContentValues actionValues = new ContentValues();
        actionValues.put(Action.COLUMN_NAME_TITLE, "title");

        Uri uri = Action.CONTENT_URI(1L, 2L);

        subject.update(uri, actionValues, SELECTION, SELECTION_ARGS);

        verify(actionService).updateLinkedAction(subject.getContext(), db, uri, actionValues, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void updateActionMatch_callsActionService_updateAction() throws Exception {
        subject.update(Action.CONTENT_URI, values, SELECTION, new String[]{"args"});

        verify(actionService).updateAction(subject.getContext(), db, values, SELECTION, new String[]{"args"});
    }

    @Test
    public void updateActionListPositionMatch_callsActionService_updateActionListPosition() throws Exception {
        subject.update(ActionListPosition.CONTENT_URI, values, SELECTION, new String[]{"args"});

        verify(actionService).updateActionListPosition(subject.getContext(), db, values, new String[]{"args"});
    }

    @Test
    public void deleteActionIdMatch_callsActionService_archiveAction() throws Exception {
        subject.delete(Action.CONTENT_URI(1L), SELECTION, SELECTION_ARGS);

        verify(actionService).archiveAction(subject.getContext(), db, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void deleteActionMatch_callsActionService_deleteActions() {
        subject.delete(Action.CONTENT_URI, SELECTION, new String[]{"args"});

        verify(actionService).deleteActions(db, SELECTION, new String[]{"args"});
    }

    @Test
    public void queryNotebookMatch_callsNotebookService_queryNotebooks() throws Exception {
        subject.query(Notebook.CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(notebookService).queryNotebooks(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void insertNotebookMatch_callsNotebookService_insertNotebook() throws Exception {
        subject.insert(Notebook.CONTENT_URI, values);

        verify(notebookService).insertNotebook(subject.getContext(), db, values);
    }

    @Test
    public void updateNotebookMatch_callsNotebookService_updateNotebook() throws Exception {
        subject.update(Notebook.CONTENT_URI, values, SELECTION, new String[]{"args"});

        verify(notebookService).updateNotebook(subject.getContext(), db, values, SELECTION, new String[]{"args"});
    }

    @Test
    public void updateNotebookListPositionMatch_callsNotebookService_updateNotebookListPosition() throws Exception {
        ContentValues values = new ContentValues();
        values.put("key", "value");

        subject.update(NotebookListPosition.CONTENT_URI, values, SELECTION, new String[]{"args"});

        verify(notebookService).updateNotebookListPosition(subject.getContext(), db, values, new String[]{"args"});
    }

    @Test
    public void deleteNotebookMatch_callsNotebookService_deleteNotebooks() throws Exception {
        subject.delete(Notebook.CONTENT_URI, SELECTION, SELECTION_ARGS);

        verify(notebookService).deleteNotebooks(db, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void deleteNotebookIdMatch_callsNotebookService_archiveNotebook_withDeleteAll() throws Exception {
        subject.delete(Notebook.ARCHIVE_URI(1L, true), SELECTION, SELECTION_ARGS);

        verify(notebookService).archiveNotebook(subject.getContext(), db, true, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void deleteNotebookIdMatch_callsNotebookService_archiveNotebook_withoutDeleteAll() throws Exception {
        subject.delete(Notebook.ARCHIVE_URI(1L, false), SELECTION, SELECTION_ARGS);

        verify(notebookService).archiveNotebook(subject.getContext(), db, false, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void queryStyleMatch_callsStyleService_queryStyles() throws Exception {
        subject.query(Style.CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(styleService).queryStyles(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
    }

    @Test
    public void insertStyleMatch_callsStyleService_insertStyle() throws Exception {
        subject.insert(Style.CONTENT_URI, values);

        verify(styleService).insertStyles(subject.getContext(), db, values);
    }

    @Test
    public void updateStyleMatch_callsStyleService_updateStyle() throws Exception {
        subject.update(Style.CONTENT_URI, values, SELECTION, SELECTION_ARGS);

        verify(styleService).updateStyle(subject.getContext(), db, values, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void deleteStyleMatch_callsStyleService_deleteStyles() throws Exception {
        subject.delete(Style.CONTENT_URI, SELECTION, SELECTION_ARGS);

        verify(styleService).deleteStyles(db, SELECTION, SELECTION_ARGS);
    }

    @Test
    public void querySearchMatch_callsSearchService_query() throws Exception {
        subject.query(Searchable.CONTENT_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);

        verify(searchService).query(db, builder, SELECTION_ARGS[0], SORT_ORDER);
    }

    @Test
    public void querySearchMatch_shouldIgnoreEmptySearch() throws Exception {
        Cursor cursor = subject.query(Searchable.CONTENT_URI, PROJECTION, SELECTION, new String[]{}, SORT_ORDER);
        assertThat(cursor).isNull();

        verifyZeroInteractions(searchService);
    }

    @Test
    public void queryBackupMatch_callsBackupService_queryBackup() throws Exception {
        subject.query(BackupContract.BACKUP_URI, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
        verify(backupService).queryBackup(db, builder);
    }

}
