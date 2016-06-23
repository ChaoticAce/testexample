package gov.sparrow.database;


import android.database.MatrixCursor;
import android.support.annotation.NonNull;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.NotebookContract;
import gov.sparrow.contracts.StyleContract;
import gov.sparrow.datasync.SyncAdapter;
import gov.sparrow.util.FixtureReader;
import org.apache.maven.artifact.ant.shaded.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class SparrowDatabaseBackupWriterTest {

    private SparrowDatabaseBackupWriter subject;
    private final String sparrowDirectory = "sparrowDirectory";
    private final String sparrowDirectoryPath = android.os.Environment.getExternalStorageDirectory() + "/" + sparrowDirectory;

    private final String[] actionColumnNames = new String[]{
            ActionContract.Action._ID,
            ActionContract.Action.COLUMN_NAME_TITLE,
            ActionContract.Action.COLUMN_NAME_NOTE_ID,
            ActionContract.Action.COLUMN_NAME_LINK_ID,
            ActionContract.Action.COLUMN_NAME_LINK_START,
            ActionContract.Action.COLUMN_NAME_LINK_END,
            ActionContract.Action.COLUMN_NAME_COMPLETED,
            ActionContract.Action.COLUMN_NAME_ARCHIVED,
            ActionContract.Action.COLUMN_NAME_DUE_DATE,
            ActionContract.Action.COLUMN_NAME_CREATED_AT,
            ActionContract.Action.COLUMN_NAME_CHECKBOX_UPDATED_AT
    };

    private final String[] notesColumnNames = new String[]{
            NoteContract.Note._ID,
            NoteContract.Note.COLUMN_NAME_TITLE,
            NoteContract.Note.COLUMN_NAME_BODY,
            NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID,
            NoteContract.Note.COLUMN_NAME_ARCHIVED,
            NoteContract.Note.COLUMN_NAME_CREATED_AT,
            NoteContract.Note.COLUMN_NAME_UPDATED_AT
    };

    private final String[] notebookColumnNames = new String[]{
            NotebookContract.Notebook._ID,
            NotebookContract.Notebook.COLUMN_NAME_TITLE,
            NotebookContract.Notebook.COLUMN_NAME_ARCHIVED
    };

    private final String[] styleColumnNames = new String[]{
            StyleContract.Style._ID,
            StyleContract.Style.COLUMN_NAME_NOTE_ID,
            StyleContract.Style.COLUMN_NAME_TYPE,
            StyleContract.Style.COLUMN_NAME_START,
            StyleContract.Style.COLUMN_NAME_END
    };

    @Before
    public void setUp() throws Exception {
        subject = new SparrowDatabaseBackupWriter();
        File directory = new File(sparrowDirectoryPath);
        directory.mkdir();
    }

    @Test
    public void writeBackupToFile_shouldWriteEmptyArrayIfCursorEmpty() throws Exception {
        MatrixCursor notebooksCursor = new MatrixCursor(notebookColumnNames, 0);
        MatrixCursor notesCursor = new MatrixCursor(notesColumnNames, 0);
        MatrixCursor actionsCursor = new MatrixCursor(actionColumnNames, 0);
        MatrixCursor stylesCursor = new MatrixCursor(styleColumnNames, 0);

        BackupMergeCursor cursor = new BackupMergeCursor(notebooksCursor, notesCursor, actionsCursor, stylesCursor);

        String testFileName = "testFileName";
        subject.backupToFile(cursor, new File(
                SyncAdapter.makePath(
                        sparrowDirectoryPath,
                        testFileName)));

        assertThatFileContentsAreEqual(sparrowDirectoryPath + "/" + testFileName, FixtureReader.TEST_FIXTURES_PATH + "empty_backup.json");

    }

    @Test
    public void writeBackupToFile_shouldWriteCursorDataToFile() throws Exception {
        MatrixCursor notebooksCursor = buildNotebookCursor();
        MatrixCursor notesCursor = buildNotesCursor();
        MatrixCursor actionsCursor = buildActionsCursor();
        MatrixCursor stylesCursor = buildStyleCursor();

        BackupMergeCursor cursor = new BackupMergeCursor(notebooksCursor, notesCursor, actionsCursor, stylesCursor);

        String testFileName = "testFileName";
        subject.backupToFile(cursor, new File(
                SyncAdapter.makePath(
                        sparrowDirectoryPath,
                        testFileName)));

        assertThatFileContentsAreEqual(sparrowDirectoryPath + "/" + testFileName, FixtureReader.TEST_FIXTURES_PATH + "backup.json");
    }

    @After
    public void tearDown() throws Exception {
        File file = new File(sparrowDirectoryPath);
        FileUtils.deleteDirectory(file);
    }

    @NonNull
    private MatrixCursor buildActionsCursor() {
        int numOfActions = 4;
        MatrixCursor cursor = new MatrixCursor(actionColumnNames, numOfActions);

        for (int i = 0; i < numOfActions; i++) {
            boolean hasNoNote = i % 3 == 0;
            cursor.addRow(new Object[]{
                    Long.valueOf(i), //id
                    "title " + i, // title
                    hasNoNote ? null : 10 + i, //note id
                    hasNoNote ? null : "3" + i, // link id
                    hasNoNote ? 0 : i + 1, //start
                    hasNoNote ? 0 : i + 2, //end
                    i % 2 == 0 ? "true" : "false", //completed
                    i % 2 == 0 ? "false" : "true", //archived
                    "due date " + i, // due date
                    "test timestamp " + i, //created at
                    "checkbox updated at " + i //checkbox updated at
            });
        }
        return cursor;
    }

    @NonNull
    private MatrixCursor buildNotesCursor() {
        int numOfNotes = 5;
        MatrixCursor cursor = new MatrixCursor(notesColumnNames, numOfNotes);

        for (int i = 0; i < numOfNotes; i++) {
            cursor.addRow(new Object[]{
                    Long.valueOf(i),
                    "title " + i,
                    "body " + i,
                    10 + i,
                    i % 2 == 0 ? "true" : "false",
                    "test timestamp " + i,
                    "updated at " + i
            });
        }
        return cursor;
    }

    @NonNull
    private MatrixCursor buildNotebookCursor() {
        int numOfNotebooks = 7;
        MatrixCursor cursor = new MatrixCursor(notebookColumnNames, numOfNotebooks);

        for (int i = 0; i < numOfNotebooks; i++) {
            Boolean archived = i < 3 ? Boolean.TRUE : Boolean.FALSE;
            cursor.addRow(new Object[]{
                    Long.valueOf(i),
                    "title " + i,
                    archived
            });
        }
        return cursor;
    }

    private MatrixCursor buildStyleCursor() {
        int numOfStyles = 3;
        MatrixCursor cursor = new MatrixCursor(styleColumnNames, numOfStyles);

        for (int i = 0; i < numOfStyles; i++) {
            cursor.addRow(new Object[]{
                    Long.valueOf(i),
                    0L,
                    "bold " + i,
                    i * 2,
                    (i + 1) * 4
            });
        }
        return cursor;
    }

    private void assertThatFileContentsAreEqual(String actualFilePath, String expectedFilePath) throws IOException {
        String result = FixtureReader.readGZIPFile(actualFilePath);
        String expectedFileContents = FixtureReader.readFile(expectedFilePath);
        assertThat(result).isEqualTo(expectedFileContents);
    }

}