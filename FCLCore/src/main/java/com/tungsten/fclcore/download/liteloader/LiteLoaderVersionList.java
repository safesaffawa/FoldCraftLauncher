package com.tungsten.fclcore.download.liteloader;

import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.download.VersionList;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fclcore.util.versioning.VersionNumber;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class LiteLoaderVersionList extends VersionList<LiteLoaderRemoteVersion> {

    private final DownloadProvider downloadProvider;

    public LiteLoaderVersionList(DownloadProvider downloadProvider) {
        this.downloadProvider = downloadProvider;
    }

    @Override
    public boolean hasType() {
        return false;
    }

    private void doBranch(String key, String gameVersion, LiteLoaderRepository repository, LiteLoaderBranch branch, boolean snapshot) {
        if (branch == null || repository == null)
            return;

        for (Map.Entry<String, LiteLoaderVersion> entry : branch.getLiteLoader().entrySet()) {
            String branchName = entry.getKey();
            LiteLoaderVersion v = entry.getValue();
            if ("latest".equals(branchName))
                continue;

            String version = v.getVersion();
            String url = repository.getUrl() + "com/mumfrey/liteloader/" + gameVersion + "/" + v.getFile();
            if (snapshot) {
                try {
                    version = version.replace("SNAPSHOT", getLatestSnapshotVersion(repository.getUrl() + "com/mumfrey/liteloader/" + v.getVersion() + "/"));
                    url = repository.getUrl() + "com/mumfrey/liteloader/" + v.getVersion() + "/liteloader-" + version + "-release.jar";
                } catch (Exception ignore) {
                }
            }

            versions.put(key, new LiteLoaderRemoteVersion(gameVersion,
                    version, Collections.singletonList(url),
                    v.getTweakClass(), v.getLibraries()
            ));
        }
    }

    @Override
    public CompletableFuture<?> refreshAsync() {
        return HttpRequest.GET(downloadProvider.injectURL(LITELOADER_LIST)).getJsonAsync(LiteLoaderVersionsRoot.class)
                .thenAcceptAsync(root -> {
                    lock.writeLock().lock();

                    try {
                        versions.clear();

                        for (Map.Entry<String, LiteLoaderGameVersions> entry : root.getVersions().entrySet()) {
                            String gameVersion = entry.getKey();
                            LiteLoaderGameVersions liteLoader = entry.getValue();

                            String gg = VersionNumber.normalize(gameVersion);
                            doBranch(gg, gameVersion, liteLoader.getRepoitory(), liteLoader.getArtifacts(), false);
                            doBranch(gg, gameVersion, liteLoader.getRepoitory(), liteLoader.getSnapshots(), true);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                });
    }

    public static final String LITELOADER_LIST = "http://dl.liteloader.com/versions/versions.json";

    private static String getLatestSnapshotVersion(String repo) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(repo + "maven-metadata.xml");
        Element r = doc.getDocumentElement();
        Element snapshot = (Element) r.getElementsByTagName("snapshot").item(0);
        Node timestamp = snapshot.getElementsByTagName("timestamp").item(0);
        Node buildNumber = snapshot.getElementsByTagName("buildNumber").item(0);
        return timestamp.getTextContent() + "-" + buildNumber.getTextContent();
    }
}
