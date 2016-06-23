package gov.sparrow.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.models.Action;
import gov.sparrow.models.builders.ActionBuilder;
import gov.sparrow.repository.ActionRepository;
import gov.sparrow.util.TimeUtil;

import static gov.sparrow.fragment.ActionDueDateFragment.ACTION_DUE_DATE;
import static gov.sparrow.util.TimeUtil.DATABASE_DUE_DATE_TIME_FORMAT;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;


@RunWith(SparrowTestRunner.class)
public class ActionDueDateFragmentTest {

    @Inject TimeUtil timeUtil;
    @Inject ActionRepository actionRepository;
    @Mock ActionRepository.UpdateActionListener listener;
    private ActionDueDateFragment subject;
    private Calendar today;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        today = Calendar.getInstance();
        subject = ActionDueDateFragment.newInstance(1L, null);
        startFragment(subject);
    }

    @Test
    public void onCreateView_shouldHideEmptyView_whenCurrentDueDateExists() {
        subject = ActionDueDateFragment.newInstance(2L, "1999-02-22 00:00:00.000");
        startFragment(subject);

        assertThat(subject.dueDateSelectionContainer).isVisible();
        assertThat(subject.emptyDateView).isNotVisible();
    }

    @Test
    public void onCreateView_shouldHideDateView_whenNoCurrentDueDate() {
        assertThat(subject.dueDateSelectionContainer).isNotVisible();
        assertThat(subject.emptyDateView).isVisible();
    }

    @Test
    public void onCreateView_whenDueDateGiven_shouldShowGivenDate() throws Exception {
        subject = ActionDueDateFragment.newInstance(3L, "2016-05-25 00:00:00.000");
        startFragment(subject);

        assertThat(subject.actionDatePicker.getYear()).isEqualTo(2016);
        assertThat(subject.actionDatePicker.getMonth()).isEqualTo(Calendar.MAY);
        assertThat(subject.actionDatePicker.getDayOfMonth()).isEqualTo(25);
        assertThat(subject.actionDayOfTheWeek).hasText("Wednesday");
        assertThat(subject.actionDayMonth).hasText("25 May");
        assertThat(subject.actionYear).hasText("2016");
    }

    @Test
    public void onDateChanged_shouldDisplaySelectedDateInfo() throws Exception {
        subject.dueDateSwitch.setChecked(true);
        subject.onDateChanged(null, 2016, 4, 25);

        assertThat(subject.actionDayOfTheWeek).hasText("Wednesday");
        assertThat(subject.actionDayMonth).hasText("25 May");
        assertThat(subject.actionYear).hasText("2016");
    }

    @Test
    public void save_shouldSaveDueDate() throws Exception {
        subject.dueDateSwitch.setChecked(true);
        subject.actionDatePicker.updateDate(2016, 4, 25);

        ActionBuilder actionBuilder = ActionBuilder.actionBuilder();
        subject.applyChanges(actionBuilder);

        Action action = actionBuilder.build();

        assertThat(action.getId()).isEqualTo(1L);
        assertThat(action.getDueDate()).isEqualTo("2016-05-25 00:00:00.000");
    }

    @Test
    public void save_whenNoDueDateSet_shouldSaveNoDueDate() {
        ActionBuilder actionBuilder = ActionBuilder.actionBuilder();
        subject.applyChanges(actionBuilder);

        Action action = actionBuilder.build();

        assertThat(action.getId()).isEqualTo(1L);
        assertThat(action.getDueDate()).isNull();
    }

    @Test
    public void switchingDueDateOff_showsEmptyDateView() throws Exception {
        subject.dueDateSwitch.setChecked(true);
        subject.dueDateSwitch.setChecked(false);

        assertThat(subject.emptyDateView).isVisible();
        assertThat(subject.dueDateSelectionContainer).isGone();
    }

    @Test
    public void switchingDueDateOff_shouldDisableDatePickerWithCover() throws Exception {
        subject.dueDateSwitch.setChecked(true);
        subject.dueDateSwitch.setChecked(false);

        assertThat(subject.datePickerCover).isVisible();
    }

    @Test
    public void switchingDueDateOn_showsDueDateDisplayWithCurrentDate() throws Exception {
        subject.dueDateSwitch.setChecked(true);

        assertThat(subject.actionDayOfTheWeek).hasText(today.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
        assertThat(subject.actionDayMonth).hasText(today.get(Calendar.DAY_OF_MONTH) + " " +
                today.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        assertThat(subject.actionYear).hasText(String.valueOf(today.get(Calendar.YEAR)));
        assertThat(subject.dueDateSelectionContainer).isVisible();
        assertThat(subject.emptyDateView).isGone();
    }

    @Test
    public void switchingDueDateOn_shouldSetDatePicker_toCurrentDate() throws Exception {
        subject.dueDateSwitch.setChecked(true);

        assertThat(subject.actionDatePicker).hasDayOfMonth(today.get(Calendar.DAY_OF_MONTH));
        assertThat(subject.actionDatePicker).hasMonth(today.get(Calendar.MONTH));
        assertThat(subject.actionDatePicker).hasYear(today.get(Calendar.YEAR));
    }

    @Test
    public void switchingDueDateOn_shouldEnableDatePicker() throws Exception {
        subject.dueDateSwitch.setChecked(true);

        assertThat(subject.datePickerCover).isGone();
    }

    @Test
    public void onCreateView_whenDueDateNotSet_shouldDisableActionDatePickerWithCover() throws Exception {
        assertThat(subject.datePickerCover).isVisible();
    }

    @Test
    public void whenActionDueDateIsSet_shouldEnableDatePicker_withSetDate() throws Exception {
        subject = ActionDueDateFragment.newInstance(1l, "2016-05-25 00:00:00.000");
        startFragment(subject);

        assertThat(subject.datePickerCover).isGone();
    }

    @Test
    public void whenActionDueDateIsSet_shouldSetDueDateSwitchOn() throws Exception {
        subject = ActionDueDateFragment.newInstance(1l, "2016-05-25 00:00:00.000");
        startFragment(subject);

        assertThat(subject.dueDateSwitch).isChecked();
    }

    @Test
    public void onSaveInstanceState_savesSetDueDate() throws Exception {
        Bundle instanceState = new Bundle();

        subject.dueDateSwitch.setChecked(true);
        subject.onSaveInstanceState(instanceState);

        assertThat(instanceState).hasKey(ACTION_DUE_DATE);
        SimpleDateFormat formatter = new SimpleDateFormat(DATABASE_DUE_DATE_TIME_FORMAT, Locale.getDefault());
        String expectedDate = formatter.format(today.getTime());
        assertThat(instanceState.get(ACTION_DUE_DATE)).isEqualTo(expectedDate);
    }

    @Test
    public void onRecreatingView_whenDueDateSet_shouldApplyDueDate() throws Exception {
        Bundle instanceState = new Bundle();
        SimpleDateFormat formatter = new SimpleDateFormat(DATABASE_DUE_DATE_TIME_FORMAT, Locale.getDefault());
        today.set(2016, 6, 13);
        String expectedDate = formatter.format(today.getTime());
        instanceState.putString(subject.ACTION_DUE_DATE, expectedDate);

        subject.onCreateView(LayoutInflater.from(RuntimeEnvironment.application), null, instanceState);

        assertThat(subject.actionDatePicker.getDayOfMonth()).isEqualTo(today.get(Calendar.DAY_OF_MONTH));
        assertThat(subject.actionDatePicker.getMonth()).isEqualTo(today.get(Calendar.MONTH));
        assertThat(subject.actionDatePicker.getYear()).isEqualTo(today.get(Calendar.YEAR));
    }

}