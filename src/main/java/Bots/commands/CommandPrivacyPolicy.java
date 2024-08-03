package Bots.commands;

import Bots.BaseCommand;
import Bots.CommandEvent;

import static Bots.Main.createQuickEmbed;

public class CommandPrivacyPolicy extends BaseCommand {

    @Override
    public void execute(CommandEvent event) {
        event.replyEmbeds(createQuickEmbed("Privacy Policy", "https://github.com/ZeNyfh/gigavibe-java-edition/blob/main/PRIVACY_POLICY.md"));
    }

    @Override
    public String[] getNames() {
        return new String[]{"privacypolicy", "privacy policy", "pp"};
    }

    @Override
    public Category getCategory() {
        return Category.General;
    }

    @Override
    public String getDescription() {
        return "Sends a link to the privacy policy.";
    }

    @Override
    public long getRatelimit() {
        return 10000;
    }
}
