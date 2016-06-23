package gov.sparrow.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.inputmethod.InputMethodManager;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.List;

import javax.inject.Inject;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.database.SparrowDatabaseHelper;
import gov.sparrow.listeners.AddSpanClickListener;
import gov.sparrow.managers.SaveManager;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.models.links.ActionLink;
import gov.sparrow.models.links.StyleLink;
import gov.sparrow.models.spans.ActionLinkSpan;
import gov.sparrow.repository.ActionRepository;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TestActivity;
import gov.sparrow.util.TimeUtil;
import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@SuppressWarnings("Duplicates")
@RunWith(SparrowTestRunner.class)
public class NotePaneFragmentTest {

    private final static String TEST_TIME = "1999-02-22 11:55:14.123";

    @Inject TimeUtil timeUtil;
    @Inject NoteRepository noteRepository;
    @Inject ActionRepository actionRepository;
    @Inject SaveManager saveManager;

    @Captor ArgumentCaptor<SaveManager.SaveCompleteListener> saveCaptor;
    @Captor ArgumentCaptor<NoteRepository.UpdateNoteListener> updateCaptor;
    @Captor ArgumentCaptor<ActionRepository.CreateActionListener> actionCaptor;
    @Captor ArgumentCaptor<ActionLinkSpan> linkCaptor;

    private Note note;
    private NotePaneFragment subject;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(timeUtil.getTimeNow()).thenReturn(TEST_TIME);
        when(timeUtil.getUpdatedTime(TEST_TIME)).thenReturn("Mon 22 Feb 1999 06:55");

        note = NoteBuilder.noteBuilder()
                .id(48L)
                .title("new title")
                .body("boom is the boom of the boom")
                .notebookId(1L)
                .lastSaved(TEST_TIME)
                .styleLinks(asList(new StyleLink("background-color", 1, 2)))
                .build();

        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);
    }

    @Test
    public void onCreateView_shouldMapArgumentsToView() {
        Note note = NoteBuilder.noteBuilder()
                .id(0L)
                .notebookId(0L)
                .title("This is my new note")
                .body("This is the body of my new note.")
                .lastSaved(TEST_TIME)
                .actionLinks(asList(new ActionLink("123", 0, 25, false)))
                .styleLinks(asList(new StyleLink(StyleLink.BOLD_STYLE, 25, 28)))
                .build();
        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);

        assertThat(subject.noteTitle.getText().toString()).isEqualTo(note.getTitle());
        assertThat(subject.noteBody.getText().toString()).isEqualTo(note.getBody());
        assertThat(subject.updateTimestamp.getText().toString()).isEqualTo("Last Saved Mon 22 Feb 1999 06:55");

        ActionLinkSpan[] actionLinkSpans = subject.noteBody.getText().getSpans(0, subject.noteBody.length(), ActionLinkSpan.class);
        assertThat(actionLinkSpans).hasSize(1);
        assertThat(actionLinkSpans[0]).isEqualsToByComparingFields(
                new ActionLinkSpan("123", RuntimeEnvironment.application.getResources().getColor(R.color.action_dark_yellow), false));

        StyleSpan[] styles = subject.noteBody.getText().getSpans(0, subject.noteBody.length(), StyleSpan.class);
        assertThat(styles).hasSize(1);
        assertThat(styles[0]).isEqualsToByComparingFields(
                new StyleSpan(Typeface.BOLD));
    }

    @Test
    public void onCreateView_whenGivenCompletedAction_shouldCreateStrikeThruActionLink() {
        Note note = NoteBuilder.noteBuilder()
                .id(0L)
                .notebookId(0L)
                .title("This is my new note")
                .body("This is the body of my new note.")
                .lastSaved(TEST_TIME)
                .actionLinks(asList(new ActionLink("123", 0, 25, true)))
                .build();
        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);

        assertThat(subject.noteTitle.getText().toString()).isEqualTo(note.getTitle());
        assertThat(subject.noteBody.getText().toString()).isEqualTo(note.getBody());
        assertThat(subject.updateTimestamp.getText().toString()).isEqualTo("Last Saved Mon 22 Feb 1999 06:55");


        ActionLinkSpan[] actionLinkSpans = subject.noteBody.getText().getSpans(0, subject.noteBody.length(), ActionLinkSpan.class);
        assertThat(actionLinkSpans).hasSize(1);
        assertThat(actionLinkSpans[0]).isEqualsToByComparingFields(
                new ActionLinkSpan("123", RuntimeEnvironment.application.getResources().getColor(R.color.action_dark_yellow), true));
    }

    @Test
    public void onCreateView_whenNewNote_shouldRequestTitleFocus() {
        Note note = NoteBuilder.noteBuilder()
                .id(0L)
                .notebookId(0L)
                .title(SparrowDatabaseHelper.BLANK_NOTE_TITLE)
                .body("")
                .lastSaved(TEST_TIME)
                .build();
        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);

        assertThat(subject.noteTitle.isFocused()).isTrue();
    }

    @Test
    public void onCreateView_whenNewNote_shouldShowSoftKeyboard() throws Exception {
        Note note = NoteBuilder.noteBuilder()
                .id(0L)
                .notebookId(0L)
                .title(SparrowDatabaseHelper.BLANK_NOTE_TITLE)
                .body("")
                .lastSaved(TEST_TIME)
                .build();
        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);

        InputMethodManager imm = (InputMethodManager) subject.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        assertThat(shadowOf(imm).isSoftInputVisible()).isTrue();

        subject = NotePaneFragment.newInstance(note);
        startFragment(subject, TestActivity.class);
        imm = (InputMethodManager) subject.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        assertThat(shadowOf(imm).isSoftInputVisible()).isTrue();
    }

    @Test
    public void onCreateView_shouldDisplayLastSavedMessage() {
        assertThat(subject.updateTimestamp.getText().toString()).isEqualTo("Last Saved " + timeUtil.getUpdatedTime(note.getLastSaved()));
    }

    @Test
    public void onCreate_whenNoteGiven_shouldSetUpdateTimestamp_toNoteLastSavedInLocalTimezone() throws Exception {
        assertThat(subject.updateTimestamp.getText()).isEqualTo("Last Saved Mon 22 Feb 1999 06:55");
    }

    @Test
    public void clickingArchiveNoteButton_shouldShowAlertDialog() throws Exception {
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertThat(dialog).isNull();

        int color = RuntimeEnvironment.application.getBaseContext().getResources().getColor(R.color.action_dark_yellow);
        subject.noteBody.getText().setSpan(new ActionLinkSpan("0a-31k", color, false), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        subject.archiveNoteButton.performClick();

        dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertThat(dialog).isNotNull();

        List<Fragment> fragments = subject.getActivity().getSupportFragmentManager().getFragments();
        Fragment fragment = fragments.get(fragments.size() - 1);
        ANDROID.assertThat(fragment).isInstanceOf(DeleteNoteDialogFragment.class);

        assertThat(fragment.getArguments().getLong(DeleteNoteDialogFragment.NOTE_ID)).isEqualTo(48L);
        assertThat(fragment.getArguments().getBoolean(DeleteNoteDialogFragment.HAS_ACTIONS)).isTrue();

    }

    @Test
    public void afterTextChangeFinished_forNoteTitle_shouldScheduleSave() {
        subject.noteTitle.setText("a test title");
        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                any(SaveManager.SaveCompleteListener.class));
    }

    @Test
    public void afterTextChangeFinished_forNoteBody_shouldScheduleSave() {
        subject.noteBody.setText("a test body");
        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                any(SaveManager.SaveCompleteListener.class));
    }

    @Test
    public void afterTextChanged_forNoteTitle_shouldDisplaySavingMessage() {
        subject.noteTitle.setText("New title");
        assertThat(subject.updateTimestamp.getText()).isEqualTo("Saving...");
    }

    @Test
    public void afterTextChanged_forNoteBody_shouldDisplaySavingMessage() {
        subject.noteBody.setText("New body");
        assertThat(subject.updateTimestamp.getText()).isEqualTo("Saving...");
    }

    @Test
    public void onUpdateNoteComplete_forNoteTitle_shouldDisplaySavedMessage() {
        subject.noteTitle.setText("New title");
        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();
        assertThat(subject.updateTimestamp.getText()).isEqualTo("All changes saved");
    }

    @Test
    public void onUpdateNoteComplete_forNoteBody_shouldDisplaySavedMessage() {
        subject.noteBody.setText("New body");
        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();
        assertThat(subject.updateTimestamp.getText()).isEqualTo("All changes saved");
    }

    @Test
    public void onMakeActionItemClicked_shouldAddActionLinkSpanToSelectedText() throws Exception {
        assertThat(shadowOf(subject.makeActionButton).getOnClickListener()).isInstanceOf(AddSpanClickListener.class);

        subject.noteBody.setSelection(1, 2);

        subject.makeActionButton.performClick();

        Editable editable = subject.noteBody.getText();
        ActionLinkSpan[] spans = editable.getSpans(0, editable.length(), ActionLinkSpan.class);

        assertThat(spans[0]).isInstanceOf(ActionLinkSpan.class);
        assertThat(editable.getSpanStart(spans[0])).isEqualTo(1);
        assertThat(editable.getSpanEnd(spans[0])).isEqualTo(2);
    }

    @Test
    public void onMakeBoldTextClicked_shouldAddBoldStyleSpanToSelectedText() throws Exception {
        assertThat(shadowOf(subject.makeBoldTextButton).getOnClickListener()).isInstanceOf(AddSpanClickListener.class);

        subject.noteBody.setSelection(1, 2);

        subject.makeBoldTextButton.performClick();

        Editable editable = subject.noteBody.getText();
        StyleSpan[] spans = editable.getSpans(0, editable.length(), StyleSpan.class);

        assertThat(spans[0].getStyle()).isEqualTo(Typeface.BOLD);
        assertThat(editable.getSpanStart(spans[0])).isEqualTo(1);
        assertThat(editable.getSpanEnd(spans[0])).isEqualTo(2);
    }

    @Test
    public void onMakeActionItemClicked_shouldScheduleSave() {
        subject.noteBody.setSelection(3, 4);

        subject.makeActionButton.performClick();

        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                any(SaveManager.SaveCompleteListener.class));
    }

    @Test
    public void onMakeTextBoldClicked_shouldScheduleSave() {
        subject.noteBody.setSelection(3, 4);

        subject.makeBoldTextButton.performClick();

        verify(saveManager).scheduleSave(
                eq(48L),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                any(SaveManager.SaveCompleteListener.class));
    }

    @Test
    public void onPauseFragment_shouldSaveNote() {
        subject.onPause();
        verify(saveManager).save(
                eq(subject.getNoteId()),
                eq(subject.noteTitle.getText()),
                eq(subject.noteBody.getText()),
                any(SaveManager.SaveCompleteListener.class));
    }

    @Test
    public void onSelectionChanged_whenSelectionSizeGreaterThanZero_shouldChangeButtonText() {
        String expectedPreText = RuntimeEnvironment.application.getResources().getString(R.string.make_action_pre_select_text);
        assertThat(subject.makeActionButton.getText().toString()).isEqualTo(expectedPreText);

        subject.noteBody.setSelection(1, 2);

        String expectedText = RuntimeEnvironment.application.getResources().getString(R.string.make_action_text);
        assertThat(subject.makeActionButton.getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void onSelectionChanged_whenSelectionSizeZero_shouldChangeButtonText() {
        subject.makeActionButton.setText("bomb");
        subject.noteBody.setSelection(1, 1);

        String expectedText = RuntimeEnvironment.application.getResources().getString(R.string.make_action_pre_select_text);
        assertThat(subject.makeActionButton.getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void onSelectionChanged_whenSelectionSizeGreaterThanZero_shouldSetButtonActivated() {
        ANDROID.assertThat(subject.makeActionButton).isNotActivated();

        subject.noteBody.setSelection(1, 4);

        ANDROID.assertThat(subject.makeActionButton).isActivated();
    }

    @Test
    public void onSelectionChanged_whenSelectionSizeZero_shouldSetButtonDeactivated() {
        subject.makeActionButton.setActivated(true);

        subject.noteBody.setSelection(3, 3);

        ANDROID.assertThat(subject.makeActionButton).isNotActivated();
    }

}