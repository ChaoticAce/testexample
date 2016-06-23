package gov.sparrow.fragment;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

import javax.inject.Inject;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.adapter.ActionListAdapter;
import gov.sparrow.adapter.helpers.SortableCursorWrapper;
import gov.sparrow.contracts.ActionContract.Action;
import gov.sparrow.contracts.ActionContract.ActionListPosition;
import gov.sparrow.contracts.NoteContract.Note;
import gov.sparrow.contracts.NotebookContract.Notebook;
import gov.sparrow.contracts.NotebookContract.NotebookListPosition;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.ActionRepository;

import static gov.sparrow.fragment.ActionPaneFragment.LOADER_ID_ACTION_COUNT;
import static gov.sparrow.fragment.ActionPaneFragment.LOADER_ID_ACTION_LIST;
import static gov.sparrow.fragment.ActionPaneFragment.LOADER_ID_ACTION_LIST_FILTER;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class ActionPaneFragmentTest {

    private ActionPaneFragment subject;
    @Inject ActionRepository actionRepository;
    @Mock SparrowProvider sparrowProvider;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, sparrowProvider);

        subject = ActionPaneFragment.newInstance();
        startFragment(subject);
    }

    @Test
    public void onCreateView_shouldShowActionTitle() {
        assertThat(subject.actionTitle.getText()).isEqualTo(subject.getResources().getString(R.string.action_label));
    }

    @Test
    public void onCreateView_shouldShowActionCreationFieldWithHint() {
        assertThat(subject.actionCreationField.getHint()).isEqualTo(subject.getResources().getString(R.string.edit_text_hint));
    }

    @Test
    public void onCreateView_shouldShowCursorInActionTextField() {
        assertThat(subject.actionCreationField.isFocused()).isTrue();
    }

    @Test
    public void onCreateLoader_whenActionList_shouldReturnActionCursorLoader() throws Exception {
        CursorLoader loader = (CursorLoader) subject.onCreateLoader(LOADER_ID_ACTION_LIST, null);
        assertThat(loader.getUri()).isEqualTo(Action.CONTENT_URI);
        assertThat(loader.getSelection()).isEqualTo(Action.TABLE_NAME + "." + Action.COLUMN_NAME_ARCHIVED + "=?");
        assertThat(loader.getSelectionArgs()).isEqualTo(new String[]{"false"});
        assertThat(loader.getProjection()).isEqualTo(new String[]{
                String.format("%s.%s", Action.TABLE_NAME, Action._ID),
                String.format("%s.%s", Action.TABLE_NAME, Action.COLUMN_NAME_TITLE),
                Action.COLUMN_NAME_COMPLETED,
                Action.COLUMN_NAME_NOTE_ID,
                String.format("%s.%s as %s", Note.TABLE_NAME, Note.COLUMN_NAME_TITLE, Action.ALIAS_NAME_NOTE_TITLE),
                Note.COLUMN_NAME_NOTEBOOK_ID,
                ActionListPosition.COLUMN_NAME_POSITION,
                Action.COLUMN_NAME_DUE_DATE,
                Action.COLUMN_NAME_CHECKBOX_UPDATED_AT
        });
    }

    @Test
    public void onCreateLoader_whenActionListFilter_shouldReturnNotebookCursorLoader() throws Exception {
        CursorLoader loader = (CursorLoader) subject.onCreateLoader(LOADER_ID_ACTION_LIST_FILTER, null);
        assertThat(loader.getUri()).isEqualTo(Notebook.CONTENT_URI);
        assertThat(loader.getSelection()).isEqualTo(Notebook.TABLE_NAME + "." + Notebook.COLUMN_NAME_ARCHIVED + "=?");
        assertThat(loader.getSelectionArgs()).isEqualTo(new String[]{"false"});
        assertThat(loader.getSortOrder()).isEqualTo(NotebookListPosition.COLUMN_NAME_POSITION + " ASC");
    }

    @Test
    public void onCreateLoader_whenOpenActionCount_shouldReturnActionCountCursorLoader() throws Exception {
        CursorLoader cursorLoader = (CursorLoader) subject.onCreateLoader(LOADER_ID_ACTION_COUNT, null);

        assertThat(cursorLoader.getProjection()).isEqualTo(new String[]{
                "sum(case when completed = 'false' AND actions.archived = 'false' then 1 else 0 end) open, " +
                        "sum(case when completed = 'true' AND actions.archived = 'false' then 1 else 0 end) closed"
        });
        assertThat(cursorLoader.getUri()).isEqualTo(Action.CONTENT_URI);
    }

    @Test
    public void onLoadFinished_whenGivenAnAction_shouldScrollListToGivenAction() throws Exception {
        subject = ActionPaneFragment.newInstance(2L);
        startFragment(subject);

        loadActionListCursor_intoList();

        Cursor actualCursor = (Cursor) subject.actionList.getSelectedItem();
        assertThat(actualCursor.getLong(actualCursor.getColumnIndex(Action._ID))).isEqualTo(2);
    }

    @Test
    public void onLoadFinished_whenNoActionGiven_shouldNotCrash() throws Exception {
        subject = ActionPaneFragment.newInstance();
        startFragment(subject);

        loadActionListCursor_intoList();
    }

    @Test
    public void onLoadFinished_whenCursorNotNull_shouldReturnOpenAndCloseCount() throws Exception {
        Loader loader = mock(Loader.class);
        when(loader.getId()).thenReturn(LOADER_ID_ACTION_COUNT);

        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndexOrThrow("open")).thenReturn(0);
        when(cursor.getColumnIndexOrThrow("closed")).thenReturn(1);
        when(cursor.getInt(0)).thenReturn(565989000);
        when(cursor.getInt(1)).thenReturn(412565123);

        subject.onLoadFinished(loader, cursor);

        assertThat(subject.actionOpenCount).hasText("565,989,000 Open / ");
        assertThat(subject.actionClosedCount).hasText("412,565,123 Total Closed");
    }

    @Test
    public void onActivityCreated_shouldInitializeLoaders() throws Exception {
        assertThat(subject.getLoaderManager().getLoader(LOADER_ID_ACTION_LIST)).isStarted();
        assertThat(subject.getLoaderManager().getLoader(LOADER_ID_ACTION_LIST_FILTER)).isStarted();
        assertThat(subject.getLoaderManager().getLoader(LOADER_ID_ACTION_COUNT)).isStarted();
    }

    @Test
    public void onActionCreationFieldEnter_shouldIgnoreEmptyBody() {
        subject.actionCreationField.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        verify(sparrowProvider, never()).insert(any(Uri.class), any(ContentValues.class));
    }

    @Test
    public void onActionCreationFieldEnter_shouldResetEditText() {
        fillActionCreationField("a fake action item");
        assertThat(subject.actionCreationField.isFocused()).isTrue();
        assertThat(subject.actionCreationField.getText().toString()).isEqualTo("");
    }

    @Test
    public void onActionCreationFieldEnterKeyDown_shouldNotAddNewAction() {
        subject.actionCreationField.setText("a fake action item");
        subject.actionCreationField.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        verify(actionRepository, never()).createAction(eq("a fake action item"), any(ActionRepository.CreateActionListener.class));
    }

    @Test
    public void onActionCreationFieldEnterKeyUp_shouldAddNewAction() {
        fillActionCreationField("a fake action item");
        verify(actionRepository).createAction(eq("a fake action item"), any(ActionRepository.CreateActionListener.class));
    }

    @Test
    public void onAddActionButtonClicked_shouldAddNewAction() {
        subject.actionCreationField.setText("a fake action item");
        subject.actionAddButton.performClick();

        verify(actionRepository).createAction(eq("a fake action item"), any(ActionRepository.CreateActionListener.class));
    }

    @Test
    public void onLongItemClick_whenItemNotChecked_shouldStartDrag() throws Exception {
        AdapterView.OnItemLongClickListener onItemLongClickListener = subject.actionList.getOnItemLongClickListener();
        View view = mock(View.class);
        CheckBox checkBox = mock(CheckBox.class);
        when(view.findViewById(R.id.action_list_item_checkbox)).thenReturn(checkBox);
        when(checkBox.isChecked()).thenReturn(false);

        boolean clickHandled = onItemLongClickListener.onItemLongClick(null, view, 3, 6);

        verify(view).startDrag(dragDataCaptor.capture(), any(View.DragShadowBuilder.class), isNull(), eq(0));
        ClipData clipData = dragDataCaptor.getValue();
        ClipDescription actualDescription = clipData.getDescription();
        ClipData.Item actualItem = clipData.getItemAt(0);
        assertThat(actualDescription.hasMimeType(Action.TYPE_ITEM)).isTrue();
        assertThat(actualItem.getText()).isEqualTo("3");
        assertThat(clickHandled).isTrue();
    }

    @Test
    public void onLongClickItem_whenItemChecked_shouldNotStartDrag() throws Exception {
        AdapterView.OnItemLongClickListener onItemLongClickListener = subject.actionList.getOnItemLongClickListener();
        View view = mock(View.class);
        CheckBox checkBox = mock(CheckBox.class);
        when(view.findViewById(R.id.action_list_item_checkbox)).thenReturn(checkBox);
        when(checkBox.isChecked()).thenReturn(true);

        boolean clickHandled = onItemLongClickListener.onItemLongClick(null, view, 3, 6);

        verify(view, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), any(), anyInt());
        assertThat(clickHandled).isFalse();
    }

    @Test
    public void onDragCompleted_shouldUpdateActionListDatabase() throws Exception {
        ActionListAdapter adapter = mock(ActionListAdapter.class);
        when(adapter.getViewTypeCount()).thenReturn(1);
        when(adapter.getItemId(2)).thenReturn(3L);

        SortableCursorWrapper cursorWrapper = mock(SortableCursorWrapper.class);
        when(cursorWrapper.getMappedPosition(6)).thenReturn(6);
        when(adapter.getCursor()).thenReturn(cursorWrapper);

        subject.actionList.setAdapter(adapter);

        subject.actionDragEventCompleted(6, 2);

        verify(actionRepository).updateActionPosition(3, 6);
    }

    @Test
    public void actionListFilterOnItemSelected_withAllActionsSelected_shouldShowAddActionField() throws Exception {
        View view = setupMockFilterViews();
        ANDROID.assertThat(subject.addActionContainer).isVisible();
        Shadows.shadowOf(subject.actionListFilter).getItemSelectedListener().onItemSelected(null, view, 0, 1);
        ANDROID.assertThat(subject.addActionContainer).isNotVisible();

        Shadows.shadowOf(subject.actionListFilter).getItemSelectedListener().onItemSelected(null, view, 0, Notebook.UNASSIGNED_NOTEBOOK_ID);

        ANDROID.assertThat(subject.addActionContainer).isVisible();
    }

    @Test
    public void actionListFilterOnItemSelected_withFilterSelected_shouldShowFilterTitle() throws Exception {
        View view = setupMockFilterViews();
        ANDROID.assertThat(subject.notebookFilterTitleContainer).isNotVisible();

        Shadows.shadowOf(subject.actionListFilter).getItemSelectedListener().onItemSelected(null, view, 0, 1);

        ANDROID.assertThat(subject.notebookFilterTitleContainer).isVisible();
        assertThat(subject.notebookFilterTitle.getText().toString()).isEqualTo("test notebook title");
    }

    @Test
    public void selectingActionListFilter_whenFilterNotAll_shouldHideActionCounts() throws Exception {
        View view = setupMockFilterViews();

        Shadows.shadowOf(subject.actionListFilter).getItemSelectedListener().onItemSelected(null, view, 0, 1);

        assertThat(subject.actionCountsContainer).isInvisible();
    }

    @Test
    public void selectingActionListFilter_withAllActionsSelected_shouldShowActionCounts() throws Exception {
        View view = setupMockFilterViews();

        Shadows.shadowOf(subject.actionListFilter).getItemSelectedListener().onItemSelected(null, view, 0, Notebook.UNASSIGNED_NOTEBOOK_ID);

        assertThat(subject.actionCountsContainer).isVisible();
    }

    private View setupMockFilterViews() {
        View view = mock(View.class);
        TextView filterTitle = mock(TextView.class);
        when(view.findViewById(R.id.filter_item_title)).thenReturn(filterTitle);
        when(filterTitle.getText()).thenReturn("test notebook title");
        return view;
    }

    private void fillActionCreationField(String body) {
        subject.actionCreationField.setText(body);
        subject.actionCreationField.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
    }

    private void loadActionListCursor_intoList() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Action._ID,
                Action.COLUMN_NAME_TITLE,
                Action.COLUMN_NAME_COMPLETED,
                ActionListPosition.COLUMN_NAME_POSITION,
                Note.COLUMN_NAME_NOTEBOOK_ID
        });
        cursor.addRow(new Object[]{1L, "title 1", "true", 0, 1L});
        cursor.addRow(new Object[]{2L, "title 2", "false", 1, 1L});

        CursorLoader loader = mock(CursorLoader.class);
        when(loader.getId()).thenReturn(LOADER_ID_ACTION_LIST);

        subject.onLoadFinished(loader, cursor);
    }
}