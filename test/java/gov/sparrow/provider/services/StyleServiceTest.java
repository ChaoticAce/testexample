package gov.sparrow.provider.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import static gov.sparrow.contracts.StyleContract.Style;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class StyleServiceTest {

    private static final String[] PROJECTION = new String[]{"projection"};
    private static final String SELECTION = "selection";
    private static final String[] SELECTION_ARGS = new String[]{"args"};
    private static final String SORT_ORDER = "sort";

    @Mock private Context context;
    @Mock private ContentResolver resolver;
    @Mock private SQLiteQueryBuilder builder;
    @Mock private SQLiteDatabase db;
    @Mock private Cursor cursor;
    @Mock private ContentValues values;
    private StyleService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(context.getContentResolver()).thenReturn(resolver);

        subject = new StyleService();
    }

    @Test
    public void queryStyles_shouldReturnStyleCursor() throws Exception {
        when(builder.query(
                any(SQLiteDatabase.class),
                any(String[].class),
                anyString(),
                any(String[].class),
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(cursor);

        Cursor expectedCursor = subject.queryStyles(db, builder, PROJECTION, SELECTION, SELECTION_ARGS, SORT_ORDER);
        assertThat(expectedCursor).isEqualTo(cursor);

        InOrder inOrder = inOrder(builder);
        inOrder.verify(builder).setTables(Style.TABLE_NAME);
        inOrder.verify(builder).query(db, PROJECTION, SELECTION, SELECTION_ARGS, null, null, SORT_ORDER);
    }

    @Test
    public void insertStyles_shouldInsertIntoDatabase() throws Exception {
        when(db.insert(anyString(), anyString(), any(ContentValues.class)))
                .thenReturn(123L);

        Uri uri = subject.insertStyles(context, db, values);

        assertThat(uri).isNotNull();
        assertThat(uri.toString()).isEqualTo(Style.CONTENT_URI.toString() + "/123");

        verify(db).insert(Style.TABLE_NAME, null, values);
        verify(resolver).notifyChange(Style.CONTENT_URI, null);
    }

    @Test
    public void updateStyle_shouldUpdateDatabase() {
        when(db.update(anyString(), any(ContentValues.class), anyString(), any(String[].class)))
                .thenReturn(777);

        int count = subject.updateStyle(context, db, values, SELECTION, SELECTION_ARGS);

        assertThat(count).isEqualTo(777);

        verify(db).update(Style.TABLE_NAME, values, SELECTION, SELECTION_ARGS);
        verify(resolver).notifyChange(Style.CONTENT_URI, null);
    }

    @Test
    public void deleteStyles_shouldDeleteFromDatabase() throws Exception {
        when(db.delete(anyString(), anyString(), any(String[].class))).thenReturn(123);
        assertThat(subject.deleteStyles(db, SELECTION, SELECTION_ARGS)).isEqualTo(123);
        verify(db).delete(Style.TABLE_NAME, SELECTION, SELECTION_ARGS);
    }

}