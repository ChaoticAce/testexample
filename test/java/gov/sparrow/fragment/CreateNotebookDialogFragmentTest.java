package gov.sparrow.fragment;

import android.support.v7.app.AlertDialog;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.NotebookRepository;
import gov.sparrow.util.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class CreateNotebookDialogFragmentTest {

    @Inject NotebookRepository notebookRepository;
    @Mock SparrowProvider contentProvider;
    private CreateNotebookDialogFragment subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);

        subject = new CreateNotebookDialogFragment();
        startFragment(subject, TestActivity.class);

    }

    @Test
    public void onNoteBookTitleField_shouldDisableSaveButton_withNoTextInput() {
        AlertDialog alertDialog = (AlertDialog) subject.getDialog();

        assertThat(subject.notebookTitle.getText().toString()).isEmpty();
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled()).isFalse();
    }

    @Test
    public void onNoteBookTitleField_shouldDisableSaveButton_whenTextRemoved() {
        AlertDialog alertDialog = (AlertDialog) subject.getDialog();
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled()).isFalse();

        subject.notebookTitle.setText("firstText");
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled()).isTrue();

        subject.notebookTitle.setText("");
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled()).isFalse();
    }

    @Test
    public void onSaveButtonPressedWithText_shouldInsertNewNotebook() throws Exception {
        AlertDialog alertDialog = (AlertDialog) subject.getDialog();

        subject.notebookTitle.setText("TEST NOTEBOOK");
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        verify(notebookRepository).createNotebook(eq("TEST NOTEBOOK"), any(NotebookRepository.CreateNotebookListener.class));
    }

}
