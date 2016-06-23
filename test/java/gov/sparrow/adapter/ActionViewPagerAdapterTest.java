package gov.sparrow.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.fragment.ActionDueDateFragment;
import gov.sparrow.fragment.ActionEditFragment;
import gov.sparrow.util.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class ActionViewPagerAdapterTest {

    private TestActivity activity;
    private ViewPager viewPager;
    private ActionViewPagerAdapter subject;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.setupActivity(TestActivity.class);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        viewPager = new ViewPager(activity);

        subject = new ActionViewPagerAdapter(activity, fragmentManager, 1L, 22L, "test action body", "test note title", "test action due date");
        viewPager.setAdapter(subject);
    }

    @Test
    public void getItem_shouldReturnActionEditFragment() throws Exception {
        assertThat(subject.getItem(subject.EDIT_POSITION)).isInstanceOf(ActionEditFragment.class);
    }

    @Test
    public void getItem_shouldReturnActionDueDateFragment() throws Exception {
        assertThat(subject.getItem(subject.DUE_DATE_POSITION)).isInstanceOf(ActionDueDateFragment.class);
    }


    @Test
    public void getPageTitle_shouldReturnEditTitle() {
        assertThat(subject.getPageTitle(subject.EDIT_POSITION)).isEqualTo(ActionEditFragment.FRAGMENT_TITLE);
    }

    @Test
    public void getPageTitle_shouldReturnDueDateTitle() throws Exception {
        assertThat(subject.getPageTitle(subject.DUE_DATE_POSITION)).isEqualTo(ActionDueDateFragment.FRAGMENT_TITLE);
    }

    @Test
    public void getPageIcon_shouldReturnEditIcon() {

        int drawable = subject.getPageIcon(subject.EDIT_POSITION);
        int expectedDrawable = ActionEditFragment.FRAGMENT_ICON_DRAWABLE;
        assertThat(drawable).isEqualTo(expectedDrawable);
    }

    @Test
    public void getPageIcon_shouldReturnDueDateIcon() throws Exception {
        int drawable = subject.getPageIcon(subject.DUE_DATE_POSITION);
        int expectedDrawable = ActionDueDateFragment.FRAGMENT_ICON_DRAWABLE;
        assertThat(drawable).isEqualTo(expectedDrawable);
    }

    @Test
    public void instantiateFragment_shouldAddEditFragmentPosition() {
        Fragment fragment = (Fragment) subject.instantiateItem(viewPager, subject.EDIT_POSITION);
        assertThat(subject.getFragment(subject.EDIT_POSITION)).isEqualTo((ActionViewPagerAdapter.ActionItemUpdatingFragment) fragment);
    }

    @Test
    public void instantiateFragment_shouldAddDueDateFragmentPosition() throws Exception {
        Fragment fragment = (Fragment) subject.instantiateItem(viewPager, subject.DUE_DATE_POSITION);
        assertThat(subject.getFragment(subject.DUE_DATE_POSITION)).isEqualTo((ActionViewPagerAdapter.ActionItemUpdatingFragment) fragment);
    }

    @Test
    public void destroyItem_shouldRemoveEditFragmentPosition() {
        Object object = subject.instantiateItem(viewPager, subject.EDIT_POSITION);
        subject.destroyItem(viewPager, subject.EDIT_POSITION, object);
        assertThat(subject.getFragment(subject.EDIT_POSITION)).isNull();
    }

    @Test
    public void destroyItem_shouldRemoveDueDateFragmentPosition() throws Exception {
        Object object = subject.instantiateItem(viewPager, subject.DUE_DATE_POSITION);
        subject.destroyItem(viewPager, subject.DUE_DATE_POSITION, object);
        assertThat(subject.getFragment(subject.DUE_DATE_POSITION)).isNull();
    }

}