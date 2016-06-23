package gov.sparrow.adapter;

import android.database.MatrixCursor;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NotebookContract;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class NotebookListAdapterCursorWrapperTest {

    private MatrixCursor cursor;
    private NotebookListAdapterCursorWrapper subject;

    @Test
    public void setsUpPositionList() throws Exception {
        cursor = new MatrixCursor(new String[]{
                NotebookContract.NotebookListPosition.COLUMN_NAME_POSITION
        });
        cursor.addRow(new Object[]{10});
        cursor.addRow(new Object[]{22});
        cursor.addRow(new Object[]{31});

        subject = new NotebookListAdapterCursorWrapper(cursor);

        subject.getWrappedCursor().moveToPosition(-1);

        assertThat(subject.getCount()).isEqualTo(3);
        assertThat(subject.getMappedPosition(0)).isEqualTo(10);
        assertThat(subject.getMappedPosition(1)).isEqualTo(22);
        assertThat(subject.getMappedPosition(2)).isEqualTo(31);
    }

    @Test
    public void rewindsCursorBeforeSettingUpPositionList() throws Exception {
        cursor = new MatrixCursor(new String[]{
                NotebookContract.NotebookListPosition.COLUMN_NAME_POSITION
        });
        cursor.addRow(new Object[]{0});
        subject = new NotebookListAdapterCursorWrapper(cursor);

        subject.getWrappedCursor().moveToPosition(0);

        assertThat(subject.getCount()).isEqualTo(1);
        assertThat(subject.getMappedPosition(0)).isEqualTo(0);
    }


}