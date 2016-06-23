package gov.sparrow.datasync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import com.google.gson.Gson;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SparrowRestoreManagerTest {

    @Mock private ContentResolver contentResolver;
    @Mock private TimeUtil timeUtil;
    @Mock AsyncRestoreTaskFactory asyncRestoreTaskFactory;
    @Mock AsyncRestoreTask asyncRestoreTask;
    @Mock private File directory;
    @Mock private SparrowRestoreManager.RestoreTaskListener listener;
    @Captor private ArgumentCaptor<ArrayList<ContentProviderOperation>> operationsCaptor;
    @Captor private ArgumentCaptor<Runnable> restoreCaptor;
    private Gson jsonMapper;
    private SparrowRestoreManager subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(asyncRestoreTaskFactory.newAsyncRestoreTask(
                any(ContentResolver.class),
                any(Gson.class),
                any(SparrowRestoreManager.RestoreTaskListener.class)
        )).thenReturn(asyncRestoreTask);

        jsonMapper = new Gson();

        subject = new SparrowRestoreManager(contentResolver, timeUtil, jsonMapper, directory);
    }

    @Test
    public void getRestoreData_whenFilesExist_shouldReturnRestoreData() throws Exception {
        File file = mock(File.class);
        when(file.lastModified()).thenReturn(1464803770000L);

        when(directory.isDirectory()).thenReturn(true);
        when(directory.listFiles()).thenReturn(new File[]{file});

        SparrowRestoreManager.RestoreData restoreData = subject.getRestoreData();
        assertThat(restoreData).isNotNull();
    }

    @Test
    public void getRestoreData_whenDirectoryDoesNotExist_shouldReturnNull() throws Exception {
        when(directory.isDirectory()).thenReturn(false);
        assertThat(subject.getRestoreData()).isNull();
    }

    @Test
    public void getRestoreData_whenFileDoesNotExist_shouldReturnNull() throws Exception {
        File[] files = new File[]{};

        when(directory.isDirectory()).thenReturn(true);
        when(directory.listFiles()).thenReturn(files);

        assertThat(subject.getRestoreData()).isNull();
    }

    @Test
    public void restoreDataGetDate_shouldReturnDateOfMostRecentFile() throws Exception {
        File file1 = mock(File.class);
        when(file1.lastModified()).thenReturn(1464803770000L);

        File file2 = mock(File.class);
        when(file2.lastModified()).thenReturn(1451670970000L);

        long latestTime = 1542048970000L;
        File file3 = mock(File.class);
        when(file3.lastModified()).thenReturn(latestTime);

        File[] files = new File[]{file1, file2, file3};

        when(directory.isDirectory()).thenReturn(true);
        when(directory.listFiles()).thenReturn(files);

        when(timeUtil.getFormattedBackupDate(latestTime)).thenReturn("test time");

        assertThat(subject.getRestoreData().getDate()).isEqualTo("test time");
    }

    @Test
    public void restoreDataRestore_shouldExecuteRestoreTask_withFile() throws Exception {
        File file = mock(File.class);
        SparrowRestoreManager.RestoreData restoreData = new SparrowRestoreManager(contentResolver, timeUtil, jsonMapper, directory)
                .new RestoreData(asyncRestoreTaskFactory, file);
        restoreData.restore(listener);
        verify(asyncRestoreTask).execute(file);
    }

}