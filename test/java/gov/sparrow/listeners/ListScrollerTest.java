package gov.sparrow.listeners;

import android.view.DragEvent;
import android.view.View;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import gov.sparrow.SparrowTestRunner;

import static gov.sparrow.listeners.ListScroller.SCROLL_SPEED;
import static gov.sparrow.listeners.ListScroller.SCROLL_THRESHOLD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ListScrollerTest {

    @Mock View dragView;
    @Mock ListView parentView;
    @Mock DragEvent dragEvent;

    private final int PARENT_HEIGHT = 300;
    private final int PARENT_TOP_Y = 200;
    private ListScroller subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = new ListScroller();
    }

    @Test
    public void whenDragLocationNearTopOfParentView_shouldScrollParentUp() throws Exception {
        dragScrollSetup(PARENT_TOP_Y + SCROLL_THRESHOLD, -1);

        subject.scrollParent(dragView, dragEvent);

        verify(parentView).smoothScrollByOffset(-1 * SCROLL_SPEED);
    }

    @Test
    public void whenDragLocationNotNearTopOfParentView_shouldNotScroll() throws Exception {
        dragScrollSetup(PARENT_TOP_Y + SCROLL_THRESHOLD, 0);

        subject.scrollParent(dragView, dragEvent);

        verify(parentView, never()).smoothScrollByOffset(anyInt());
    }

    @Test
    public void whenDragLocationNearBottomOfParentView_shouldScrollParentDown() throws Exception {
        dragScrollSetup(PARENT_TOP_Y + PARENT_HEIGHT - SCROLL_THRESHOLD, 1);

        subject.scrollParent(dragView, dragEvent);

        verify(parentView).smoothScrollByOffset(SCROLL_SPEED);
    }

    private void dragScrollSetup(float eventYPosition, final int drawViewYPosition) {
        when(dragView.getParent()).thenReturn(parentView);
        when(dragEvent.getY()).thenReturn(eventYPosition);
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_LOCATION);

        when(parentView.getHeight()).thenReturn(PARENT_HEIGHT);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                int[] coords = (int[]) arguments[0];
                coords[1] = drawViewYPosition;
                return null;
            }
        }).when(dragView).getLocationInWindow((int[]) any());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                int[] coords = (int[]) arguments[0];
                coords[1] = PARENT_TOP_Y;
                return null;
            }
        }).when(parentView).getLocationInWindow((int[]) any());
    }
}