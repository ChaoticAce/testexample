package gov.sparrow;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

public class SparrowTestRunner extends RobolectricGradleTestRunner {
    private static final int[] MAX_SDK_LEVEL = {21};

    public SparrowTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    /*
    * Setting config here allows us to forgo using the
    *   @Config(constants = BuildConfig.class)
    * notation in every test class.
    * */
    @Override
    public Config getConfig(Method method) {
        Config config = super.getConfig(method);
        config = new Config.Implementation(MAX_SDK_LEVEL,
                config.manifest(),
                config.qualifiers(),
                config.packageName(),
                config.resourceDir(),
                config.assetDir(),
                config.shadows(),
                config.application(),
                config.libraries(),
                ensureBuildConfig(config.constants()));

        return config;
    }

    private Class<?> ensureBuildConfig(Class<?> constants) {
        if (constants == Void.class) return BuildConfig.class;
        return constants;
    }
}
