package Bots;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.interactions.InteractionHookImpl;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static Bots.Main.botPrefix;
import static Bots.Main.createQuickError;

// TODO: Purge this file and re-make the system to be better
// (Basically, give the queueing responsibility back to the code that calls this)
// (This would allow code to call setEphemeral and whatever as it likes)

/**
 * An extension of the MessageReceivedEvent that provides generally useful attributes for commands.
 * Can take either a SlashCommandInteractionEvent or MessageReceivedEvent as the input
 *
 * @author 9382
 * @version 2.1.1
 */
public class MessageEvent {
    final Object coreEvent;
    final JDA JDA;
    final Guild guild;
    final GuildMessageChannelUnion channel;
    final Member member;
    final User user;
    final String[] args;
    final OptionMapping[] options;
    final String rawContent;
    final List<Message.Attachment> attachments;
    final JSONObject config;

    public MessageEvent(MessageReceivedEvent event) {
        this.coreEvent = event;
        this.JDA = event.getJDA();
        this.guild = event.getGuild();
        this.channel = event.getGuildChannel();
        this.member = event.getMember();
        this.user = event.getAuthor();
        this.rawContent = event.getMessage().getContentRaw().replaceFirst(botPrefix, "").trim();
        this.args = this.rawContent.split(" ");
        this.options = new OptionMapping[0]; //Not a thing outside of slash commands, but we should still define it here
        this.attachments = event.getMessage().getAttachments();
        this.config = GuildDataManager.GetGuildConfig(event.getGuild().getIdLong());
    }

    public MessageEvent(SlashCommandInteractionEvent event) {
        this.coreEvent = event;
        this.JDA = event.getJDA();
        this.guild = event.getGuild();
        this.channel = event.getGuildChannel();
        this.member = event.getMember();
        this.user = event.getUser();

        List<OptionMapping> options = event.getInteraction().getOptions();
        List<String> args = new ArrayList<>();
        args.add("/" + event.getFullCommandName());
        List<Message.Attachment> Attachments = new ArrayList<>();
        for (OptionMapping option : options) {
            if (option.getType() == OptionType.ATTACHMENT) {
                Attachments.add(option.getAsAttachment());
            } else {
                args.add(option.getAsString());
            }
        }
        this.options = options.toArray(new OptionMapping[0]);
        this.attachments = Attachments;
        this.rawContent = String.join(" ", args);
        this.args = this.rawContent.split(" "); //Ensure parallel interpretation to a regular message (also just easier)

        if (event.getGuild() != null) {
            this.config = GuildDataManager.GetGuildConfig(event.getGuild().getIdLong());
        } else {
            this.config = new JSONObject();
        }
    }

    public boolean isSlash() {
        return this.coreEvent.getClass() == SlashCommandInteractionEvent.class;
    }

    public JDA getJDA() {
        return this.JDA;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public GuildMessageChannelUnion getChannel() {
        return this.channel;
    }

    public Member getMember() {
        return this.member;
    }

    public User getUser() {
        return this.user;
    }

    public String[] getArgs() {
        return this.args;
    }

    public OptionMapping[] getOptions() {
        return this.options;
    }

    public List<Message.Attachment> getAttachments() {
        return this.attachments;
    }

    public String getContentRaw() {
        return this.rawContent;
    }

    public Object getCoreEvent() { //Use in commands as little as you can, since this gets hacky fast
        return this.coreEvent;
    }

    public boolean isAcknowledged() {
        return isSlash() && ((SlashCommandInteractionEvent) this.coreEvent).isAcknowledged();
    }

    public void deferReply(boolean ephemeral) {
        if (isSlash()) {
            ((SlashCommandInteractionEvent) this.coreEvent).deferReply(ephemeral).queue();
        }
    }

    public void deferReply() {
        deferReply(false);
    }

    public void reply(Consumer<MessageEvent.Response> lambda, String s) {
        if (isSlash()) {
            SlashCommandInteractionEvent event = ((SlashCommandInteractionEvent) this.coreEvent);
            if (isAcknowledged())
                event.getHook().editOriginal(s).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            else
                event.reply(s).queue(x -> lambda.accept(new MessageEvent.Response(x)));
        } else {
            try {
                ((MessageReceivedEvent) this.coreEvent).getMessage().reply(s).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void reply(String s) {
        reply(x -> {
        }, s);
    }

    public void replyEmbeds(Consumer<MessageEvent.Response> lambda, MessageEmbed embed, MessageEmbed... embeds) {
        if (isSlash()) {
            SlashCommandInteractionEvent event = ((SlashCommandInteractionEvent) this.coreEvent);
            if (isAcknowledged()) {
                List<MessageEmbed> allembeds = new ArrayList<>();
                allembeds.add(embed);
                allembeds.addAll(List.of(embeds));
                event.getHook().editOriginalEmbeds(allembeds).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            } else
                event.replyEmbeds(embed, embeds).queue(x -> lambda.accept(new MessageEvent.Response(x)));
        } else {
            MessageReceivedEvent event = ((MessageReceivedEvent) this.coreEvent);
            if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_SEND)) {
                try {
                    Objects.requireNonNull(event.getMember()).getUser().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(createQuickError(String.format("The bot does not have the permission to message in `%s`.", event.getChannel().getName()))).queue());
                } catch (Exception ignored) {
                } // this can safely be ignored as I expect this to throw an exception.
            }
            try {
                event.getMessage().replyEmbeds(embed, embeds).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void replyEmbeds(MessageEmbed embed, MessageEmbed... embeds) {
        replyEmbeds(x -> {
        }, embed, embeds);
    }

    public void replyFiles(Consumer<MessageEvent.Response> lambda, FileUpload... files) {
        if (isSlash()) {
            SlashCommandInteractionEvent event = ((SlashCommandInteractionEvent) this.coreEvent);
            if (isAcknowledged())
                event.getHook().editOriginalAttachments(files).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            else
                event.replyFiles(files).queue(x -> lambda.accept(new MessageEvent.Response(x)));
        } else {
            try {
                ((MessageReceivedEvent) this.coreEvent).getMessage().replyFiles(files).queue(x -> lambda.accept(new MessageEvent.Response(x)));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void replyFiles(FileUpload... files) {
        replyFiles(x -> {
        }, files);
    }

    public JSONObject getConfig() {
        return this.config;
    }

    public static class Response {
        //Bad type conversion practices, the sequel
        final Object coreObject;

        public Response(InteractionHook interaction) {
            this.coreObject = interaction;
        }

        public Response(Message message) {
            this.coreObject = message;
        }

        public boolean isSlash() {
            return this.coreObject.getClass() == InteractionHookImpl.class;
        }

        public void delete() {
            if (isSlash()) {
                ((InteractionHookImpl) this.coreObject).deleteOriginal().queue();
            } else {
                ((Message) this.coreObject).delete().queue();
            }
        }

        public void editMessage(String s) {
            if (isSlash()) {
                ((InteractionHookImpl) this.coreObject).editOriginal(s).queue();
            } else {
                ((Message) this.coreObject).editMessage(s).queue();
            }
        }

        public void editMessageFormat(String s, Object... objects) {
            if (isSlash()) {
                ((InteractionHookImpl) this.coreObject).editOriginalFormat(s, objects).queue();
            } else {
                ((Message) this.coreObject).editMessageFormat(s, objects).queue();
            }
        }

        public void editMessageEmbeds(MessageEmbed... embeds) {
            if (isSlash()) {
                ((InteractionHookImpl) this.coreObject).editOriginalEmbeds(embeds).queue();
            } else {
                ((Message) this.coreObject).editMessageEmbeds(embeds).queue();
            }
        }

        public void editMessageFiles(FileUpload... files) {
            if (isSlash()) {
                ((InteractionHookImpl) this.coreObject).editOriginalAttachments(files).queue();
            } else {
                ((Message) this.coreObject).editMessageAttachments(files).queue();
            }
        }
    }
}
