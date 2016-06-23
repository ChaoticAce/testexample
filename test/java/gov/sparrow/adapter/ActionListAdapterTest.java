package gov.sparrow.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.List;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.ActionContract.Action;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.fragment.ActionItemSettingsDialog;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.ActionRepository;
import gov.sparrow.util.TestActivity;
import gov.sparrow.util.TimeUtil;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ActionListAdapterTest {

    @Mock ActionListAdapterCursorWrapper cursor;
    @Mock TimeUtil timeUtil;
    @Mock ActionRepository actionRepository;
    @Mock SparrowProvider sparrowProvider;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    private ActionListAdapter subject;
    private ActionBarActivity activity;
    private View view;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, sparrowProvider);

        when(cursor.getColumnIndexOrThrow(Action._ID)).thenReturn(1);
        when(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_NOTE_ID)).thenReturn(2);
        when(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_COMPLETED)).thenReturn(3);
        when(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_TITLE)).thenReturn(5);
        when(cursor.getColumnIndexOrThrow(Action.ALIAS_NAME_NOTE_TITLE)).thenReturn(4);
        when(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_DUE_DATE)).thenReturn(6);
        when(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_CHECKBOX_UPDATED_AT)).thenReturn(7);
        when(cursor.getLong(1)).thenReturn(1L);
        when(cursor.isNull(2)).thenReturn(false);
        when(cursor.getLong(2)).thenReturn(2L);
        when(cursor.getString(4)).thenReturn("note title");
        when(cursor.getString(5)).thenReturn("fake action body");
        when(cursor.getString(6)).thenReturn("2016-05-25 00:00:00.000");
        when(cursor.getString(7)).thenReturn("2016-05-26 01:10:10.000");
        when(cursor.getPosition()).thenReturn(9);

        subject = new ActionListAdapter(activity, 0, null, timeUtil, actionRepository, null);
        activity = Robolectric.setupActivity(TestActivity.class);
        view = LayoutInflater.from(activity).inflate(R.layout.action_list_item, null, false);
    }

    @Test
    public void bindView_shouldBindAnActionListItem() {
        subject.bindView(view, activity, cursor);

        assertThat(((TextView) view.findViewById(R.id.action_list_item_body))).hasText("fake action body");
        assertThat((ImageView) view.findViewById(R.id.action_list_item_settings_button)).isVisible();
    }

    @Test
    public void bindView_whenDueDateGiven_shouldShowDueDate() throws Exception {
        when(timeUtil.getFormattedDate(anyString())).thenReturn("25 MAY");

        subject.bindView(view, activity, cursor);

        TextView dateView = (TextView) view.findViewById(R.id.action_list_item_date);
        assertThat((TextView) view.findViewById(R.id.action_list_item_date)).hasText("25 MAY");
        assertThat(dateView.getTypeface()).hasStyle(Typeface.BOLD);
    }

    @Test
    public void bindView_whenNoDueDateGiven_shouldNotShowDueDate() throws Exception {
        when(cursor.getString(6)).thenReturn(null);
        subject.bindView(view, activity, cursor);

        assertThat((TextView) view.findViewById(R.id.action_list_item_date)).isEmpty();
    }

    @Test
    public void bindView_whenCompleted_shouldShowCompletedDate() throws Exception {
        when(timeUtil.getFormattedDate("2016-05-26 01:10:10.000")).thenReturn("26 MAY");
        when(timeUtil.getFormattedDate("2016-05-25 01:10:10.000")).thenReturn("25 MAY");
        when(cursor.getString(3)).thenReturn("true");

        subject.bindView(view, activity, cursor);
        TextView dateView = (TextView) view.findViewById(R.id.action_list_item_date);

        assertThat(dateView).hasText("26 MAY");
        assertThat(dateView.getTypeface()).hasStyle(Typeface.NORMAL);
    }

    @Test
    public void bindView_shouldBindAnActionListItemAsChecked_andDisableSettingsMenu() {
        when(cursor.getString(3)).thenReturn("true");

        subject.bindView(view, activity, cursor);

        CheckBox actionCheckbox = ((CheckBox) view.findViewById(R.id.action_list_item_checkbox));
        assertThat(actionCheckbox).isChecked();

        TextView actionBody = ((TextView) view.findViewById(R.id.action_list_item_body));
        assertThat(actionBody).hasPaintFlags(actionBody.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        ImageView settingsButton = (ImageView) view.findViewById(R.id.action_list_item_settings_button);
        assertThat(settingsButton).isNotVisible();

        verifyZeroInteractions(sparrowProvider);
    }

    @Test
    public void onActionItemDragButtonClicked_givenMotionEventActionDown_shouldStartDrag_withItemPosition() throws Exception {
        View itemView = LayoutInflater.from(activity).inflate(R.layout.action_list_item, null, false);
        itemView = spy(itemView);

        subject.bindView(itemView, activity, cursor);

        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_DOWN);

        boolean clickHandled = itemView.findViewById(R.id.action_list_item_grip).dispatchTouchEvent(motionEvent);

        verify(itemView).startDrag(dragDataCaptor.capture(), any(View.DragShadowBuilder.class), isNull(), eq(0));
        ClipData clipData = dragDataCaptor.getValue();
        ClipDescription actualDescription = clipData.getDescription();
        ClipData.Item actualItem = clipData.getItemAt(0);
        assertThat(actualDescription.hasMimeType(Action.TYPE_ITEM)).isTrue();
        assertThat(actualItem.getText()).isEqualTo("9");
        assertThat(clickHandled).isTrue();
    }

    @Test
    public void onActionItemDragButtonClicked_whenGivenNonMotionEventActionDown_shouldNotStartDrag() throws Exception {
        View itemView = LayoutInflater.from(activity).inflate(R.layout.action_list_item, null, false);
        itemView = spy(itemView);

        subject.bindView(itemView, activity, cursor);

        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_BUTTON_RELEASE);

        boolean clickHandled = itemView.findViewById(R.id.action_list_item_grip).dispatchTouchEvent(motionEvent);

        verify(itemView, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), any(), anyInt());
        assertThat(clickHandled).isFalse();
    }

    @Test
    public void onActionItemDragButtonClicked_whenItemCompleted_shouldNotStartDrag() throws Exception {
        View itemView = LayoutInflater.from(activity).inflate(R.layout.action_list_item, null, false);
        itemView = spy(itemView);
        subject.bindView(itemView, activity, cursor);
        ((CheckBox) itemView.findViewById(R.id.action_list_item_checkbox)).setChecked(true);

        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_DOWN);

        boolean clickHandled = itemView.findViewById(R.id.action_list_item_grip).dispatchTouchEvent(motionEvent);

        verify(itemView, never()).startDrag(any(ClipData.class), any(View.DragShadowBuilder.class), any(), anyInt());
        assertThat(clickHandled).isFalse();
    }

    @Test
    public void onCheckBoxClicked_shouldCompleteTheAction() {
        when(cursor.getString(3)).thenReturn("false");
        when(timeUtil.getTimeNow()).thenReturn("the date/time");

        subject.bindView(view, activity, cursor);

        CheckBox actionCheckbox = ((CheckBox) view.findViewById(R.id.action_list_item_checkbox));
        actionCheckbox.setChecked(true);

        ImageView settingsButton = (ImageView) view.findViewById(R.id.action_list_item_settings_button);
        ANDROID.assertThat(settingsButton).isVisible();

        verify(actionRepository).updateActionCompleted(eq(1L), eq("the date/time"), eq(true), any(ActionRepository.UpdateActionListener.class));
    }

    @Test
    public void onCheckBoxChecked_shouldUpdateActionPositionToPositionOfLastUncompletedAction() throws Exception {
        CheckBox actionCheckbox = ((CheckBox) view.findViewById(R.id.action_list_item_checkbox));
        when(cursor.getLastIncompletePosition()).thenReturn(2);
        subject.bindView(view, activity, cursor);

        actionCheckbox.setChecked(true);

        verify(actionRepository).updateActionPosition(1L, 2);
    }

    @Test
    public void onCheckBoxUnchecked_shouldUpdateActionPositionToPositionOfFirstCompletedAction() throws Exception {
        CheckBox actionCheckbox = ((CheckBox) view.findViewById(R.id.action_list_item_checkbox));
        when(cursor.getFirstCompletedPosition()).thenReturn(6);
        subject.bindView(view, activity, cursor);

        actionCheckbox.setChecked(true);
        reset(actionRepository);

        actionCheckbox.setChecked(false);

        verify(actionRepository).updateActionPosition(1L, 6);
    }

    @Test
    public void whenCheckBoxChecked_shouldHideGrip() throws Exception {
        when(cursor.getString(3)).thenReturn(Boolean.TRUE.toString());
        subject.bindView(view, activity, cursor);

        assertThat(view.findViewById(R.id.action_list_item_grip)).isNotVisible();
    }

    @Test
    public void whenCheckBoxUnchecked_shouldShowGrip() throws Exception {
        when(cursor.getString(3)).thenReturn(Boolean.FALSE.toString());
        subject.bindView(view, activity, cursor);

        assertThat(view.findViewById(R.id.action_list_item_grip)).isVisible();
    }

    @Test
    public void whenCheckBoxChecked_shouldHideActionSettingsButton() throws Exception {
        when(cursor.getString(3)).thenReturn(Boolean.TRUE.toString());
        subject.bindView(view, activity, cursor);

        assertThat(view.findViewById(R.id.action_list_item_settings_button)).isNotVisible();
    }

    @Test
    public void whenCheckBoxUnchecked_shouldShowActionSettingsButton() throws Exception {
        when(cursor.getString(3)).thenReturn(Boolean.FALSE.toString());
        subject.bindView(view, activity, cursor);

        assertThat(view.findViewById(R.id.action_list_item_settings_button)).isVisible();
    }

    @Test
    public void noteLink_showsNoteTitle() throws Exception {
        subject.bindView(view, activity, cursor);

        TextView textView = ((TextView) view.findViewById(R.id.action_list_item_note_link));
        assertThat(textView).hasText("note title");
    }

    @Test
    public void clickingNoteLink_opensNotePaneWithCorrectNote() throws Exception {
        subject.bindView(view, activity, cursor);

        TextView textView = ((TextView) view.findViewById(R.id.action_list_item_note_link));
        textView.performClick();

        assertThat(textView).isVisible();
        assertThat(TestActivity.CLICKED_ACTION_ITEM).isTrue();
        assertThat(TestActivity.CLICKED_ACTION_ITEM_NOTE_ID).isEqualTo(2L);
    }

    @Test
    public void actionItem_withoutNoteId_shouldNotHaveNoteLink() throws Exception {
        when(cursor.isNull(cursor.getColumnIndexOrThrow(Action.COLUMN_NAME_NOTE_ID))).thenReturn(true);
        subject.bindView(view, activity, cursor);
        TextView textView = ((TextView) view.findViewById(R.id.action_list_item_note_link));

        assertThat(textView).isInvisible();
        assertThat(textView).isNotClickable();
    }

    @Test
    public void onActionListItemSettingsButtonClicked_launchesSettingsDialog() {
        subject.bindView(view, activity, cursor);

        ImageView settingsButton = (ImageView) view.findViewById(R.id.action_list_item_settings_button);
        settingsButton.performClick();

        assertThat(ShadowAlertDialog.getLatestDialog()).isNotNull();
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        Fragment dialogFragment = fragments.get(fragments.size() - 1);
        ANDROID.assertThat(dialogFragment).isInstanceOf(ActionItemSettingsDialog.class);
    }

    @Test
    public void changeCursor_whenCursorNotNull_changesWrappedCursor() throws Exception {
        subject.changeCursor(cursor);

        assertThat(subject.getCursor()).isInstanceOf(ActionListAdapterCursorWrapper.class);
    }

}