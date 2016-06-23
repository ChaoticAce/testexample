package gov.sparrow.listeners;


import android.content.ClipData;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NoteContract;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NoteTouchListenerTest {

    @Mock View view;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    private NoteTouchListener subject;
    private final long noteId = 23L;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = new NoteTouchListener(view, noteId);

        when(view.getContext()).thenReturn(RuntimeEnvironment.application);
    }

    @Test
    public void onTouch_givenMotionEventActionDown_shouldStartDrag_withNoteId() throws Exception {
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_DOWN);

        boolean touchHandled = subject.onTouch(null, motionEvent);

        assertDragStarted(touchHandled);
    }

    @Test
    public void onTouch_givenNonMotionEventActionDown_shouldNotStartDrag() throws Exception {
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_BUTTON_RELEASE);

        boolean touchHandled = subject.onTouch(null, motionEvent);

        verify(view, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), isNull(), eq(0));
        assertThat(touchHandled).isFalse();
    }

    @Test
    public void onLongItemClick_shouldStartDrag_withNoteId() throws Exception {
        boolean touchHandled = subject.onItemLongClick(null, view, -1, noteId);

        assertDragStarted(touchHandled);
    }

    private void assertDragStarted(boolean touchHandled) {
        verify(view).startDrag(dragDataCaptor.capture(), any(View.DragShadowBuilder.class), isNull(), eq(0));
        Uri expectedUri = NoteContract.Note.CONTENT_URI(noteId);
        assertThat(dragDataCaptor.getValue().getItemAt(0).getUri()).isEqualTo(expectedUri);
        assertThat(touchHandled).isTrue();
    }
}