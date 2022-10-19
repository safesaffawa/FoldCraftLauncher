package com.tungsten.fclcore.mod;

import static com.tungsten.fclcore.util.io.NetworkUtils.encodeLocation;

import com.tungsten.fclcore.task.FileDownloadTask;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RemoteMod {
    private final String slug;
    private final String author;
    private final String title;
    private final String description;
    private final List<String> categories;
    private final String pageUrl;
    private final String iconUrl;
    private final IMod data;

    public RemoteMod(String slug, String author, String title, String description, List<String> categories, String pageUrl, String iconUrl, IMod data) {
        this.slug = slug;
        this.author = author;
        this.title = title;
        this.description = description;
        this.categories = categories;
        this.pageUrl = pageUrl;
        this.iconUrl = iconUrl;
        this.data = data;
    }

    public String getSlug() {
        return slug;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public IMod getData() {
        return data;
    }

    public enum VersionType {
        Release,
        Beta,
        Alpha
    }

    public enum Type {
        CURSEFORGE,
        MODRINTH
    }

    public interface IMod {
        List<RemoteMod> loadDependencies(RemoteModRepository modRepository) throws IOException;

        Stream<Version> loadVersions(RemoteModRepository modRepository) throws IOException;
    }

    public interface IVersion {
        Type getType();
    }

    public static class Version {
        private final IVersion self;
        private final String modid;
        private final String name;
        private final String version;
        private final String changelog;
        private final Date datePublished;
        private final VersionType versionType;
        private final File file;
        private final List<String> dependencies;
        private final List<String> gameVersions;
        private final List<ModLoaderType> loaders;

        public Version(IVersion self, String modid, String name, String version, String changelog, Date datePublished, VersionType versionType, File file, List<String> dependencies, List<String> gameVersions, List<ModLoaderType> loaders) {
            this.self = self;
            this.modid = modid;
            this.name = name;
            this.version = version;
            this.changelog = changelog;
            this.datePublished = datePublished;
            this.versionType = versionType;
            this.file = file;
            this.dependencies = dependencies;
            this.gameVersions = gameVersions;
            this.loaders = loaders;
        }

        public IVersion getSelf() {
            return self;
        }

        public String getModid() {
            return modid;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getChangelog() {
            return changelog;
        }

        public Date getDatePublished() {
            return datePublished;
        }

        public VersionType getVersionType() {
            return versionType;
        }

        public File getFile() {
            return file;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public List<String> getGameVersions() {
            return gameVersions;
        }

        public List<ModLoaderType> getLoaders() {
            return loaders;
        }
    }

    public static class File {
        private final Map<String, String> hashes;
        private final String url;
        private final String filename;

        public File(Map<String, String> hashes, String url, String filename) {
            this.hashes = hashes;
            this.url = url;
            this.filename = filename;
        }

        public Map<String, String> getHashes() {
            return hashes;
        }

        public FileDownloadTask.IntegrityCheck getIntegrityCheck() {
            if (hashes.containsKey("md5")) {
                return new FileDownloadTask.IntegrityCheck("MD5", hashes.get("sha1"));
            } else if (hashes.containsKey("sha1")) {
                return new FileDownloadTask.IntegrityCheck("SHA-1", hashes.get("sha1"));
            } else if (hashes.containsKey("sha512")) {
                return new FileDownloadTask.IntegrityCheck("SHA-256", hashes.get("sha1"));
            } else {
                return null;
            }
        }

        public String getUrl() {
            return encodeLocation (url);
        }

        public String getFilename() {
            return filename;
        }
    }
}
