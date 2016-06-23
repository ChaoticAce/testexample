package gov.sparrow.provider.services;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.StyleContract.Style;
import gov.sparrow.database.BackupMergeCursor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import static gov.sparrow.contracts.ActionContract.Action;
import static gov.sparrow.contracts.NoteContract.Note;
import static gov.sparrow.contracts.NotebookContract.Notebook;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class BackupServiceTest {

    @Mock private SQLiteDatabase db;
    @Mock private SQLiteQueryBuilder builder;
    private BackupService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = new BackupService();
    }

    @Test
    public void queryBackup_shouldQueryTables() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getCount()).thenReturn(1);

        when(builder.query(
                any(SQLiteDatabase.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(cursor, cursor, cursor, cursor);

        subject.queryBackup(db, builder);

        InOrder inOrder = inOrder(builder, db);

        inOrder.verify(db).beginTransaction();

        inOrder.verify(builder).setTables(Notebook.TABLE_NAME);
        inOrder.verify(builder).query(
                any(SQLiteDatabase.class),
                (String[]) isNull(),
                (String) isNull(),
                (String[]) isNull(),
                (String) isNull(),
                (String) isNull(),
                (String) isNull());

        inOrder.verify(builder).setTables(Note.TABLE_NAME);
        inOrder.verify(builder).query(
                any(SQLiteDatabase.class),
                (String[]) isNull(),
                (String) isNull(),
                (String[]) isNull(),
                (String) isNull(),
                (String) isNull(),
                (String) isNull());

        inOrder.verify(builder).setTables(Action.TABLE_NAME);
        inOrder.verify(builder).query(
                any(SQLiteDatabase.class),
                (String[]) isNull(),
                (String) isNull(),
                (String[]) isNull(),
                (String) isNull(),
                (String) isNull(),
                (String) isNull());

        inOrder.verify(builder).setTables(Style.TABLE_NAME);
        inOrder.verify(builder).query(
                any(SQLiteDatabase.class),
                (String[]) isNull(),
                (String) isNull(),
                (String[]) isNull(),
                (String) isNull(),
                (String) isNull(),
                (String) isNull());

        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();
    }

    @Test
    public void queryBackup_shouldReturnMergeCursor() throws Exception {
        Cursor notebookCursor = mock(Cursor.class);
        when(notebookCursor.getCount()).thenReturn(4);

        Cursor noteCursor = mock(Cursor.class);
        when(noteCursor.getCount()).thenReturn(5);

        Cursor actionCursor = mock(Cursor.class);
        when(actionCursor.getCount()).thenReturn(3);

        Cursor styleCursor = mock(Cursor.class);
        when(styleCursor.getCount()).thenReturn(10);

        when(builder.query(
                any(SQLiteDatabase.class),
                (String[]) any(),
                anyString(),
                (String[]) any(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(notebookCursor, noteCursor, actionCursor, styleCursor);

        BackupMergeCursor cursor = (BackupMergeCursor) subject.queryBackup(db, builder);

        assertThat(cursor.getCount()).isEqualTo(22);
        assertThat(cursor.getActionCount()).isEqualTo(3);
        assertThat(cursor.getNotebookCount()).isEqualTo(4);
        assertThat(cursor.getNoteCount()).isEqualTo(5);
        assertThat(cursor.getStyleCount()).isEqualTo(10);
    }


}