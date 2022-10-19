package com.tungsten.fclcore.download.fabric;

import com.tungsten.fclcore.download.DefaultDependencyManager;
import com.tungsten.fclcore.game.Version;
import com.tungsten.fclcore.task.FileDownloadTask;
import com.tungsten.fclcore.task.Task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <b>Note</b>: Fabric should be installed first.
 */
public final class FabricAPIInstallTask extends Task<Version> {

    private final DefaultDependencyManager dependencyManager;
    private final Version version;
    private final FabricAPIRemoteVersion remote;
    private final List<Task<?>> dependencies = new ArrayList<>(1);

    public FabricAPIInstallTask(DefaultDependencyManager dependencyManager, Version version, FabricAPIRemoteVersion remoteVersion) {
        this.dependencyManager = dependencyManager;
        this.version = version;
        this.remote = remoteVersion;
    }

    @Override
    public Collection<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean isRelyingOnDependencies() {
        return false;
    }

    @Override
    public void execute() throws IOException {
        dependencies.add(new FileDownloadTask(
                new URL(remote.getVersion().getFile().getUrl()),
                dependencyManager.getGameRepository().getRunDirectory(version.getId()).toPath().resolve("mods").resolve("fabric-api-" + remote.getVersion().getVersion() + ".jar").toFile(),
                remote.getVersion().getFile().getIntegrityCheck())
        );
    }
}
