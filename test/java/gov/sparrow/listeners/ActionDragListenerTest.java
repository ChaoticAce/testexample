package gov.sparrow.listeners;

import android.content.ClipData;
import android.content.ClipDescription;
import android.view.DragEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.repository.ActionRepository;
import gov.sparrow.util.TestFragment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class ActionDragListenerTest {

    @Mock ListScroller listScroller;
    @Mock ActionRepository actionRepository;
    private ActionDragListener subject;
    private View view;
    private DragEvent dragEvent;
    private TestFragment fragment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        fragment = new TestFragment();
        startFragment(fragment);
        subject = new ActionDragListener(3, listScroller, fragment);
        view = mock(View.class);
        dragEvent = mock(DragEvent.class);

    }

    @Test
    public void onDrag_acceptsActionDrags() throws Exception {
        ClipDescription clipDescription = new ClipDescription("test", new String[]{ActionContract.Action.TYPE_ITEM});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);

        assertThat(subject.onDrag(view, dragEvent)).isTrue();

    }

    @Test
    public void onDrag_doesNotAcceptNonActionDrags() throws Exception {
        ClipDescription clipDescription = new ClipDescription("test", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);

        assertThat(subject.onDrag(view, dragEvent)).isFalse();
    }

    @Test
    public void onDraggedItemDrops_movesActionToNewPosition() {
        ClipData dragData =  ClipData.newPlainText("action", "1");
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DROP);
        when(dragEvent.getClipData()).thenReturn(dragData);

        subject.onDrag(view, dragEvent);

        assertThat(fragment.ACTION_DRAG_EVENT_COMPLETED).isTrue();
        assertThat(fragment.ACTION_DRAG_EVENT_NEW_POSITION).isEqualTo(3);
        assertThat(fragment.ACTION_DRAG_EVENT_OLD_POSITION).isEqualTo(1);
    }

    @Test
    public void whenDraggedItemEntersView_highlightsView() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_ENTERED);

        assertViewHovering(true);
    }

    @Test
    public void whenDraggedItemStaysOverView_scrollsParent() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_LOCATION);

        subject.onDrag(view, dragEvent);

        verify(listScroller).scrollParent(view, dragEvent);
    }

    @Test
    public void whenDraggedItemExitsView_dehighlightsView() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_EXITED);

        assertViewHovering(false);
    }

    @Test
    public void whenDragEnds_dehighlightsView() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_ENDED);

        assertViewHovering(false);
    }

    private void assertViewHovering(boolean isHovered) {
        subject.onDrag(view, dragEvent);
        verify(view).setHovered(isHovered);
    }
}