package gov.sparrow.listeners;

import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.adapter.ActionViewPagerAdapter;
import gov.sparrow.models.Action;
import gov.sparrow.models.builders.ActionBuilder;
import gov.sparrow.repository.ActionRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ActionItemSettingsDialogSaveListenerTest {

    @Mock DialogFragment dialog;
    @Mock ViewPager viewPager;
    @Mock ActionViewPagerAdapter adapter;
    @Mock ActionViewPagerAdapter.ActionItemUpdatingFragment fragment;
    @Mock ActionViewPagerAdapter.ActionItemUpdatingFragment fragment2;
    @Mock ActionBuilder actionBuilder;
    @Mock ActionBuilder actionBuilder2;
    @Captor ArgumentCaptor<ActionRepository.UpdateActionListener> updateCaptor;
    @Inject ActionRepository actionRepository;
    private ActionItemSettingsDialogSaveListener subject;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(viewPager.getAdapter()).thenReturn(adapter);
        when(adapter.getFragment(0)).thenReturn(fragment);
        when(adapter.getFragment(1)).thenReturn(fragment2);
        when(adapter.getCount()).thenReturn(2);

        when(fragment.applyChanges(any(ActionBuilder.class))).thenReturn(actionBuilder);
        when(fragment2.applyChanges(actionBuilder)).thenReturn(actionBuilder2);

        subject = new ActionItemSettingsDialogSaveListener(dialog, viewPager, actionRepository);
    }

    @Test
    public void onClick_shouldCallSaveOnCurrentFragment() {
        subject.onClick(null);
        verify(fragment).applyChanges(any(ActionBuilder.class));
        verify(fragment2).applyChanges(actionBuilder);
    }

    @Test
    public void onSave_shouldDismissDialog() {
        subject.onClick(null);
        verify(actionRepository).updateAction(any(Action.class), updateCaptor.capture());
        updateCaptor.getValue().onUpdateActionComplete(1);
        verify(dialog).dismiss();
    }

}