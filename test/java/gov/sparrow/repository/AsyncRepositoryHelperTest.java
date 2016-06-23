package gov.sparrow.repository;

import android.database.Cursor;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class AsyncRepositoryHelperTest {

    @Mock AsyncRepositoryHelper.AsyncRepositoryListener listener;
    private AsyncRepositoryHelper subject;

    @Before
    public void setUp() {
        initMocks(this);
        subject = new AsyncRepositoryHelper(RuntimeEnvironment.application.getContentResolver(), listener);
    }

    @Test
    public void onQueryComplete_notifiesListener() {
        Cursor cursor = mock(Cursor.class);
        subject.onQueryComplete(1, null, cursor);
        verify(listener).onQueryComplete(1, null, cursor);
    }

    @Test
    public void onInsertComplete_notifiesListener() {
        Uri uri = mock(Uri.class);
        subject.onInsertComplete(1, null, uri);
        verify(listener).onInsertComplete(1, null, uri);
    }

    @Test
    public void onUpdateComplete_notifiesListener() {
        subject.onUpdateComplete(1, null, 123);
        verify(listener).onUpdateComplete(1, null, 123);
    }

    @Test
    public void onDeleteComplete_notifiesListener() {
        subject.onDeleteComplete(1, null, 123);
        verify(listener).onDeleteComplete(1, null, 123);
    }
}
