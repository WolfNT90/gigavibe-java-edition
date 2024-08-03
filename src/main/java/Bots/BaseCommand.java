package Bots;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import static Bots.CommandStateChecker.*;

// Custom base command class used by all commands
// Structured in the recommended order of a command, ignoring the var and enum at the top
public abstract class BaseCommand {
    public SlashCommandData slashCommand;

    public void Init() { //Optional initialisation stuff if something is required on start for a command
    }

    public Check[] getChecks() { //Common checks like IsDJ to make sure everything is fine
        return new Check[0];
    }

    public abstract void execute(CommandEvent event) throws Exception; //The main event loop

    public final void executeWithChecks(CommandEvent event) throws Exception { //For main - do not override
        CheckResult checkResult = PerformChecks(event, this.getChecks());
        if (!checkResult.Succeeded()) {
            event.replyEmbeds(Main.createQuickEmbed("❌ **Not Allowed**", checkResult.GetMessage()));
        } else {
            this.execute(event);
        }
    }

    public abstract String[] getNames(); //The first name in the list is treated as the primary name by cmds

    public abstract Category getCategory(); //The category, used by cmds

    public abstract String getDescription(); //The description, used by cmds

    public String getOptions() { //The options text used by help / cmds. Purely decorative
        return "";
    }

    public void ProvideOptions(SlashCommandData slashCommand) { //Provides options for a slash command
    }

    public long getRatelimit() { //Ratelimit in milliseconds
        return 0;
    }

    public enum Category {
        General, Music, DJ, Admin, Dev
    }
}
