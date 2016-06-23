package gov.sparrow.fragment;

import android.support.v7.app.AlertDialog;
import android.widget.Button;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.datasync.SparrowRestoreManager;
import gov.sparrow.util.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class RestoreDialogFragmentTest {

    @Inject SparrowRestoreManager restoreManager;
    @Captor ArgumentCaptor<SparrowRestoreManager.RestoreTaskListener> restoreCaptor;
    private RestoreDialogFragment subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);
    }

    @Test
    public void onCreateDialogView_withoutBackupFile_shouldDisableRestoreButton() throws Exception {
        checkRestoreEnabled(false);
    }

    @Test
    public void onCreateDialogView_withBackupFile_shouldEnableRestoreButton() throws Exception {
        checkRestoreEnabled(true);
    }

    @Test
    public void onRestoreClicked_shouldCallOnRestoreStarted() throws Exception {
        clickRestore();
        assertThat(TestActivity.RESTORE_STARTED_CALLED).isTrue();
    }

    private void checkRestoreEnabled(boolean enabled) {
        if (enabled) {
            when(restoreManager.getRestoreData()).thenReturn(mock(SparrowRestoreManager.RestoreData.class));
        } else {
            when(restoreManager.getRestoreData()).thenReturn(null);
        }

        subject = RestoreDialogFragment.newInstance();
        startFragment(subject, TestActivity.class);

        verify(restoreManager).getRestoreData();

        Button button = ((AlertDialog) subject.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);

        if (enabled) {
            assertThat(button.isEnabled()).isTrue();
        } else {
            assertThat(button.isEnabled()).isFalse();
        }
    }

    private void clickRestore() {
        when(restoreManager.getRestoreData()).thenReturn(mock(SparrowRestoreManager.RestoreData.class));

        subject = RestoreDialogFragment.newInstance();
        startFragment(subject, TestActivity.class);

        AlertDialog dialog = (AlertDialog) subject.getDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }

}