package gov.sparrow.repository;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.contracts.StyleContract;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.models.links.ActionLink;
import gov.sparrow.models.links.StyleLink;
import gov.sparrow.provider.SparrowProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentProviderOperation;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.Collections;

import static gov.sparrow.contracts.NotebookContract.Notebook;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NoteRepositoryTest {

    private final Note testNote = NoteBuilder.noteBuilder()
            .id(1L)
            .title("test title")
            .body("test body")
            .notebookId(11L)
            .lastSaved("test update time")
            .actionLinks(asList(new ActionLink("123", 0, 2, false)))
            .styleLinks(asList(new StyleLink(StyleLink.BOLD_STYLE, 3, 5)))
            .build();

    @Mock SparrowProvider contentProvider;
    @Mock AsyncRepositoryHelper asyncRepositoryHelper;
    @Captor ArgumentCaptor<Object> listenerCaptor;
    @Captor ArgumentCaptor<ArrayList<ContentProviderOperation>> operationsCaptor;
    private NoteRepository subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);

        when(contentProvider.update(
                any(Uri.class),
                any(ContentValues.class),
                anyString(),
                any(String[].class)
        )).thenReturn(1);

        when(contentProvider.applyBatch(any(ArrayList.class)))
                .thenReturn(new ContentProviderResult[2]);

        subject = new NoteRepository(RuntimeEnvironment.application.getContentResolver(), asyncRepositoryHelper);
    }

    @Test
    public void asyncCreateNote_startsAsyncInsert() {
        NoteRepository.CreateNoteListener listener = mock(NoteRepository.CreateNoteListener.class);

        subject.asyncCreateNote(2L, "test title", "test body", listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID, 2L);
        expectedValues.put(NoteContract.Note.COLUMN_NAME_TITLE, "test title");
        expectedValues.put(NoteContract.Note.COLUMN_NAME_BODY, "test body");

        verify(asyncRepositoryHelper).startInsert(
                eq(NoteRepository.CREATE_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI),
                eq(expectedValues));
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void asyncArchiveNote_startsAsyncDelete() {
        NoteRepository.DeleteNoteListener listener = mock(NoteRepository.DeleteNoteListener.class);
        ContentValues expectedValues = new ContentValues();
        expectedValues.put(NoteContract.Note.COLUMN_NAME_ARCHIVED, "true");

        subject.asyncArchiveNote(testNote.getId(), listener);

        verify(asyncRepositoryHelper).startDelete(
                eq(NoteRepository.DELETE_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI(testNote.getId())),
                eq(NoteContract.Note._ID + "=?"),
                eq(new String[]{Long.toString(testNote.getId())})
        );
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void asyncGetNote_startsAsyncQuery() {
        NoteRepository.QueryNoteListener listener = mock(NoteRepository.QueryNoteListener.class);

        subject.asyncGetNote(1L, listener);
        verify(asyncRepositoryHelper).startQuery(
                eq(NoteRepository.QUERY_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI(1L)),
                isNull(String[].class),
                eq(NoteRepository.SELECTION_BY_ID),
                eq(new String[]{"1"}),
                isNull(String.class));
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void asyncGetLastSavedNote_startsAsyncQuery() throws Exception {
        NoteRepository.QueryNoteListener listener = mock(NoteRepository.QueryNoteListener.class);

        subject.asyncGetLastSavedNote(2L, listener);
        verify(asyncRepositoryHelper).startQuery(
                eq(NoteRepository.QUERY_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI(2L, true)),
                isNull(String[].class),
                eq(NoteContract.Note.TABLE_NAME + "." + NoteContract.Note.COLUMN_NAME_ARCHIVED + "=?" + " AND " +
                        NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID + "=?"),
                eq(new String[]{"false", Long.toString(2L)}),
                eq(NoteContract.Note.COLUMN_NAME_UPDATED_AT + " DESC LIMIT 1"));
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void asyncGetLastSavedNote_whenAllNotebookGiven_startsAsyncQuery() throws Exception {
        NoteRepository.QueryNoteListener listener = mock(NoteRepository.QueryNoteListener.class);

        subject.asyncGetLastSavedNote(Notebook.UNASSIGNED_NOTEBOOK_ID, listener);
        verify(asyncRepositoryHelper).startQuery(
                eq(NoteRepository.QUERY_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI(Notebook.UNASSIGNED_NOTEBOOK_ID, true)),
                isNull(String[].class),
                eq(NoteContract.Note.TABLE_NAME + "." + NoteContract.Note.COLUMN_NAME_ARCHIVED + "=?"),
                eq(new String[]{"false"}),
                eq(NoteContract.Note.COLUMN_NAME_UPDATED_AT + " DESC LIMIT 1"));
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void asyncUpdateNotebookId_updatesNoteNotebookId() throws Exception {
        NoteRepository.UpdateNoteListener listener = mock(NoteRepository.UpdateNoteListener.class);

        subject.asyncUpdateNotebook(1L, 2L, listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID, 2L);

        verify(asyncRepositoryHelper).startUpdate(
                eq(NoteRepository.UPDATE_NOTE_TOKEN),
                listenerCaptor.capture(),
                eq(NoteContract.Note.CONTENT_URI),
                eq(expectedValues),
                eq(NoteRepository.SELECTION_BY_ID),
                eq(new String[]{Long.toString(1L)}));
        assertThat((listenerCaptor.getValue())).isEqualTo(listener);
    }

    @Test
    public void updateNote_callsApplyBatch_onContentProvider() throws Exception {
        int result = subject.updateNote(
                testNote.getId(),
                testNote.getTitle(),
                testNote.getBody(),
                testNote.getLastSaved(),
                testNote.getActionLinks(),
                testNote.getStyleLinks());
        assertThat(result).isEqualTo(2);

        ContentValues expectedNoteValues = new ContentValues();
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_TITLE, "test title");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_BODY, "test body");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_UPDATED_AT, "test update time");

        ContentValues expectedActionValues = new ContentValues();
        expectedActionValues.put(ActionContract.Action.COLUMN_NAME_TITLE, "te");
        expectedActionValues.put(ActionContract.Action.COLUMN_NAME_NOTE_ID, testNote.getId());
        expectedActionValues.put(ActionContract.Action.COLUMN_NAME_LINK_ID, "123");
        expectedActionValues.put(ActionContract.Action.COLUMN_NAME_LINK_START, 0);
        expectedActionValues.put(ActionContract.Action.COLUMN_NAME_LINK_END, 2);

        ContentValues expectedStyleValues = new ContentValues();
        expectedStyleValues.put(StyleContract.Style.COLUMN_NAME_TYPE, StyleLink.BOLD_STYLE);
        expectedStyleValues.put(StyleContract.Style.COLUMN_NAME_NOTE_ID, testNote.getId());
        expectedStyleValues.put(StyleContract.Style.COLUMN_NAME_START, 3);
        expectedStyleValues.put(StyleContract.Style.COLUMN_NAME_END, 5);

        verify(contentProvider).applyBatch(operationsCaptor.capture());

        ArrayList<ContentProviderOperation> operations = operationsCaptor.getValue();

        assertThat(operations).hasSize(5);

        ShadowContentProviderOperation updateOperation = Shadows.shadowOf(operations.get(0));
        assertThat(operations.get(0).getUri()).isEqualTo(NoteContract.Note.CONTENT_URI);
        assertThat(updateOperation.getSelection()).isEqualTo("_id=?");
        assertThat(updateOperation.getSelectionArgs()).containsOnly(Long.toString(testNote.getId()));
        assertThat(updateOperation.getContentValues()).isEqualTo(expectedNoteValues);

        ShadowContentProviderOperation insertOperation = Shadows.shadowOf(operations.get(1));
        assertThat(operations.get(2).getUri()).isEqualTo(ActionContract.Action.CONTENT_URI);
        assertThat(insertOperation.getContentValues()).isEqualTo(expectedActionValues);

        ShadowContentProviderOperation deleteOperation = Shadows.shadowOf(operations.get(2));
        assertThat(operations.get(2).getUri()).isEqualTo(ActionContract.Action.CONTENT_URI);
        assertThat(deleteOperation.getSelection()).isEqualTo(
                ActionContract.Action.COLUMN_NAME_NOTE_ID + "=? AND " +
                        ActionContract.Action.COLUMN_NAME_LINK_ID + " NOT IN (?)");
        assertThat(deleteOperation.getSelectionArgs()).containsOnly(Long.toString(testNote.getId()), "123");

        ShadowContentProviderOperation deleteStylesOperation = Shadows.shadowOf(operations.get(3));
        assertThat(operations.get(3).getUri()).isEqualTo(StyleContract.Style.CONTENT_URI);
        assertThat(deleteStylesOperation.getSelection()).isEqualTo(StyleContract.Style.COLUMN_NAME_NOTE_ID + "=?");
        assertThat(deleteStylesOperation.getSelectionArgs()).containsOnly(Long.toString(testNote.getId()));

        ShadowContentProviderOperation insertStylesOperation = Shadows.shadowOf(operations.get(4));
        assertThat(operations.get(4).getUri()).isEqualTo(StyleContract.Style.CONTENT_URI);
        assertThat(insertStylesOperation.getContentValues()).isEqualTo(expectedStyleValues);
    }

    @Test
    public void updateNote_shouldDeleteAllActions_whenThereAreNoActionLinks() throws Exception {
        subject.updateNote(
                testNote.getId(),
                testNote.getTitle(),
                testNote.getBody(),
                testNote.getLastSaved(),
                Collections.<ActionLink>emptyList(),
                Collections.<StyleLink>emptyList());

        ContentValues expectedNoteValues = new ContentValues();
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_TITLE, "test title");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_BODY, "test body");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_UPDATED_AT, "test update time");

        verify(contentProvider).applyBatch(operationsCaptor.capture());
        ArrayList<ContentProviderOperation> operations = operationsCaptor.getValue();

        ShadowContentProviderOperation deleteOperation = Shadows.shadowOf(operations.get(1));
        assertThat(operations.get(1).getUri()).isEqualTo(ActionContract.Action.CONTENT_URI);
        assertThat(deleteOperation.getSelection()).isEqualTo(ActionContract.Action.COLUMN_NAME_NOTE_ID + "=?");
        assertThat(deleteOperation.getSelectionArgs()).containsOnly(Long.toString(testNote.getId()));
    }

    @Test
    public void updateNote_shouldDeleteAllStyles_whenThereAreNoStyles() throws Exception {
        subject.updateNote(
                testNote.getId(),
                testNote.getTitle(),
                testNote.getBody(),
                testNote.getLastSaved(),
                Collections.<ActionLink>emptyList(),
                Collections.<StyleLink>emptyList());

        ContentValues expectedNoteValues = new ContentValues();
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_TITLE, "test title");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_BODY, "test body");
        expectedNoteValues.put(NoteContract.Note.COLUMN_NAME_UPDATED_AT, "test update time");

        verify(contentProvider).applyBatch(operationsCaptor.capture());
        ArrayList<ContentProviderOperation> operations = operationsCaptor.getValue();

        ShadowContentProviderOperation deleteOperation = Shadows.shadowOf(operations.get(2));
        assertThat(operations.get(2).getUri()).isEqualTo(StyleContract.Style.CONTENT_URI);
        assertThat(deleteOperation.getSelection()).isEqualTo(StyleContract.Style.COLUMN_NAME_NOTE_ID + "=?");
        assertThat(deleteOperation.getSelectionArgs()).containsOnly(Long.toString(testNote.getId()));
    }
}
