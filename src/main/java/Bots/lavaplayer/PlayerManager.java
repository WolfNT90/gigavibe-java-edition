package Bots.lavaplayer;

import com.github.topislavalinkplugins.topissourcemanagers.applemusic.AppleMusicSourceManager;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifyConfig;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Bots.Main.*;

public class PlayerManager {

    private static PlayerManager INSTANCE;
    private static boolean hasSpotify = false;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        SpotifyConfig spotifyConfig = new SpotifyConfig();
        spotifyConfig.setClientId(Dotenv.load().get("SPOTIFYCLIENTID"));
        spotifyConfig.setClientSecret(Dotenv.load().get("SPOTIFYCLIENTSECRET"));
        spotifyConfig.setCountryCode("GB");

        this.audioPlayerManager.registerSourceManager(new AppleMusicSourceManager(null, "gb", this.audioPlayerManager));
        try {
            this.audioPlayerManager.registerSourceManager(new SpotifySourceManager(null, spotifyConfig, this.audioPlayerManager));
            hasSpotify = true;
        } catch (Exception exception) {
            printlnTime("Spotify manager was unable to load due to a complication. Continuing without it...\nError: " + exception);
            hasSpotify = false;
        }

        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void loadAndPlay(GuildMessageChannelUnion commandChannel, String trackUrl, Boolean sendEmbed) {
        if (trackUrl.toLowerCase().contains("spotify")) {
            if (!hasSpotify) {
                commandChannel.sendMessageEmbeds(createQuickError("There was an error and the spotify track could not load.")).queue();
                return;
            }
        }
        final GuildMusicManager musicManager = this.getMusicManager(commandChannel.getGuild());
        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                audioTrack.setUserData(commandChannel);
                if (!sendEmbed) {
                    musicManager.scheduler.queue(audioTrack);
                    return;
                }
                String length;
                musicManager.scheduler.queue(audioTrack);
                EmbedBuilder embed = new EmbedBuilder();
                embed.setThumbnail(getThumbURL(audioTrack));
                if (audioTrack.getInfo().length > 432000000 || audioTrack.getInfo().length < 0 || audioTrack.getInfo().uri.toLowerCase().endsWith("opus")) {
                    length = "Unknown";
                } else {
                    length = toTimestamp((audioTrack.getInfo().length));
                }
                embed.setColor(botColour);
                if (audioTrack.getInfo().title.isEmpty()) {
                    String[] trackNameArray = audioTrack.getInfo().identifier.split("/");
                    String trackName = trackNameArray[trackNameArray.length - 1];
                    embed.setTitle((trackName), (audioTrack.getInfo().uri));
                } else {
                    embed.setTitle(audioTrack.getInfo().title, (audioTrack.getInfo().uri));
                }
                embed.setDescription("Duration: `" + length + "`\n" + "Channel: `" + audioTrack.getInfo().author + "`");
                commandChannel.sendMessageEmbeds(embed.build()).queue();
            }


            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                String length;
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(botColour);
                final List<AudioTrack> tracks = audioPlaylist.getTracks();
                if (!tracks.isEmpty()) {
                    String author = (tracks.get(0).getInfo().author);
                    if (tracks.get(0).getInfo().length > 432000000 || tracks.get(0).getInfo().length < 0 || tracks.get(0).getInfo().uri.toLowerCase().endsWith("opus")) { // 5 days
                        length = "Unknown";
                    } else {
                        length = toTimestamp((tracks.get(0).getInfo().length));
                    }
                    if (tracks.size() == 1 || audioPlaylist.getName().contains("Search results for:")) {
                        if (!sendEmbed) {
                            musicManager.scheduler.queue(tracks.get(0));
                            return;
                        }
                        musicManager.scheduler.queue(tracks.get(0));
                        embed.setThumbnail(getThumbURL(tracks.get(0)));
                        embed.setTitle((tracks.get(0).getInfo().title), (tracks.get(0).getInfo().uri));
                        embed.setDescription("Duration: `" + length + "`\n" + "Channel: `" + author + "`");
                        commandChannel.sendMessageEmbeds(embed.build()).queue();
                    } else {
                        long lengthSeconds = 0;
                        for (int i = 0; i < tracks.size(); ) {
                            lengthSeconds = (lengthSeconds + tracks.get(i).getInfo().length);
                            musicManager.scheduler.queue(tracks.get(i));
                            i++;
                        }
                        embed.setTitle(audioPlaylist.getName().replaceAll("&amp;", "&").replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("\\\\", "\\\\\\\\"));
                        embed.appendDescription("Size: **" + tracks.size() + "** tracks.\nLength: **" + toTimestamp(lengthSeconds) + "**\n\n");

                        for (int i = 0; i < tracks.size(); ) {
                            if (i > 4 || tracks.get(i) == null) {
                                break;
                            }
                            if (tracks.get(i).getInfo().title == null) {
                                embed.appendDescription(i + 1 + ". [" + tracks.get(i).getInfo().identifier + "](" + tracks.get(i).getInfo().uri + ")\n");
                            } else {
                                embed.appendDescription(i + 1 + ". [" + tracks.get(i).getInfo().title + "](" + tracks.get(i).getInfo().uri + ")\n");
                            }
                            i++;
                        }
                        if (tracks.size() > 5) {
                            embed.appendDescription("...");
                        }
                        embed.setThumbnail(getThumbURL(tracks.get(0)));
                        commandChannel.sendMessageEmbeds(embed.build()).queue();
                    }
                    for (int i = 0; i < tracks.size(); ) {
                        tracks.get(i).setUserData(commandChannel);
                        i++;
                    }
                }
            }

            @Override
            public void noMatches() {
                commandChannel.sendMessageEmbeds(createQuickError("No matches found for the track.")).queue();
                printlnTime("No match found for the track.");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                clearVotes(commandChannel.getGuild().getIdLong());
                commandChannel.sendMessageEmbeds(createQuickError("The track failed to load.\n\n```\n" + e.getMessage() + "\n```")).queue();
                printlnTime("track loading failed, stacktrace: ");
                e.printStackTrace();
            }
        });
        try {
            float check1 = musicManager.audioPlayer.getPlayingTrack().getPosition();
            Thread.sleep(500);
            if (musicManager.audioPlayer.getPlayingTrack().getPosition() == check1) {
                loadAndPlay(commandChannel, musicManager.audioPlayer.getPlayingTrack().getInfo().uri, false);
                musicManager.scheduler.nextTrack();
            }
        } catch (Exception ignored) {
        }
    }

    public String getThumbURL(AudioTrack track) {
        if (track.getInfo().uri.toLowerCase().contains("youtube")) {
            return "https://img.youtube.com/vi/" + track.getIdentifier() + "/0.jpg";
        }
        if (track.getInfo().uri.toLowerCase().contains("spotify")) {
            try {
                URL url = new URL("https://embed.spotify.com/oembed/?url=" + track.getInfo().uri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                reader.close();
                Pattern pattern = Pattern.compile("\"thumbnail_url\":\"([^\"]+)\",\"");
                Matcher matcher = pattern.matcher(output.toString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return "";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
        if (track.getInfo().uri.toLowerCase().contains("soundcloud")) {
            try {
                URL url = new URL(track.getInfo().uri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                reader.close();
                Pattern pattern = Pattern.compile("<img src=\"([^\"]+)\" width=\"");
                Matcher matcher = pattern.matcher(output.toString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
        // if people care about Apple Music, add this feature here
        else { // assume it is a file
            return "";
            // file thumbnail code, WIP but this can be pushed already since this includes soundcloud + spotify thumbnails now
        }
    }
}
