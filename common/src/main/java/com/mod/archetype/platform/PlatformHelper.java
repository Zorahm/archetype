package com.mod.archetype.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface PlatformHelper {

    PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlatformHelper implementation found"));

    boolean isForge();

    boolean isFabric();

    boolean isClient();

    boolean isDedicatedServer();

    Path getConfigDir();
}
