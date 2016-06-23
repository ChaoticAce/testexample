package gov.sparrow.listeners;

import android.content.ClipData;
import android.content.ClipDescription;
import android.net.Uri;
import android.view.DragEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.NotebookContract;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TestActivity;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NotebookDragListenerTest {

    @Mock DragEvent dragEvent;
    @Mock NoteRepository noteRepository;
    @Mock NotebookDragListener.NotebookDragCompleteListener notebookDragCompletedListener;
    @Mock ListScroller listScroller;
    @Captor ArgumentCaptor<NoteRepository.UpdateNoteListener> updateCaptor;
    @Captor ArgumentCaptor<NoteRepository.QueryNoteListener> queryCaptor;
    private NotebookDragListener subject;
    private View view;
    private TestActivity activity;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        activity = Robolectric.setupActivity(TestActivity.class);
        subject = new NotebookDragListener(noteRepository, 22L, "new notebook title", 145, listScroller, activity, notebookDragCompletedListener);
        view = new View(RuntimeEnvironment.application);
    }

    @Test
    public void onDrag_acceptsNoteDrags() {
        ClipDescription clipDescription = new ClipDescription("test", new String[]{NoteContract.Note.TYPE_ITEM});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);

        assertThat(subject.onDrag(view, dragEvent)).isTrue();
    }

    @Test
    public void onDrag_acceptsNotebookDrags() {
        ClipDescription clipDescription = new ClipDescription("test", new String[]{NotebookContract.Notebook.TYPE_ITEM});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);

        assertThat(subject.onDrag(view, dragEvent)).isTrue();
    }

    @Test
    public void onDrag_whenNotebookIsAll_doesNotAcceptNotebookDrags() throws Exception {
        ClipDescription clipDescription = new ClipDescription("test", new String[]{NotebookContract.Notebook.TYPE_ITEM});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);
        subject = new NotebookDragListener(noteRepository, 0L, "new notebook title", 1, null, activity, notebookDragCompletedListener);

        assertThat(subject.onDrag(view, dragEvent)).isFalse();
    }

    @Test
    public void onDrag_ignoresNonNoteDrags() {
        ClipDescription clipDescription =
                new ClipDescription("test", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(dragEvent.getClipDescription()).thenReturn(clipDescription);

        assertThat(subject.onDrag(view, dragEvent)).isFalse();
    }

    @Test
    public void whenDraggedItemEntersView_highlightsView() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_ENTERED);

        subject.onDrag(view, dragEvent);
        assertThat(view.isHovered()).isTrue();
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

        assertViewNotHighlighted();
    }

    @Test
    public void whenDragEnds_dehighlightsView() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_ENDED);

        assertViewNotHighlighted();
    }

    @Test
    public void whenNoteDraggedItemDrops_movesNoteToNotebook() throws Exception {
        runNoteDragWithDropAction();

        verify(noteRepository).asyncUpdateNotebook(eq(2L), eq(22L),
                any(NoteRepository.UpdateNoteListener.class));
    }

    @Test
    public void whenDraggedNoteMoved_toastShown() throws Exception {
        runNoteDragWithDropAction();

        verify(noteRepository).asyncUpdateNotebook(eq(2L), eq(22L), updateCaptor.capture());
        updateCaptor.getValue().onUpdateNoteComplete(0);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Note moved to new notebook title");
    }

    @Test
    public void whenDraggedNoteMoved_reloadWorkspaceWithOldNotebookId() throws Exception {
        runNoteDragWithDropAction();

        verify(noteRepository).asyncUpdateNotebook(eq(2L), eq(22L), updateCaptor.capture());
        updateCaptor.getValue().onUpdateNoteComplete(0);

        assertThat(TestActivity.NOTEBOOK_DRAG_EVENT_COMPLETED).isTrue();
    }

    @Test
    public void whenNotebookDraggedItemDrops_movesNotebookPosition() throws Exception {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DROP);
        when(dragEvent.getClipDescription()).thenReturn(new ClipDescription("do not care", new String[]{NotebookContract.Notebook.TYPE_ITEM}));

        ClipData data = ClipData.newPlainText("notebook", "10");
        when(dragEvent.getClipData()).thenReturn(data);

        subject.onDrag(view, dragEvent);

        verify(notebookDragCompletedListener).notebookDragEventCompleted(10, 145);
    }

    @Test
    public void whenNotebookDraggedItemDropsOnAllNotebook_shouldNotMoveNotebook() throws Exception {
        subject = new NotebookDragListener(noteRepository, 0L, "ALL", 0, null, activity, notebookDragCompletedListener);

        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DROP);
        when(dragEvent.getClipDescription()).thenReturn(new ClipDescription("do not care", new String[]{NotebookContract.Notebook.TYPE_ITEM}));

        ClipData data = ClipData.newPlainText("notebook", "10");
        when(dragEvent.getClipData()).thenReturn(data);

        subject.onDrag(view, dragEvent);

        verify(notebookDragCompletedListener, never()).notebookDragEventCompleted(anyInt(), anyInt());
    }

    private void runNoteDragWithDropAction() {
        when(dragEvent.getAction()).thenReturn(DragEvent.ACTION_DROP);
        when(dragEvent.getClipDescription()).thenReturn(new ClipDescription("do not care", new String[]{NoteContract.Note.TYPE_ITEM}));
        ClipData.Item item = new ClipData.Item(Uri.withAppendedPath(NoteContract.Note.CONTENT_URI, Long.toString(2L)));
        ClipData data = new ClipData("don't care", new String[]{NoteContract.Note.TYPE_ITEM}, item);
        when(dragEvent.getClipData()).thenReturn(data);

        subject.onDrag(view, dragEvent);

        verify(noteRepository).asyncGetNote(eq(2L), queryCaptor.capture());
        Note note = NoteBuilder.noteBuilder()
                .id(2L)
                .notebookId(33L)
                .build();
        queryCaptor.getValue().onQueryNoteComplete(note);
    }

    private void assertViewNotHighlighted() {
        subject.onDrag(view, dragEvent);
        assertThat(view.isHovered()).isFalse();
    }
}