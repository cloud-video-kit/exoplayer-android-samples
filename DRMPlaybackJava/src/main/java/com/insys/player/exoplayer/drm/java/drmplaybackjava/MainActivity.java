package com.insys.player.exoplayer.drm.java.drmplaybackjava;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer exoPlayer = null;

    private static final String URL = "https://dtkya1w875897.cloudfront.net/da6dc30a-e52f-4af2-9751-000b89416a4e/assets/357577a1-3b61-43ae-9af5-82b9727e2f22/videokit-720p-dash-hls-drm/dash/index.mpd";
    private static final String DRM_LICENSE_URL = "https://insys-marketing.la.drm.cloud/acquire-license/widevine";
    private static final String USER_AGENT = "ExoPlayer-Drm";
    private static final UUID drmSchemeUuid = C.WIDEVINE_UUID;
    private static final Map<String, String> httpHeaders;

    static {
        httpHeaders = new HashMap<>();
        httpHeaders.put("x-drm-brandGuid", "da6dc30a-e52f-4af2-9751-000b89416a4e");
        httpHeaders.put("x-drm-userToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4OTM0NTYwMDAsImRybVRva2VuSW5mbyI6eyJleHAiOiIyMDMwLTAxLTAxVDAwOjAwOjAwKzAwOjAwIiwia2lkIjpbIjFmODNhZTdmLTMwYzgtNGFkMC04MTcxLTI5NjZhMDFiNjU0NyJdLCJwIjp7InBlcnMiOmZhbHNlfX19.hElVqrfK-iLeV_ZleJJO8i-Mf1D2yYVXdtgBE0ja9R4");
    }

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected  void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initializePlayer() {
        // Create a data source factory for HTTP requests
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setTransferListener(new DefaultBandwidthMeter.Builder(this)
                        .setResetOnNetworkTypeChange(false)
                        .build());

        // Create a media source factory for DASH (Dynamic Adaptive Streaming over HTTP)
        DashMediaSource.Factory mediaSourceFactory = new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(dataSourceFactory),
                new DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT)
        );

        // Create a media item with the necessary details
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(URL))  // Set the media URL
                .setMimeType(MimeTypes.APPLICATION_MPD)  // Set the MIME type of the media
                .setDrmConfiguration(
                        new MediaItem.DrmConfiguration.Builder(drmSchemeUuid)
                                .setLicenseUri(DRM_LICENSE_URL)  // Set the DRM license server URL
                                .setLicenseRequestHeaders(httpHeaders)  // Set custom DRM request headers
                                .build()
                )
                .build();

        // Create an ExoPlayer instance using the ExoPlayer.Builder
        exoPlayer = new ExoPlayer.Builder(this)
                .setSeekForwardIncrementMs(10000)  // Set the seek forward increment duration
                .setSeekBackIncrementMs(10000)  // Set the seek backward increment duration
                .build();

        // Set the media source, playWhenReady, and prepare the ExoPlayer
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem));
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare();

        StyledPlayerView playerView = findViewById(R.id.playerView);
        playerView.setPlayer(exoPlayer);
    }
}