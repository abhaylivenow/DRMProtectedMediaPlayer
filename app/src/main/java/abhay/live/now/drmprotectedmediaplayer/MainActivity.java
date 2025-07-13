package abhay.live.now.drmprotectedmediaplayer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.common.C;
import androidx.media3.ui.PlayerView;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

@UnstableApi public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;

    private Button resolutionButton;

    private final String manifestUrl = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd";
    private final String licenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth";

    private TrackSelector trackSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.playerView);
        resolutionButton = findViewById(R.id.btnResolution);
        initializePlayer();

        resolutionButton.setOnClickListener(view -> showResolutionDialog());
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
    }

    private void showResolutionDialog() {
        if (player == null || trackSelector == null) return;

        Tracks currentTracks = player.getCurrentTracks();
        TrackGroup selectedTrackGroup = null;
        List<Integer> availableTrackIndices = new ArrayList<>();
        List<String> resolutionLabels = new ArrayList<>();

        for (Tracks.Group group : currentTracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_VIDEO) {
                for (int i = 0; i < group.length; i++) {
                    if (group.isTrackSupported(i)) {
                        Format format = group.getTrackFormat(i);
                        resolutionLabels.add(format.height + "p");
                        availableTrackIndices.add(i);

                        if (selectedTrackGroup == null) {
                            selectedTrackGroup = group.getMediaTrackGroup(); // only need this once
                        }
                    }
                }
            }
        }

        if (selectedTrackGroup == null || resolutionLabels.isEmpty()) {
            Toast.makeText(this, "No video resolutions available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] resolutionArray = resolutionLabels.toArray(new String[0]);

        TrackGroup finalSelectedTrackGroup = selectedTrackGroup;
        new AlertDialog.Builder(this)
                .setTitle("Select Resolution")
                .setItems(resolutionArray, (dialog, which) -> {
                    int trackIndex = availableTrackIndices.get(which);

                    TrackSelectionOverride override = new TrackSelectionOverride(
                            finalSelectedTrackGroup,
                            ImmutableList.of(trackIndex)
                    );

                    DefaultTrackSelector.Parameters parameters = (DefaultTrackSelector.Parameters) trackSelector
                            .getParameters()
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .addOverride(override)
                            .build();

                    trackSelector.setParameters(parameters);
                })
                .show();
    }

}