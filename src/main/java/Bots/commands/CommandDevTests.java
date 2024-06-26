package Bots.commands;

import Bots.BaseCommand;
import Bots.CommandStateChecker.Check;
import Bots.MessageEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import static Bots.Main.botColour;
import static Bots.Main.registerButtonInteraction;

public class CommandDevTests extends BaseCommand {
    private void HandleButtonEvent(ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(botColour);
        eb.setDescription("*new* description text");
        event.getInteraction().editMessageEmbeds(eb.build()).queue();
    }

    @Override
    public void Init() {
        registerButtonInteraction("dev-button", this::HandleButtonEvent);
    }

    @Override
    public Check[] getChecks() {
        return new Check[]{Check.IS_DEV};
    }

    @Override
    public void execute(MessageEvent event) {
        System.out.println("Its dev time");
        String[] args = event.getArgs();
        if (args.length == 1) {
            event.reply("No dev command provided");
        } else {
            String command = args[1];
            if (command.equalsIgnoreCase("dirty-config")) { //Adds an illegal object to the json to invalidate it
                event.getConfig().put("bad-value", new Exception());
                event.reply("Added something nonsensical to the config");
            } else if (command.equalsIgnoreCase("test-buttons")) { //Testing for the button registration system
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(botColour);
                eb.setDescription("description text");
                event.getChannel().sendMessageEmbeds(eb.build()).queue(
                        message -> message.editMessageComponents().setActionRow(
                                Button.secondary("dev-button", "Me"), Button.secondary("not-dev-button", "Not Me")
                        ).queue()
                );
            } else {
                event.reply("Unrecognised dev command " + command);
            }
        }
    }

    @Override
    public String[] getNames() {
        return new String[]{"devtools", "dev"};
    }

    @Override
    public Category getCategory() {
        return Category.Dev;
    }

    @Override
    public String getDescription() {
        return "A suite of testing tools for making sure features work";
    }

    @Override
    public String getOptions() {
        return "(dirty-config | test-buttons)"; //(Command1 | Command2 | Command3) - add them here once they exist
    }

    @Override
    public void ProvideOptions(SlashCommandData slashCommand) {
        slashCommand.addOption(OptionType.STRING, "tool", "The tool to use", true);
    }
}
