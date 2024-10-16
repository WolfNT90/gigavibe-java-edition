package Bots.commands;

import Bots.BaseCommand;
import Bots.CommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Objects;

import static Bots.Main.createQuickError;

public class CommandBug extends BaseCommand {

    @Override
    public void execute(CommandEvent event) {
        event.deferReply(true); //hacky way of making it ephemeral
        if (event.getArgs().length == 1) {
            event.replyEmbeds(createQuickError("Please provide something to report."));
            return;
        }
        String messageContentCleaned = event.getContentRaw().split(" ", 2)[1];
        Objects.requireNonNull(event.getJDA().getUserById(211789389401948160L)).openPrivateChannel().queue(a -> a.sendMessage("User: `" + event.getUser().getName() + "`\nUserID: `" + event.getUser().getId() + "`\nGuild: `" + event.getGuild().getId() + "`\n\n" + messageContentCleaned).queue());
        event.reply("Thanks for sending in a bug report!\nThe developer should be in contact with you via the bot's dms.\n**Please use the bug command to reply to messages.**");
    }

    @Override
    public String[] getNames() {
        return new String[]{"bug", "issue"};
    }

    @Override
    public Category getCategory() {
        return Category.General;
    }

    @Override
    public String getDescription() {
        return "Send a bug report to the developer.";
    }

    @Override
    public String getOptions() {
        return "<message>";
    }

    @Override
    public void ProvideOptions(SlashCommandData slashCommand) {
        slashCommand.addOption(OptionType.STRING, "message", "The bug report", true);
    }

    @Override
    public long getRatelimit() {
        return 60000;
    }
}
