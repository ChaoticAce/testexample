package gov.sparrow.repository;

import android.content.ContentValues;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.lang.ref.WeakReference;

import static gov.sparrow.contracts.NotebookContract.Notebook;
import static gov.sparrow.contracts.NotebookContract.NotebookListPosition;
import static gov.sparrow.repository.NotebookRepository.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NotebookRepositoryTest {

    @Mock AsyncRepositoryHelper asyncRepositoryHelper;
    private NotebookRepository subject;

    @Before
    public void setUp() {
        initMocks(this);
        subject = new NotebookRepository(asyncRepositoryHelper);
    }

    @Test
    public void createNotebook_startsAsyncInsert() {
        NotebookRepository.CreateNotebookListener listener = mock(NotebookRepository.CreateNotebookListener.class);
        subject.createNotebook("test notebook", listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Notebook.COLUMN_NAME_TITLE, "test notebook");

        verify(asyncRepositoryHelper).startInsert(
                eq(CREATE_NOTEBOOK_TOKEN),
                any(WeakReference.class),
                eq(Notebook.CONTENT_URI),
                eq(expectedValues));
    }

    @Test
    public void updateNotebook_startsAsyncUpdate() {
        NotebookRepository.UpdateNotebookListener listener = mock(NotebookRepository.UpdateNotebookListener.class);
        subject.updateNotebook(1L, "test notebook", listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Notebook.COLUMN_NAME_TITLE, "test notebook");

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_NOTEBOOK_TOKEN),
                any(WeakReference.class),
                eq(Notebook.CONTENT_URI),
                eq(expectedValues),
                eq(Notebook._ID + "=?"),
                eq(new String[]{Long.toString(1L)}));
    }

    @Test
    public void updateNotebookListPosition_startsAsyncUpdate() throws Exception {
        String selection = NotebookListPosition.COLUMN_NAME_NOTEBOOK_ID + "=?";
        ContentValues expectedValues = new ContentValues();
        expectedValues.put(NotebookListPosition.COLUMN_NAME_POSITION, 5);

        subject.updateNotebookPosition(1L, 5);

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_NOTEBOOK_TOKEN),
                isNull(),
                eq(NotebookListPosition.CONTENT_URI),
                eq(expectedValues),
                eq(selection),
                eq(new String[]{"1"}));
    }

    @Test
    public void deleteNotebook_withDeleteAll_startsAsyncArchive() throws Exception {
        NotebookRepository.DeleteNotebookListener listener = mock(NotebookRepository.DeleteNotebookListener.class);
        subject.archiveNotebook(1L, true, listener);
        verify(asyncRepositoryHelper).startDelete(
                eq(DELETE_NOTEBOOK_TOKEN),
                any(WeakReference.class),
                eq(Notebook.ARCHIVE_URI(1L, true)),
                eq(Notebook._ID + "=?"),
                eq(new String[]{Long.toString(1L)}));
    }

    @Test
    public void deleteNotebook__withoutDeleteAll_startsAsyncArchive() throws Exception {
        NotebookRepository.DeleteNotebookListener listener = mock(NotebookRepository.DeleteNotebookListener.class);
        subject.archiveNotebook(1L, false, listener);
        verify(asyncRepositoryHelper).startDelete(
                eq(DELETE_NOTEBOOK_TOKEN),
                any(WeakReference.class),
                eq(Notebook.ARCHIVE_URI(1L, false)),
                eq(Notebook._ID + "=?"),
                eq(new String[]{Long.toString(1L)}));
    }

}
