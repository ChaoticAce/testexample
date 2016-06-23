package gov.sparrow.datasync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import com.google.gson.Gson;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.*;
import gov.sparrow.models.Action;
import gov.sparrow.models.Note;
import gov.sparrow.models.Notebook;
import gov.sparrow.models.Style;
import gov.sparrow.models.builders.ActionBuilder;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.util.FixtureReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SparrowTestRunner.class)
public class AsyncRestoreTaskTest {

    @Mock ContentResolver contentResolver;
    @Mock SparrowRestoreManager.RestoreTaskListener listener;
    @Captor private ArgumentCaptor<ArrayList<ContentProviderOperation>> operationsCaptor;
    private Gson jsonMapper;
    private AsyncRestoreTask subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        jsonMapper = new Gson();
        subject = new AsyncRestoreTask(contentResolver, jsonMapper, listener);
    }

    @Test
    public void doInBackground_restoresDatabase() throws Exception {
        File fileToParse = FixtureReader.getGzippedFixture("tiny_backup.json");
        subject.doInBackground(new File[]{fileToParse});

        final Notebook notebook = new Notebook(0, "title 0", true);

        final Note note = NoteBuilder.noteBuilder()
                .id(0L)
                .title("title 0")
                .body("body 0")
                .notebookId(0L)
                .archived(false)
                .createdAt("test timestamp 0")
                .lastSaved("updated at 0")
                .build();

        final Action action = ActionBuilder.actionBuilder()
                .id(0L)
                .title("title 0")
                .linkStart(0)
                .linkEnd(0)
                .completed(true)
                .dueDate("due date 0")
                .createdAt("test timestamp 0")
                .checkboxUpdatedAt("checkbox updated at 0")
                .build();

        final Style style = new Style(0L, 0L, "bold 0", 0, 4);

        ArrayList<ContentProviderOperation> expectedOperations = new ArrayList<ContentProviderOperation>() {
            {
                add(ContentProviderOperation.newDelete(StyleContract.Style.CONTENT_URI)
                        .build());
                add(ContentProviderOperation.newDelete(ActionContract.Action.CONTENT_URI)
                        .build());
                add(ContentProviderOperation.newDelete(NoteContract.Note.CONTENT_URI)
                        .build());
                add(ContentProviderOperation.newDelete(NotebookContract.Notebook.CONTENT_URI)
                        .build());
                add(ContentProviderOperation.newInsert(NotebookContract.Notebook.CONTENT_URI)
                        .withValues(notebook.getContentValues())
                        .build());
                add(ContentProviderOperation.newInsert(NoteContract.Note.CONTENT_URI)
                        .withValues(note.getContentValues())
                        .build());
                add(ContentProviderOperation.newInsert(ActionContract.Action.CONTENT_URI)
                        .withValues(action.getContentValues())
                        .build());
                add(ContentProviderOperation.newInsert(StyleContract.Style.CONTENT_URI)
                        .withValues(style.getContentValues())
                        .build());
            }
        };

        verify(contentResolver).applyBatch(eq(SparrowContract.SPARROW_CONTENT_AUTHORITY), operationsCaptor.capture());

        ArrayList<ContentProviderOperation> operations = operationsCaptor.getValue();
        assertThat(operations.size()).isEqualTo(expectedOperations.size());
        for (int i = 0; i < operations.size(); i++) {
            assertThat(operations.get(i).getUri()).isEqualTo(expectedOperations.get(i).getUri());
            assertThat(shadowOf(operations.get(i)).getContentValues())
                    .isEqualTo(shadowOf(expectedOperations.get(i)).getContentValues());
        }
    }

    @Test
    public void onPostExecute_callsListener() throws Exception {
        subject.onPostExecute(null);
        verify(listener).onRestoreTaskComplete();
    }

}