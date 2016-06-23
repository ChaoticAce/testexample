package gov.sparrow.provider.services;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import gov.sparrow.SparrowTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SearchServiceTest {

    public static final String SORT_ORDER = "created_at DESC";
    public static final String QUERY_STRING = "query string";

    @Mock private SQLiteDatabase db;
    @Mock private SQLiteQueryBuilder builder;
    @Mock private Cursor expectedCursor;

    @Captor private ArgumentCaptor<String[]> queryCaptor;

    private SearchService subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = new SearchService();
    }

    @Test
    public void query_shouldQueryDatabase() {
        when(db.rawQuery(anyString(), isNull(String[].class)))
                .thenReturn(expectedCursor);

        String[] subQueries = new String[]{
                "SELECT notes._id as _id, notes.title as title, substr(notes.body, 0, 128) as body, notes.notebook_id as notebook_id, notebooks.title as notebook_title, NULL as completed, type as type, NULL as due_date, created_at as created_at FROM searchable_notes INNER JOIN notes ON (docid = notes._id) LEFT JOIN notebooks ON (notebook_id=notebooks._id) WHERE searchable_notes MATCH 'test text*' AND notes.archived='false'",
                "SELECT actions._id as _id, actions.title as title, notes.title as body, NULL as notebook_id, NULL as notebook_title, completed as completed, actions.type as type, due_date as due_date, actions.created_at as created_at FROM searchable_actions INNER JOIN actions ON (docid = actions._id) LEFT JOIN notes ON (actions.note_id = notes._id) WHERE searchable_actions MATCH 'test text*' AND actions.archived='false'"
        };
        when(builder.buildUnionQuery(subQueries, SORT_ORDER, null))
                .thenReturn(QUERY_STRING);

        Cursor cursor = subject.query(db, builder, "test text", SORT_ORDER);
        assertThat(cursor).isEqualTo(expectedCursor);

        verify(builder).buildUnionQuery(queryCaptor.capture(), eq(SORT_ORDER), isNull(String.class));

        String[] actualQueries = queryCaptor.getValue();
        assertThat(actualQueries[0]).isEqualTo(subQueries[0]);
        assertThat(actualQueries[1]).isEqualTo(subQueries[1]);

        verify(db).rawQuery(QUERY_STRING, null);
    }

    @Test
    public void query_shouldSanitizesQueryString() throws Exception {
        when(db.rawQuery(anyString(), isNull(String[].class)))
                .thenReturn(expectedCursor);

        String[] subQueries = new String[]{
                "SELECT notes._id as _id, notes.title as title, substr(notes.body, 0, 128) as body, notes.notebook_id as notebook_id, notebooks.title as notebook_title, NULL as completed, type as type, NULL as due_date, created_at as created_at FROM searchable_notes INNER JOIN notes ON (docid = notes._id) LEFT JOIN notebooks ON (notebook_id=notebooks._id) WHERE searchable_notes MATCH ' test  text  select *  from   notes  where _id=23*' AND notes.archived='false'",
                "SELECT actions._id as _id, actions.title as title, notes.title as body, NULL as notebook_id, NULL as notebook_title, completed as completed, actions.type as type, due_date as due_date, actions.created_at as created_at FROM searchable_actions INNER JOIN actions ON (docid = actions._id) LEFT JOIN notes ON (actions.note_id = notes._id) WHERE searchable_actions MATCH ' test  text  select *  from   notes  where _id=23*' AND actions.archived='false'"
        };
        when(builder.buildUnionQuery(subQueries, SORT_ORDER, null))
                .thenReturn(QUERY_STRING);

        subject.query(db, builder, "'test' text' select * \"from\" -notes -where _id=23", SORT_ORDER);

        verify(builder).buildUnionQuery(queryCaptor.capture(), eq(SORT_ORDER), isNull(String.class));

        String[] actualQueries = queryCaptor.getValue();
        assertThat(actualQueries[0]).isEqualTo(subQueries[0]);
        assertThat(actualQueries[1]).isEqualTo(subQueries[1]);
    }
}