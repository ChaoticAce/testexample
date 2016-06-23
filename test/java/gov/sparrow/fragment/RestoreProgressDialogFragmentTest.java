package gov.sparrow.fragment;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.datasync.SparrowRestoreManager;
import gov.sparrow.util.TestActivity;
import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class RestoreProgressDialogFragmentTest {

    @Mock SparrowRestoreManager.RestoreData restoreData;
    @Captor ArgumentCaptor<SparrowRestoreManager.RestoreTaskListener> restoreCaptor;
    private RestoreProgressDialogFragment subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = RestoreProgressDialogFragment.newInstance(restoreData);
        startFragment(subject, TestActivity.class);

        assertThat(subject.getRetainInstance()).isTrue();
    }

    @Test
    public void onCreate_shouldCallRestore() throws Exception {
        verify(restoreData).restore(restoreCaptor.capture());

        ANDROID.assertThat(subject.getDialog()).isShowing();
        restoreCaptor.getValue().onRestoreTaskComplete();

        assertThat(TestActivity.RESTORE_COMPLETE_CALLED).isTrue();
        ANDROID.assertThat(subject.getDialog()).isNull();
    }

}