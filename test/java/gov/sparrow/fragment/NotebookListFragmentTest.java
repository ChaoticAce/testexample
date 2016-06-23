package gov.sparrow.fragment;

import android.content.ClipData;
import android.content.ClipDescription;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.adapter.NotebookListAdapter;
import gov.sparrow.contracts.NotebookContract.Notebook;
import gov.sparrow.contracts.NotebookContract.NotebookListPosition;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.NotebookRepository;
import gov.sparrow.util.TestActivity;
import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static gov.sparrow.contracts.ActionContract.Action;
import static gov.sparrow.contracts.SparrowContract.SPARROW_CONTENT_AUTHORITY;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class NotebookListFragmentTest {

    @Inject NotebookRepository notebookRepository;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    @Mock SparrowProvider contentProvider;
    @Mock Cursor cursor;
    private NotebookListFragment subject;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        ShadowContentResolver.registerProvider(SPARROW_CONTENT_AUTHORITY, contentProvider);

        subject = new NotebookListFragment();
        startFragment(subject, TestActivity.class);
    }

    @Test
    public void onActionsSelected_shouldActivateButton() {
        subject.viewActionsButton.performClick();

        assertThat(subject.viewActionsButton).isActivated();
    }

    @Test
    public void onActionsSelected_afterShouldDeselectNotebook() {
        populateNotebookList();
        subject.notebookList.setItemChecked(1, true);

        subject.viewActionsButton.performClick();

        assertThat(subject.notebookList.getCheckedItemPosition()).isEqualTo(AdapterView.INVALID_POSITION);
    }

    @Test
    public void onActionsSelected_shouldPersistDeselectedNotebookBeyondDestruction() throws Exception {
        populateNotebookList();
        subject.notebookList.performItemClick(null, 1, 1);

        subject.viewActionsButton.performClick();

        Bundle bundle = new Bundle();
        subject.onSaveInstanceState(bundle);

        assertThat(bundle.getLong(NotebookListFragment.SELECTED_ID)).isEqualTo(NotebookListFragment.INVALID_ID);
    }

    @Test
    public void onNotebookItemClick_shouldCallOnNotebookItemClicked() {
        populateNotebookList();

        shadowOf(subject.notebookList).performItemClick(1);

        assertThat(TestActivity.NOTEBOOK_LIST_ITEM_CLICKED).isTrue();
        assertThat(TestActivity.CLICKED_NOTEBOOK_ID).isEqualTo(1L);
    }

    @Test
    public void onNotebookItemClick_shouldDeactivateActionsButton() throws Exception {
        populateNotebookList();
        subject.viewActionsButton.setActivated(true);

        shadowOf(subject.notebookList).performItemClick(1);

        assertThat(subject.viewActionsButton).isNotActivated();
    }

    @Test
    public void onNotebookHeaderClick_shouldCallOnNotebookItemClicked() {
        populateNotebookList();

        subject.notebookHeader.performClick();

        assertThat(TestActivity.NOTEBOOK_LIST_ITEM_CLICKED).isTrue();
        assertThat(TestActivity.CLICKED_NOTEBOOK_ID).isEqualTo(0L);
        assertThat(subject.notebookList.getCheckedItemPosition()).isEqualTo(0);
    }

    @Test
    public void onNotebookHeaderClick_shouldDeactivateActionsButton() throws Exception {
        populateNotebookList();
        subject.viewActionsButton.setActivated(true);

        subject.notebookHeader.performClick();

        assertThat(subject.viewActionsButton).isNotActivated();
    }

    @Test
    public void clickingAddNotebook_shouldShowNewNotebookDialog() {
        subject.addNotebookButton.performClick();

        assertThat(ShadowAlertDialog.getLatestDialog()).isNotNull();
        List<Fragment> fragments = subject.getActivity().getSupportFragmentManager().getFragments();
        assertThat(fragments.get(fragments.size() - 1)).isInstanceOf(CreateNotebookDialogFragment.class);
    }

    @Test
    public void notebookListItemsDoNotShowEditButtonByDefault() throws Exception {
        populateNotebookList();

        assertThat(subject.notebookList.getChildAt(0)
                .findViewById(R.id.notebook_list_item_edit_text_button)).isGone();
        assertThat(subject.notebookList.getChildAt(1)
                .findViewById(R.id.notebook_list_item_edit_text_button)).isGone();
    }

    @Test
    public void onCreateView_shouldLoadSavedSelectedId() throws Exception {
        Loader loader = mock(Loader.class);
        when(loader.getId()).thenReturn(Notebook.NOTEBOOK_MATCH);
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Notebook.COLUMN_NAME_TITLE,
                NotebookListPosition.COLUMN_NAME_POSITION,
                Notebook._ID
        });
        cursor.addRow(new Object[]{"first", "0", 2L});
        cursor.addRow(new Object[]{"second", "1", 1L});
        cursor.addRow(new Object[]{"third", "2", 3L});
        Bundle savedInstance = new Bundle();

        savedInstance.putLong(NotebookListFragment.SELECTED_ID, 1L);
        subject.onActivityCreated(savedInstance);
        subject.onLoadFinished(loader, cursor);

        assertThat(subject.notebookList.getCheckedItemPosition()).isEqualTo(1);
    }

    @Test
    public void onCreateView_shouldLoadActionButtonActivationState() throws Exception {
        Bundle savedInstance = new Bundle();
        savedInstance.putBoolean(NotebookListFragment.ACTION_BUTTON_ACTIVATION_STATE, true);
        assertThat(subject.viewActionsButton.isActivated()).isFalse();

        subject.onActivityCreated(savedInstance);

        assertThat(subject.viewActionsButton.isActivated()).isTrue();
    }

    @Test
    public void onCreateLoader_forNotebookList_shouldCreateCursorLoader() throws Exception {
        CursorLoader cursorLoader = (CursorLoader) subject.onCreateLoader(Notebook.NOTEBOOK_MATCH, null);

        assertThat(cursorLoader.getProjection()).isEqualTo(new String[]{
                Notebook._ID,
                Notebook.COLUMN_NAME_TITLE,
                NotebookListPosition.COLUMN_NAME_POSITION});
        assertThat(cursorLoader.getUri()).isEqualTo(Notebook.CONTENT_URI);
    }

    @Test
    public void onCreateLoader_forActionCount_shouldCreateCursorLoader() throws Exception {
        CursorLoader cursorLoader = (CursorLoader) subject.onCreateLoader(Action.ACTION_MATCH, null);

        assertThat(cursorLoader.getProjection()).isEqualTo(new String[]{
                String.format("count() as %s", Action.ALIAS_NAME_COUNT)});
        assertThat(cursorLoader.getUri()).isEqualTo(Action.CONTENT_URI);
        assertThat(cursorLoader.getSelection()).isEqualTo(String.format("%s=? AND %s.%s=?",
                Action.COLUMN_NAME_COMPLETED, Action.TABLE_NAME, Action.COLUMN_NAME_ARCHIVED));
        assertThat(cursorLoader.getSelectionArgs()).isEqualTo(new String[]{"false", "false"});
    }

    @Test
    public void onLoadFinished_shouldSwapNewData() {
        CursorAdapter adapter = mock(CursorAdapter.class);
        when(adapter.getViewTypeCount()).thenReturn(1);
        subject.notebookList.setAdapter(adapter);

        Loader loader = mock(Loader.class);
        when(loader.getId()).thenReturn(Notebook.NOTEBOOK_MATCH);

        subject.notebookList.setAdapter(adapter);
        subject.onLoadFinished(loader, cursor);

        verify(adapter).changeCursor(cursor);
    }

    @Test
    public void onLoadFinished_forInitialLoad_shouldHighlightAllNotebook() {
        CursorAdapter adapter = mock(CursorAdapter.class);
        Loader loader = mock(Loader.class);
        when(adapter.getViewTypeCount()).thenReturn(1);
        when(adapter.getCount()).thenReturn(2);
        when(adapter.getItemId(0)).thenReturn(1L);
        when(adapter.getItemId(1)).thenReturn(Notebook.UNASSIGNED_NOTEBOOK_ID);
        when(loader.getId()).thenReturn(Notebook.NOTEBOOK_MATCH);
        subject.notebookList.setAdapter(adapter);

        subject.notebookList.setAdapter(adapter);
        subject.onLoadFinished(loader, cursor);


        subject.onLoadFinished(loader, mock(Cursor.class));

        long checkedItemId = subject.notebookList.getItemIdAtPosition(subject.notebookList.getCheckedItemPosition());
        assertThat(checkedItemId).isEqualTo(Notebook.UNASSIGNED_NOTEBOOK_ID);
    }

    @Test
    public void onLoadFinished_whenReloadingAfterDrag_shouldHighlightPreviouslyHighlightedNotebook() throws Exception {
        CursorAdapter adapter = mock(CursorAdapter.class);
        Loader loader = mock(Loader.class);
        when(adapter.getViewTypeCount()).thenReturn(1);
        when(adapter.getCount()).thenReturn(2);
        when(adapter.getItemId(0)).thenReturn(1L);
        when(adapter.getItemId(1)).thenReturn(25L);
        when(loader.getId()).thenReturn(Notebook.NOTEBOOK_MATCH);

        subject.notebookList.getOnItemClickListener().onItemClick(null, null, 1, 25L);

        subject.notebookList.setAdapter(adapter);
        subject.onLoadFinished(loader, mock(Cursor.class));

        long checkedItemId = subject.notebookList.getItemIdAtPosition(subject.notebookList.getCheckedItemPosition());
        assertThat(checkedItemId).isEqualTo(25L);
    }

    @Test
    public void onLoadFinished_shouldShowNumberOfOpenActions() throws Exception {
        String[] columnNames = new String[]{Action.ALIAS_NAME_COUNT};
        MatrixCursor cursor = new MatrixCursor(columnNames, 1);
        cursor.addRow(new Object[]{5});

        Loader loader = mock(Loader.class);
        when(loader.getId()).thenReturn(Action.ACTION_MATCH);

        subject.onLoadFinished(loader, cursor);

        ANDROID.assertThat(subject.openActions).hasText("5 OPEN");
    }

    @Test
    public void destroyingFragment_shouldSaveSelectedNotebookId() throws Exception {
        populateNotebookList();
        Bundle bundle = new Bundle();

        shadowOf(subject.notebookList).performItemClick(2);
        subject.onSaveInstanceState(bundle);

        assertThat(bundle.getLong(NotebookListFragment.SELECTED_ID)).isEqualTo(2L);
    }

    @Test
    public void destroyingFragment_shouldSaveActionButtonActivation() throws Exception {
        Bundle bundle = new Bundle();

        subject.viewActionsButton.setActivated(true);
        subject.onSaveInstanceState(bundle);

        assertThat(bundle.getBoolean(NotebookListFragment.ACTION_BUTTON_ACTIVATION_STATE)).isEqualTo(true);
    }

    @Test
    public void onNotebookDragEventCompleted_shouldUpdatePosition_withNotebookRepository() throws Exception {
        NotebookListAdapter adapter = mock(NotebookListAdapter.class);
        when(adapter.getViewTypeCount()).thenReturn(1);
        when(adapter.getItemId(1)).thenReturn(33L);

        subject.notebookList.setAdapter(adapter);
        subject.notebookDragEventCompleted(1, 100);

        verify(notebookRepository).updateNotebookPosition(33L, 100);
    }

    @Test
    public void onLongItemClick_shouldStartDrag() throws Exception {
        AdapterView.OnItemLongClickListener onItemLongClickListener = subject.notebookList.getOnItemLongClickListener();
        View view = mock(View.class);

        boolean clickHandled = onItemLongClickListener.onItemLongClick(null, view, 3, 6);

        verify(view).startDrag(dragDataCaptor.capture(), any(View.DragShadowBuilder.class), isNull(), eq(0));
        ClipData clipData = dragDataCaptor.getValue();
        ClipDescription actualDescription = clipData.getDescription();
        ClipData.Item actualItem = clipData.getItemAt(0);
        assertThat(actualDescription.hasMimeType(Notebook.TYPE_ITEM)).isTrue();
        assertThat(actualItem.getText()).isEqualTo("3");
        assertThat(clickHandled).isTrue();
    }

    @Test
    public void onLongClickItem_whenAllNotebookClicked_shouldNotStartDrag() throws Exception {
        AdapterView.OnItemLongClickListener onItemLongClickListener = subject.notebookList.getOnItemLongClickListener();
        View view = mock(View.class);

        boolean clickHandled = onItemLongClickListener.onItemLongClick(null, view, 0, Notebook.UNASSIGNED_NOTEBOOK_ID);

        verify(view, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), any(), anyInt());
        assertThat(clickHandled).isFalse();
    }

    private void populateNotebookList() {
        //Using ArrayAdapters because Robolectric 3.0 doesn't work with bindView
        List<String> cursors = Arrays.asList("All", "test notebook title", "test notebook title 2");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(subject.getActivity(),
                R.layout.notebook_list_item, R.id.notebook_list_item_title, cursors);
        subject.notebookList.setAdapter(adapter);
        shadowOf(subject.notebookList).populateItems();
    }
}