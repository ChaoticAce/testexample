package gov.sparrow.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.List;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NotebookContract;
import gov.sparrow.fragment.EditNotebookDialogFragment;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TestActivity;

import static gov.sparrow.fragment.EditNotebookDialogFragment.NOTEBOOK_ID;
import static gov.sparrow.fragment.EditNotebookDialogFragment.NOTEBOOK_TITLE;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NotebookListAdapterTest {

    @Mock NoteRepository noteRepository;
    @Mock Cursor cursor;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    private NotebookListAdapter subject;

    @Before
    public void setUp() {
        initMocks(this);
        subject = new NotebookListAdapter(null, 0, noteRepository, null, null);

        when(cursor.getColumnIndexOrThrow(NotebookContract.Notebook.COLUMN_NAME_TITLE)).thenReturn(4555);
        when(cursor.getColumnIndexOrThrow(NotebookContract.Notebook._ID)).thenReturn(6666);
        when(cursor.getString(4555)).thenReturn("someTitle");
        when(cursor.getLong(6666)).thenReturn(12L);
        when(cursor.getPosition()).thenReturn(3);
    }

    @Test
    public void clickingEditNotebook_shouldShowEditNotebookDialogWithContent() {
        TestActivity activity = Robolectric.setupActivity(TestActivity.class);
        View view = LayoutInflater.from(activity).inflate(R.layout.notebook_list_item, null, false);
        subject.bindView(view, activity, cursor);

        view.findViewById(R.id.notebook_list_item_edit_text_button).performClick();

        assertThat(ShadowAlertDialog.getLatestDialog()).isNotNull();
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        Fragment dialogFragment = fragments.get(fragments.size() - 1);
        assertThat(dialogFragment).isInstanceOf(EditNotebookDialogFragment.class);
        assertThat(dialogFragment.getArguments().getLong(NOTEBOOK_ID)).isEqualTo(12L);
        assertThat(dialogFragment.getArguments().getString(NOTEBOOK_TITLE)).isEqualTo("someTitle");
    }

    @Test
    public void bindView() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);
        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat(((TextView) view.findViewById(R.id.notebook_list_item_title))).hasText("someTitle");
    }

    @Test
    public void changeCursor_whenCursorNotNull_changesWrappedCursor() throws Exception {
        subject.changeCursor(cursor);

        assertThat(subject.getCursor()).isInstanceOf(NotebookListAdapterCursorWrapper.class);
    }

    @Test
    public void allNotebook_shouldNotShowTouchGrip() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);
        when(cursor.getLong(6666)).thenReturn(NotebookContract.Notebook.UNASSIGNED_NOTEBOOK_ID);

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat((ImageView) view.findViewById(R.id.notebook_list_item_grip)).isNotVisible();
    }

    @Test
    public void userNotebook_shouldShowTouchGrip() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat((ImageView) view.findViewById(R.id.notebook_list_item_grip)).isVisible();
    }

    @Test
    public void touchingUserNotebook_whenActionDownMotionEvent_shouldStartDrag() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);
        view = spy(view);
        subject.bindView(view, RuntimeEnvironment.application, cursor);
        ImageView gripImage = (ImageView) view.findViewById(R.id.notebook_list_item_grip);
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_DOWN);

        boolean clickHandled = gripImage.dispatchTouchEvent(motionEvent);
        verify(view).startDrag(dragDataCaptor.capture(), any(View.DragShadowBuilder.class), isNull(), eq(0));

        ClipData clipData = dragDataCaptor.getValue();
        ClipDescription actualDescription = clipData.getDescription();
        ClipData.Item actualItem = clipData.getItemAt(0);
        assertThat(actualDescription.hasMimeType(NotebookContract.Notebook.TYPE_ITEM)).isTrue();
        assertThat(actualItem.getText()).isEqualTo("3");
        assertThat(clickHandled).isTrue();
    }

    @Test
    public void touchingUserNotebook_whenNonActionDownMotionEvent_shouldNotStartDrag() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);
        view = spy(view);
        subject.bindView(view, RuntimeEnvironment.application, cursor);
        ImageView gripImage = (ImageView) view.findViewById(R.id.notebook_list_item_grip);
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_UP);

        boolean clickHandled = gripImage.dispatchTouchEvent(motionEvent);
        verify(view, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), isNull(), eq(0));
        assertThat(clickHandled).isFalse();
    }

    @Test
    public void startDrag_shouldUseSelectedBackgroundColor() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.notebook_list_item, null, false);
        view = spy(view);
        ImageView gripImage = (ImageView) view.findViewById(R.id.notebook_list_item_grip);

        subject.bindView(view, RuntimeEnvironment.application, cursor);
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_DOWN);

        gripImage.dispatchTouchEvent(motionEvent);

        InOrder order = inOrder(view);

        order.verify(view).setBackgroundColor(RuntimeEnvironment.application.getResources().getColor(R.color.active_grey));
        order.verify(view).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), any(), anyInt());
        order.verify(view).setBackground(RuntimeEnvironment.application.getResources().getDrawable(R.drawable.notebook_item_selector));
    }

}