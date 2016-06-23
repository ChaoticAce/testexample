package gov.sparrow.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import gov.sparrow.SparrowTestRunner;

import static org.fest.assertions.api.ANDROID.assertThat;

@RunWith(SparrowTestRunner.class)
public class CursorWrapperAdapterTest {

    @Mock Cursor cursor;
    private CursorWrapperAdapter subject;

    @Before
    public void setUp() throws Exception {
        subject = new CursorWrapperAdapterImplement(null, cursor, false);

    }

    @Test
    public void changeCursor_whenCursorNull_changesNullCursor() throws Exception {
        subject.changeCursor(null);

        assertThat(subject.getCursor()).isNull();
    }

    @Test
    public void changeCursor_whenCursorSameAsWrappedCursor_doesNotChangeCursors() throws Exception {
        subject.changeCursor(cursor);
        Cursor cursor1 = subject.getCursor();

        subject.changeCursor(cursor);
        Cursor cursor2 = subject.getCursor();

        assertThat(cursor1).isEqualTo(cursor2);
    }

    private class CursorWrapperAdapterImplement extends CursorWrapperAdapter {

        public CursorWrapperAdapterImplement(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        protected Cursor wrapCursor(Cursor cursor) {
            return new CursorWrapper(cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }
    }
}