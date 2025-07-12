package abhay.live.now.drmprotectedmediaplayer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.common.C;
import androidx.media3.ui.PlayerView;

@UnstableApi public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;

    private final String manifestUrl = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd";
    private final String licenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth";

    private TrackSelector trackSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.playerView);
        initializePlayer();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        trackSelector = new DefaultTrackSelector(this);

        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);

        MediaItem.DrmConfiguration drmConfiguration =
                new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(licenseUrl)
                        .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(manifestUrl))
                .setDrmConfiguration(drmConfiguration)
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onTracksChanged(Tracks tracks) {
                logAvailableVideoResolutions();
            }
        });

        logAvailableVideoResolutions();
    }

    private void logAvailableVideoResolutions() {
        if (player == null) return;

        Tracks tracks = player.getCurrentTracks();

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_VIDEO) {
                for (int i = 0; i < group.length; i++) {
                    if (group.isTrackSupported(i)) {
                        Format format = group.getTrackFormat(i);
                        int width = format.width;
                        int height = format.height;
                        int bitrate = format.bitrate;

                        Log.d("VideoResolution", "Track " + i +
                                ": " + width + "x" + height + " (" + height + "p), bitrate: " + bitrate);
                    }
                }
            }
        }
    }
}