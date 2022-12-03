package com.tungsten.fcl.setting;

import static com.tungsten.fcl.setting.ConfigHolder.config;
import static com.tungsten.fclcore.task.FetchTask.DEFAULT_CONCURRENCY;
import static com.tungsten.fclcore.util.Lang.mapOf;
import static com.tungsten.fclcore.util.Pair.pair;

import android.content.Context;

import com.tungsten.fcl.R;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fcl.util.FXUtils;
import com.tungsten.fclcore.download.AdaptedDownloadProvider;
import com.tungsten.fclcore.download.ArtifactMalformedException;
import com.tungsten.fclcore.download.AutoDownloadProvider;
import com.tungsten.fclcore.download.BMCLAPIDownloadProvider;
import com.tungsten.fclcore.download.BalancedDownloadProvider;
import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.download.MojangDownloadProvider;
import com.tungsten.fclcore.fakefx.beans.InvalidationListener;
import com.tungsten.fclcore.task.DownloadException;
import com.tungsten.fclcore.task.FetchTask;
import com.tungsten.fclcore.util.StringUtils;
import com.tungsten.fclcore.util.io.ResponseCodeException;

import javax.net.ssl.SSLHandshakeException;
import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DownloadProviders {
    private DownloadProviders() {}

    private static DownloadProvider currentDownloadProvider;

    public static final Map<String, DownloadProvider> providersById;
    public static final Map<String, DownloadProvider> rawProviders;
    private static final AdaptedDownloadProvider fileDownloadProvider = new AdaptedDownloadProvider();

    private static final MojangDownloadProvider MOJANG;
    private static final BMCLAPIDownloadProvider BMCLAPI;
    private static final BMCLAPIDownloadProvider MCBBS;

    public static final String DEFAULT_PROVIDER_ID = "balanced";
    public static final String DEFAULT_RAW_PROVIDER_ID = "mcbbs";

    private static final InvalidationListener observer;

    static {
        String bmclapiRoot = "https://bmclapi2.bangbang93.com";

        MOJANG = new MojangDownloadProvider();
        BMCLAPI = new BMCLAPIDownloadProvider(bmclapiRoot);
        MCBBS = new BMCLAPIDownloadProvider("https://download.mcbbs.net");
        rawProviders = mapOf(
                pair("mojang", MOJANG),
                pair("bmclapi", BMCLAPI),
                pair("mcbbs", MCBBS)
        );

        AdaptedDownloadProvider fileProvider = new AdaptedDownloadProvider();
        fileProvider.setDownloadProviderCandidates(Arrays.asList(MCBBS, BMCLAPI, MOJANG));
        BalancedDownloadProvider balanced = new BalancedDownloadProvider(Arrays.asList(MCBBS, BMCLAPI, MOJANG));

        providersById = mapOf(
                pair("official", new AutoDownloadProvider(MOJANG, fileProvider)),
                pair("balanced", new AutoDownloadProvider(balanced, fileProvider)),
                pair("mirror", new AutoDownloadProvider(MCBBS, fileProvider)));

        observer = FXUtils.observeWeak(() -> {
            FetchTask.setDownloadExecutorConcurrency(
                    config().getAutoDownloadThreads() ? DEFAULT_CONCURRENCY : config().getDownloadThreads());
        }, config().autoDownloadThreadsProperty(), config().downloadThreadsProperty());
    }

    static void init() {
        FXUtils.onChangeAndOperate(config().versionListSourceProperty(), versionListSource -> {
            if (!providersById.containsKey(versionListSource)) {
                config().setVersionListSource(DEFAULT_PROVIDER_ID);
                return;
            }

            currentDownloadProvider = Optional.ofNullable(providersById.get(versionListSource))
                    .orElse(providersById.get(DEFAULT_PROVIDER_ID));
        });

        FXUtils.onChangeAndOperate(config().downloadTypeProperty(), downloadType -> {
            if (!rawProviders.containsKey(downloadType)) {
                config().setDownloadType(DEFAULT_RAW_PROVIDER_ID);
                return;
            }

            DownloadProvider primary = Optional.ofNullable(rawProviders.get(downloadType))
                    .orElse(rawProviders.get(DEFAULT_RAW_PROVIDER_ID));
            fileDownloadProvider.setDownloadProviderCandidates(
                    Stream.concat(
                            Stream.of(primary),
                            rawProviders.values().stream().filter(x -> x != primary)
                    ).collect(Collectors.toList())
            );
        });
    }

    public static String getPrimaryDownloadProviderId() {
        String downloadType = config().getDownloadType();
        if (providersById.containsKey(downloadType))
            return downloadType;
        else
            return DEFAULT_PROVIDER_ID;
    }

    public static DownloadProvider getDownloadProviderByPrimaryId(String primaryId) {
        return Optional.ofNullable(providersById.get(primaryId))
                .orElse(providersById.get(DEFAULT_PROVIDER_ID));
    }

    /**
     * Get current primary preferred download provider
     */
    public static DownloadProvider getDownloadProvider() {
        return config().isAutoChooseDownloadType() ? currentDownloadProvider : fileDownloadProvider;
    }

    public static String localizeErrorMessage(Context context, Throwable exception) {
        if (exception instanceof DownloadException) {
            URL url = ((DownloadException) exception).getUrl();
            if (exception.getCause() instanceof SocketTimeoutException) {
                return AndroidUtils.getLocalizedText(context, "install_failed_downloading_timeout", url);
            } else if (exception.getCause() instanceof ResponseCodeException) {
                ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
                if (AndroidUtils.hasStringId(context, "download_code_" + responseCodeException.getResponseCode())) {
                    return AndroidUtils.getLocalizedText(context, "download_code_" + responseCodeException.getResponseCode(), url);
                } else {
                    return AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
                }
            } else if (exception.getCause() instanceof FileNotFoundException) {
                return AndroidUtils.getLocalizedText(context, "download_code_404", url);
            } else if (exception.getCause() instanceof AccessDeniedException) {
                return AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + AndroidUtils.getLocalizedText(context, "exception_access_denied", ((AccessDeniedException) exception.getCause()).getFile());
            } else if (exception.getCause() instanceof ArtifactMalformedException) {
                return AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + context.getString(R.string.exception_artifact_malformed);
            } else if (exception.getCause() instanceof SSLHandshakeException) {
                return AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + context.getString(R.string.exception_ssl_handshake);
            } else {
                return AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
            }
        } else if (exception instanceof ArtifactMalformedException) {
            return context.getString(R.string.exception_artifact_malformed);
        } else if (exception instanceof CancellationException) {
            return context.getString(R.string.message_cancelled);
        }
        return StringUtils.getStackTrace(exception);
    }
}
