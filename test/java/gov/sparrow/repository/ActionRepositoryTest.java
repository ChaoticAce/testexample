package gov.sparrow.repository;

import android.content.ContentValues;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.builders.ActionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static gov.sparrow.contracts.ActionContract.Action;
import static gov.sparrow.contracts.ActionContract.ActionListPosition;
import static gov.sparrow.repository.ActionRepository.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ActionRepositoryTest {

    @Mock AsyncRepositoryHelper asyncRepositoryHelper;
    @Captor ArgumentCaptor<UpdateActionListener> updateListenerCaptor;
    private ActionRepository subject;

    @Before
    public void setUp() {
        initMocks(this);
        subject = new ActionRepository(asyncRepositoryHelper);
    }

    @Test
    public void createAction_startsAsyncInsert() {
        ActionRepository.CreateActionListener listener = mock(ActionRepository.CreateActionListener.class);
        subject.createAction("test action", listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Action.COLUMN_NAME_TITLE, "test action");

        verify(asyncRepositoryHelper).startInsert(
                eq(CREATE_ACTION_TOKEN),
                any(UpdateActionListener.class),
                eq(Action.CONTENT_URI),
                eq(expectedValues));
    }

    @Test
    public void updateActionCheckMark_startsAsyncUpdate() {
        ActionRepository.UpdateActionListener listener = mock(ActionRepository.UpdateActionListener.class);
        subject.updateActionCompleted(1L, "test time", true, listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Action.COLUMN_NAME_COMPLETED, Boolean.toString(true));
        expectedValues.put(Action.COLUMN_NAME_CHECKBOX_UPDATED_AT, "test time");

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_ACTION_TOKEN),
                any(UpdateActionListener.class),
                eq(Action.CONTENT_URI),
                eq(expectedValues),
                eq("_id=?"),
                eq(new String[]{Long.toString(1L)}));
    }

    @Test
    public void updateAction_startsAsyncUpdate() {
        ActionRepository.UpdateActionListener listener = mock(ActionRepository.UpdateActionListener.class);
        gov.sparrow.models.Action action = ActionBuilder.actionBuilder()
                .title("test action title")
                .noteId(12L)
                .id(1L)
                .dueDate("test action due date")
                .build();
        subject.updateAction(action, listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Action.COLUMN_NAME_TITLE, "test action title");
        expectedValues.put(Action.COLUMN_NAME_DUE_DATE, "test action due date");

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_ACTION_TOKEN),
                updateListenerCaptor.capture(),
                eq(Action.CONTENT_URI(12L, 1L)),
                eq(expectedValues),
                eq("_id=?"),
                eq(new String[]{Long.toString(1L)}));

        assertThat(updateListenerCaptor.getValue()).isEqualTo(listener);
    }

    @Test
    public void updateActionBody_withNullNoteId_startsAsyncUpdate() {
        ActionRepository.UpdateActionListener listener = mock(ActionRepository.UpdateActionListener.class);
        gov.sparrow.models.Action action = ActionBuilder.actionBuilder()
                .title("test action title")
                .id(1L)
                .dueDate("test action due date")
                .build();
        subject.updateAction(action, listener);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(Action.COLUMN_NAME_TITLE, "test action title");
        expectedValues.put(Action.COLUMN_NAME_DUE_DATE, "test action due date");

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_ACTION_TOKEN),
                any(UpdateActionListener.class),
                eq(Action.CONTENT_URI),
                eq(expectedValues),
                eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateActionListPosition_startsAsyncUpdate() throws Exception {
        String selection = ActionListPosition.COLUMN_NAME_ACTION_ID + "=?";
        ContentValues expectedValues = new ContentValues();
        expectedValues.put(ActionListPosition.COLUMN_NAME_POSITION, 5);

        subject.updateActionPosition(1L, 5);

        verify(asyncRepositoryHelper).startUpdate(
                eq(UPDATE_ACTION_TOKEN),
                isNull(),
                eq(ActionListPosition.CONTENT_URI),
                eq(expectedValues),
                eq(selection),
                eq(new String[]{"1"}));
    }

    @Test
    public void archiveAction_startsAsyncDelete() throws Exception {
        String selection = Action._ID + "=?";
        subject.archiveAction(1L);
        verify(asyncRepositoryHelper).startDelete(
                eq(DELETE_ACTION_TOKEN),
                isNull(UpdateActionListener.class),
                eq(Action.CONTENT_URI(1L)),
                eq(selection),
                eq(new String[]{"1"}));
    }
}
