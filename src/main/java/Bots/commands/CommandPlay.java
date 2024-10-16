package Bots.commands;

import Bots.BaseCommand;
import Bots.CommandStateChecker.Check;
import Bots.CommandEvent;
import Bots.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static Bots.Main.createQuickEmbed;
import static Bots.Main.createQuickError;

public class CommandPlay extends BaseCommand {
    final public Set<String> audioFiles = Set.of(
            "mp3", "mp4", "wav", "ogg", "flac", "mov", "wmv", "m4a", "aac", "webm", "opus", "m3u", "txt"
    );

    @Override
    public Check[] getChecks() {
        return new Check[]{Check.IS_CHANNEL_BLOCKED, Check.TRY_JOIN_VC};
    }

    @Override
    public void execute(CommandEvent event) throws IOException {
        event.deferReply(); //expect to take a while
        String string = event.getContentRaw();
        String[] args = string.split(" ", 2);

        // figure out if there are actually attachments that can be played
        List<Message.Attachment> attachments = event.getAttachments();
        List<Message.Attachment> playableAttachments = new ArrayList<>();

        for (int i = 0; i < attachments.size(); i++) {
            Message.Attachment attachment = attachments.get(i);
            if (attachment.getFileExtension() != null && audioFiles.contains(attachment.getFileExtension().toLowerCase())) {
                playableAttachments.add(attachment);
            }
        }

        // play attachments
        if (!playableAttachments.isEmpty()) {
            Message.Attachment att = playableAttachments.get(0);
            String fileExtension = att.getFileExtension() != null ? att.getFileExtension().toLowerCase() : "";

            if (fileExtension.equals("txt")) {
                playFromTXT(event, true);
            } else {
                try {
                    if (playableAttachments.size() == 1) {
                        PlayerManager.getInstance().loadAndPlay(event, playableAttachments.get(0).getUrl(), true);
                    } else {
                        for (Message.Attachment attachment : playableAttachments) {
                            PlayerManager.getInstance().loadAndPlay(event, attachment.getUrl(), false);
                        }
                        event.replyEmbeds(createQuickEmbed("✅ **Success**", "Queued " + playableAttachments.size() + " tracks from attachments."));
                    }
                } catch (Exception e) {
                    event.replyEmbeds(createQuickError("Something went wrong when loading the tracks from attachments.\n```\n" + e.getMessage() + "\n```"));
                }
            }
        } else { // no valid attachments to play, check for url/s in message content.
            if (args.length < 2) {
                // error for no valid attachments found
                if (!attachments.isEmpty()){
                    event.replyEmbeds(createQuickError("The track failed to load: Unknown file format."));
                    return;
                }
                event.replyEmbeds(createQuickError("No arguments given."));
                return;
            }
            if (playFromTXT(event, false)) return;
            // if playFromTXT fails, it tries to play the singular link.

            String link = String.valueOf(args[1]);
            if (link.contains("https://") || link.contains("http://")) {
                link = link.replace("youtube.com/shorts/", "youtube.com/watch?v=");
                link = link.replace("youtu.be/", "www.youtube.com/watch?v=");
            } else {
                link = "ytsearch: " + link;
            }
            try {
                PlayerManager.getInstance().loadAndPlay(event, link, true);
            } catch (FriendlyException e) {
                event.replyEmbeds(createQuickError("Something went wrong when decoding the track.\n```\n" + e.getMessage() + "\n```"));
            }
        }
    }

    private static boolean playFromTXT(CommandEvent event, boolean isAttachment) throws IOException {
        URL url;
        if (isAttachment) {
            Message.Attachment att = event.getAttachments().get(0);
            String fileExtension = att.getFileExtension() != null ? att.getFileExtension().toLowerCase() : "";
            if (!fileExtension.equals("txt")) {
                return false; // could be an actual audio file
            }
            url = new URL(att.getUrl());
        } else {
            String link = String.valueOf(event.getArgs()[1]);
            if (!link.split("&")[0].split("\\?")[0].toLowerCase().endsWith(".txt")) { // could be an actual audio file
                return false;
            }
            url = new URL(link);
        }

        URLConnection connection = url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        int actualListSize = 0;
        ArrayList<String> finalURLs = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                actualListSize++;
                line = line.trim();
                if (!line.startsWith("http")) {
                    line = "ytsearch: " + line;
                }
                finalURLs.add(line);
            }
            if (!isAttachment && finalURLs.size() <= 2) return false;
            for (String finalLine : finalURLs) {
                PlayerManager.getInstance().loadAndPlay(event, finalLine.split("\\|", 2)[0].trim(), false);
            }
            event.replyEmbeds(createQuickEmbed("✅ **Success**", "Queued " + actualListSize + " songs!"));
        } catch (Exception e) {
            event.replyEmbeds(createQuickError("Something went wrong when loading the tracks from the file.\n```\n" + e.getMessage() + "\n```")); // tell the user what happened.
        }
        return true;
    }

    @Override
    public Category getCategory() {
        return Category.Music;
    }

    @Override
    public String[] getNames() {
        return new String[]{"play", "p"};
    }

    @Override
    public String getDescription() {
        return "Plays songs and playlists from sources such as yt, soundcloud, spotify, and discord/http urls.";
    }

    @Override
    public String getOptions() {
        return "<file OR track>";
    }

    @Override
    public void ProvideOptions(SlashCommandData slashCommand) {
        slashCommand.addOptions(
                new OptionData(OptionType.STRING, "track", "The track to play", false),
                new OptionData(OptionType.ATTACHMENT, "file", "The file to play", false)
        );
    }

    @Override
    public long getRatelimit() {
        return 2500;
    }
}