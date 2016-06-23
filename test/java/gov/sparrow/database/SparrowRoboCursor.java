package gov.sparrow.database;

import android.database.ContentObserver;
import android.database.DataSetObserver;

import org.robolectric.fakes.RoboCursor;

public class SparrowRoboCursor extends RoboCursor {
    @Override
    public void registerContentObserver(ContentObserver observer) {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {

    }

    @Override
    public String[] getColumnNames() {
        return (String[]) columnNames.toArray();
    }
}
