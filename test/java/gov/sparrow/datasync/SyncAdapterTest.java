package gov.sparrow.datasync;

import android.content.ContentProviderClient;
import android.os.Environment;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.BackupContract;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.database.BackupMergeCursor;
import gov.sparrow.database.SparrowDatabaseBackupWriter;
import gov.sparrow.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SyncAdapterTest {

    @Mock ContentProviderClient contentProviderClient;
    @Mock SparrowDatabaseBackupWriter sparrowDatabaseBackupWriter;
    @Mock BackupMergeCursor cursor;
    @Mock TimeUtil timeUtil;
    @Captor ArgumentCaptor<File> fileCaptor;
    private SyncAdapter subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(contentProviderClient.query(BackupContract.BACKUP_URI, null, null, null, null))
                .thenReturn(cursor);
        when(timeUtil.getBackUpTimeNow()).thenReturn("date");

        subject = new SyncAdapter(RuntimeEnvironment.application, false, sparrowDatabaseBackupWriter, timeUtil);
    }

    @Test
    public void onPerformSync_shouldBackupDatabase() throws Exception {
        subject.onPerformSync(null, null, SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProviderClient, null);
        verify(sparrowDatabaseBackupWriter).backupToFile(eq(cursor), fileCaptor.capture());
        String expectedPath = Environment.getExternalStorageDirectory() + "/sparrow/backup_date.json.gz";
        assertThat(fileCaptor.getValue().getAbsolutePath()).isEqualTo(expectedPath);
    }

}