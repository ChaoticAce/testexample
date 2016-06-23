package gov.sparrow;

import gov.sparrow.util.DaggerTestSparrowApplicationComponent;
import gov.sparrow.util.TestSparrowApplicationComponent;
import gov.sparrow.util.TestSparrowApplicationModule;

/*
* Robolectric will automatically use Test<YOUR APPLICATION NAME> in place of <YOUR APPLICATION NAME>
* Append "Test" to the production application name and extend the application.
* */
public class TestSparrowApplication extends SparrowApplication {

    private TestSparrowApplicationComponent component;

    @Override
    public TestSparrowApplicationComponent getAppComponent() {
        if (component == null) {
            component = DaggerTestSparrowApplicationComponent.builder()
                    .testSparrowApplicationModule(new TestSparrowApplicationModule(this))
                    .build();
        }
        return component;
    }
}
