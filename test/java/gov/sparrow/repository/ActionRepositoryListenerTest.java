package gov.sparrow.repository;

import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gov.sparrow.repository.ActionRepository.CREATE_ACTION_TOKEN;
import static gov.sparrow.repository.ActionRepository.UPDATE_ACTION_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SparrowTestRunner.class)
public class ActionRepositoryListenerTest {

    private ActionRepositoryListener subject;

    @Before
    public void setUp() {
        subject = new ActionRepositoryListener();
    }

    @Test
    public void onInsertComplete_callsCreateActionListener() throws Exception {
        ActionRepository.CreateActionListener listener = mock(ActionRepository.CreateActionListener.class);
        subject.onInsertComplete(
                CREATE_ACTION_TOKEN,
                listener,
                Uri.parse("content://test.authority/action/123"));
        verify(listener).onCreateActionComplete("123");
    }

    @Test
    public void onInsertComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_ACTION_TOKEN,
                    null,
                    Uri.parse("content://test.authority/action/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onInsertComplete_ignoresNullListener() throws Exception {
        try {
            subject.onInsertComplete(
                    CREATE_ACTION_TOKEN,
                    null,
                    Uri.parse("content://test.authority/action/123"));
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onUpdateComplete_callsUpdateActionListener() throws Exception {
        ActionRepository.UpdateActionListener listener = mock(ActionRepository.UpdateActionListener.class);
        subject.onUpdateComplete(UPDATE_ACTION_TOKEN, listener, 1);
        verify(listener).onUpdateActionComplete(1);
    }

    @Test
    public void onUpdateComplete_ignoresDeadReference() throws Exception {
        try {
            subject.onUpdateComplete(UPDATE_ACTION_TOKEN, null, 1);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onUpdateComplete_ignoresNullListener() throws Exception {
        try {
            subject.onUpdateComplete(UPDATE_ACTION_TOKEN, null, 1);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

}