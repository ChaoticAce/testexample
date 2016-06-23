package gov.sparrow.repository;

import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gov.sparrow.repository.NotebookRepository.CREATE_NOTEBOOK_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SparrowTestRunner.class)
public class NotebookRepositoryListenerTest {

    private NotebookRepositoryListener subject;

    @Before
    public void setUp() {
        subject = new NotebookRepositoryListener();
    }

    @Test
    public void onInsertComplete_callsCreateNotebookListener() throws Exception {
        NotebookRepository.CreateNotebookListener listener = mock(NotebookRepository.CreateNotebookListener.class);
        subject.onInsertComplete(
                CREATE_NOTEBOOK_TOKEN,
                listener,
                Uri.parse("content://test.authority/notebook/123"));
        verify(listener).onCreateNotebookComplete("123");
    }

    @Test
    public void onInsertComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_NOTEBOOK_TOKEN,
                    null,
                    Uri.parse("content://test.authority/notebook/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onInsertComplete_ignoresNullListener() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_NOTEBOOK_TOKEN,
                    null,
                    Uri.parse("content://test.authority/notebook/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

}