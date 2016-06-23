package gov.sparrow.fragment;

import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import static android.content.DialogInterface.BUTTON_POSITIVE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class DeleteNoteDialogFragmentTest {

    @Inject NoteRepository noteRepository;
    @Captor ArgumentCaptor<NoteRepository.DeleteNoteListener> updateCaptor;
    private DeleteNoteDialogFragment subject;
    private String noteTitle;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        noteTitle = "Test Title";
        subject = DeleteNoteDialogFragment.newInstance(11L, noteTitle, true);
        startFragment(subject, TestActivity.class);
    }

    @Test
    public void clickingDeleteButton_shouldArchiveNote() {
        ((AlertDialog) subject.getDialog()).getButton(BUTTON_POSITIVE).performClick();

        verify(noteRepository).asyncArchiveNote(eq(11L), updateCaptor.capture());
        updateCaptor.getValue().onDeleteNoteComplete();
        assertThat(TestActivity.ARCHIVE_CLICKED).isTrue();
    }

    @Test
    public void dialogMessage_whenHasNoActions_showsNormalWarning() throws Exception {
        subject = DeleteNoteDialogFragment.newInstance(98L, noteTitle, false);
        startFragment(subject, TestActivity.class);

        AlertDialog dialog = (AlertDialog) subject.getDialog();
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        String expectedMessage = RuntimeEnvironment.application.getResources().getString(R.string.delete_note_message_no_actions);
        assertThat(messageView.getText().toString()).isEqualTo(expectedMessage);
    }

    @Test
    public void dialogMessage_whenHasActions_showsDeleteActionWarning() throws Exception {
        AlertDialog dialog = (AlertDialog) subject.getDialog();
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        String expectedMessage = RuntimeEnvironment.application.getResources().getString(R.string.delete_note_message_actions);
        assertThat(messageView.getText().toString()).isEqualTo(expectedMessage);
    }

    @Test
    public void dialogTitle_shouldContainNoteTitle() throws Exception {
        AlertDialog dialog = (AlertDialog) subject.getDialog();
        String expectedTitle = RuntimeEnvironment.application.getResources().getString(R.string.delete_note_title, noteTitle);
        TextView titleView = (TextView) dialog.findViewById(android.support.v7.appcompat.R.id.alertTitle);
        assertThat(titleView.getText().toString()).isEqualTo(expectedTitle);
    }

    @Test
    public void dialogTitle_whemTitleIsEmpty_shouldContainUntitledNoteTitle() throws Exception {
        subject = DeleteNoteDialogFragment.newInstance(11L, "", true);
        startFragment(subject, TestActivity.class);

        AlertDialog dialog = (AlertDialog) subject.getDialog();
        String expectedTitle = RuntimeEnvironment.application.getResources().getString(R.string.delete_note_title, "Untitled");
        TextView titleView = (TextView) dialog.findViewById(android.support.v7.appcompat.R.id.alertTitle);
        assertThat(titleView.getText().toString()).isEqualTo(expectedTitle);
    }

    @Test
    public void dialogDeleteButton_whenHasActions_showActionLabel() throws Exception {
        AlertDialog dialog = (AlertDialog) subject.getDialog();
        String expectedLabel = RuntimeEnvironment.application.getResources().getString(R.string.delete_all);
        assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()).isEqualTo(expectedLabel);
    }

    @Test
    public void dialogDeleteButton_whenHasNoActions_showNormalLabel() throws Exception {
        subject = DeleteNoteDialogFragment.newInstance(98L, noteTitle, false);
        startFragment(subject, TestActivity.class);

        AlertDialog dialog = (AlertDialog) subject.getDialog();
        String expectedLabel = RuntimeEnvironment.application.getResources().getString(R.string.delete);
        assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()).isEqualTo(expectedLabel);
    }


}