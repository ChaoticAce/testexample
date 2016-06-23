package gov.sparrow.views;

import android.content.ClipDescription;
import android.view.DragEvent;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NoteContract;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SparrowEditTextTest {
    @Mock DragEvent event;
    private SparrowEditText subject;

    @Before
    public void setUp() throws Exception{
        initMocks(this);
        subject = new SparrowEditText(RuntimeEnvironment.application);
    }

    @Test
    public void onDragEvent_ignoresNoteMimeType() throws Exception {
        ClipDescription clipDescription = new ClipDescription("blah", new String[]{NoteContract.Note.TYPE_ITEM});

        when(event.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(event.getClipDescription()).thenReturn(clipDescription);
        assertThat(subject.onDragEvent(event)).isFalse();
    }

    @Test
    public void onDragEvent_acceptsNonNoteMimeType() throws Exception {
        ClipDescription clipDescription = new ClipDescription("blah", new String[]{"lkdjfalkdfjaf"});

        when(event.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(event.getClipDescription()).thenReturn(clipDescription);
        assertThat(subject.onDragEvent(event)).isTrue();
    }

    @Test
    public void onSelectionChanged_whenSelectionChangedListenerSet_shouldCallSelectionChangedListener() throws Exception {
        SparrowEditText.SelectionChangedListener selectionListener = mock(SparrowEditText.SelectionChangedListener.class);
        subject.setOnSelectionChanged(selectionListener);

        subject.onSelectionChanged(1, 2);

        verify(selectionListener).selectionChanged(1, 2);
    }
}